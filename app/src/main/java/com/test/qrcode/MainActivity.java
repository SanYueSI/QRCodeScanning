package com.test.qrcode;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.test.qrcode.camera.utils.FileUtil;
import com.test.qrcode.camera.utils.ZXingUtilsTest;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_CODE_ADDRESS = 100;
    private final String[] permissions = new String[]{Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //创建两个Bitmap,一个放二维码，一个放logo
    private Bitmap codeBmp, logoBmp;
    private String url = "hello world!";
    private TextView tvSaveCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvSaveCode = findViewById(R.id.tv_save_code);
        findViewById(R.id.tv_create_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //如果需要logo圆角的话可以对bitmap进行圆角处理或者图片用圆角图片
                logoBmp = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
                codeBmp = ZXingUtilsTest.createQRImage(url, logoBmp);
                ((ImageView) findViewById(R.id.image)).setImageBitmap(codeBmp);
                tvSaveCode.setVisibility(codeBmp != null ? View.VISIBLE : View.GONE);
            }
        });
        findViewById(R.id.tv_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; i < permissions.length; i++) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, permissions[0]) != PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission(MainActivity.this, permissions[1]) != PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission(MainActivity.this, permissions[2]) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE_ADDRESS);
                    } else {
                        Intent intent = new Intent(MainActivity.this, ScanningQRCodeActivity.class);
                        startActivityForResult(intent, 1000);
                        break;
                    }
                }
            }
        });
        tvSaveCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileUtil.saveImageToGallery(MainActivity.this,codeBmp);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String resultText = data.getStringExtra("resultText");
                Toast.makeText(this, resultText, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_ADDRESS) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(this.permissions)) {
                    Intent intent = new Intent(MainActivity.this, ScanningQRCodeActivity.class);
                    startActivity(intent);
                    break;
                }
            }
        }
    }
}