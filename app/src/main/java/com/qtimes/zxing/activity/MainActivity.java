package com.qtimes.zxing.activity;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.qtimes.zxing.R;
import com.qtimes.zxing.camera.CreateQRBitmp;
import com.qtimes.zxing.dialog.ImageOptDialog;
import com.qtimes.zxing.utils.BitmapUtil;
import com.qtimes.zxing.utils.ImageUtil;
import com.qtimes.libzxing.zxing.activity.CaptureActivity;

public class MainActivity extends AppCompatActivity {
    private final int SCAN_REQUEST_CODE_QR = 199;//扫描物理位置二维码
    private final int SCAN_REQUEST_CODE_BAR = 200;//扫描短地址条形码
    private final int SELECT_IMAGE_REQUEST_CODE = 201;
    protected final int PERMS_REQUEST_CODE = 202;

    private EditText etInput;
    private Bitmap qrCodeBitmap;
    private ImageView ivQrImage;
    private TextView tvBarCode, tvQrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //6.0版本或以上需请求权限
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            requestPermissions(permissions, PERMS_REQUEST_CODE);
        }

        etInput = findViewById(R.id.et_input);
        ivQrImage = findViewById(R.id.iv_qr_image);

        findViewById(R.id.btn_scanning).setOnClickListener(onClickListener);
        findViewById(R.id.btn_select).setOnClickListener(onClickListener);
        findViewById(R.id.generate_qr_code).setOnClickListener(onClickListener);
        findViewById(R.id.btn_long_press).setOnClickListener(onClickListener);
        findViewById(R.id.btn_scanning_qr).setOnClickListener(onClickListener);
        tvBarCode = findViewById(R.id.tv_bar_code);
        tvQrCode = findViewById(R.id.tv_qr_code);

        ivQrImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                longPress();
                return false;
            }
        });
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_scanning://扫描
                    Intent intentBar = new Intent(MainActivity.this, CaptureActivity.class);
                    startActivityForResult(intentBar, SCAN_REQUEST_CODE_BAR);
                    break;
                case R.id.btn_scanning_qr:
                    Intent intentQr = new Intent(MainActivity.this, ScanActivity.class);
                    startActivityForResult(intentQr, SCAN_REQUEST_CODE_QR);
                    break;
                case R.id.btn_select:
                    //激活系统图库，选择一张图片
                    Intent innerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    Intent wrapperIntent = Intent.createChooser(innerIntent, "选择二维码图片");
                    startActivityForResult(wrapperIntent, SELECT_IMAGE_REQUEST_CODE);
                    break;
                case R.id.generate_qr_code://生成二维码
                    String contentString = etInput.getText().toString().trim();
                    if (TextUtils.isEmpty(contentString)) {
                        showToast("请输入二维码内容");
                        return;
                    }
                    Log.i("ansen", "输入的内容:" + contentString);
                    Bitmap portrait = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                    //两个方法，一个不传大小，使用默认
                    qrCodeBitmap = CreateQRBitmp.createQRCodeBitmap(contentString, portrait);
                    ivQrImage.setImageBitmap(qrCodeBitmap);
                    break;
                case R.id.btn_long_press:
                    longPress();
                    break;

            }
        }
    };

    private void longPress() {
        if (qrCodeBitmap == null) {
            showToast("请先生成二维码图片");
            return;
        }
        ImageOptDialog imageOptDialog = new ImageOptDialog(this);
        imageOptDialog.setCallback(new ImageOptDialog.ImageOptCallback() {
            //识别二维码
            @Override
            public void onIdentifyQrClick() {
                View view = getWindow().getDecorView().getRootView();//找到当前页面的根布局
                view.setDrawingCacheEnabled(true);//禁用绘图缓存
                view.buildDrawingCache();

                Bitmap temBitmap = view.getDrawingCache();
                String result = BitmapUtil.parseQRcode(temBitmap);
                showToast("长按识别二维码结果:" + result);

                //禁用DrawingCahce否则会影响性能 ,而且不禁止会导致每次截图到保存的是缓存的位图
                view.setDrawingCacheEnabled(false);//识别完成之后开启绘图缓存
            }

            //保存图片到本地
            @Override
            public void onSaveImageClick() {
                View view = getWindow().getDecorView().getRootView();//找到当前页面的根布局
                view.setDrawingCacheEnabled(true);//禁用绘图缓存
                view.buildDrawingCache();

                Bitmap temBitmap = view.getDrawingCache();
                ImageUtil.savePicToLocal(temBitmap, MainActivity.this);

                //禁用DrawingCahce否则会影响性能 ,而且不禁止会导致每次截图到保存的是缓存的位图
                view.setDrawingCacheEnabled(false);//识别完成之后开启绘图缓存

                showToast("保存图片到本地成功");
            }
        });
        imageOptDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == SELECT_IMAGE_REQUEST_CODE) {//从图库选择图片
            String[] proj = {MediaStore.Images.Media.DATA};
            // 获取选中图片的路径
            Cursor cursor = this.getContentResolver().query(intent.getData(), proj, null, null, null);
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                String photoPath = cursor.getString(columnIndex);
                String result = BitmapUtil.parseQRcode(photoPath);
                if (!TextUtils.isEmpty(result)) {
                    showToast("从图库选择的图片识别结果:" + result);
                } else {
                    showToast("从图库选择的图片不是二维码图片");
                }
            }
            cursor.close();
        } else if (requestCode == SCAN_REQUEST_CODE_BAR && resultCode == RESULT_OK) {
            String input = intent.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            tvBarCode.setText(input);
            showToast("条形码扫描结果:" + input);
        } else if (requestCode == SCAN_REQUEST_CODE_QR && resultCode == RESULT_OK) {
            String input = intent.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            tvQrCode.setText(input);
            showToast("二维码扫描结果:" + input);
        }
    }

    private void showToast(String str) {
        Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
    }
}
