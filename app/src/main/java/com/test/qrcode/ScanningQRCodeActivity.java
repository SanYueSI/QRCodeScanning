package com.test.qrcode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.test.qrcode.camera.handler.ScanningQRCodeHandler;
import com.test.qrcode.camera.mangger.AmbientLightManager;
import com.test.qrcode.camera.mangger.BeepManager;
import com.test.qrcode.camera.mangger.CameraManager;
import com.test.qrcode.camera.widget.ViewfinderView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

/***
 * Create by Yip
 * Create Time 3/4/21
 */
public class ScanningQRCodeActivity extends Activity implements SurfaceHolder.Callback {
    private CameraManager cameraManager;
    private ScanningQRCodeHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private Collection<BarcodeFormat> decodeFormats;
    private String characterSet;
    //管理震动和声音
    private BeepManager beepManager;
    //界面亮度
    private AmbientLightManager ambientLightManager;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageView ivFlash;
    private ImageView ivImage;
    public static final int RC_CHOOSE_PHOTO = 2000;
    private byte[] datas;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        //常亮屏
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_scanning_qr_code);
        viewfinderView = findViewById(R.id.viewfinder_view);
        surfaceView = findViewById(R.id.preview_view);
        ivFlash = findViewById(R.id.iv_flash);
        ivImage = findViewById(R.id.iv_image);

        hasSurface = false;
        beepManager = new BeepManager(this,true);
        ambientLightManager = new AmbientLightManager(this);
        cameraManager = new CameraManager(getApplication());
        viewfinderView.setCameraManager(cameraManager);
        surfaceHolder = surfaceView.getHolder();

        ivFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraManager.getTorchState()) {
                    cameraManager.setTorch(false);
                } else {
                    cameraManager.setTorch(true);
                }
                ivFlash.setImageResource(cameraManager.getTorchState() ? R.drawable.ic_baseline_flash_off_24 : R.drawable.ic_baseline_flash_on_24);
            }
        });
        ivImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentToPickPic = new Intent(Intent.ACTION_PICK, null);
                intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intentToPickPic, RC_CHOOSE_PHOTO);

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        handler = null;
        beepManager.updatePrefs();
        ambientLightManager.start(cameraManager);
        decodeFormats = null;
        characterSet = null;
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }


    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        //historyManager = null; // Keep for onActivityResult
        if (!hasSurface) {
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                setResult(RESULT_CANCELED);
                finish();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // do nothing
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_CHOOSE_PHOTO && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            Bitmap bmp = null;
            try {
                bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            datas = baos.toByteArray();
            //这里没有直接去发送消息 在onPause的时候handler已经退出工作了需要在onResume重新赋值才能发送所以在initCamera中去发送了
        }
    }

    /**
     * 解码完成
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            // 声音和震动
            beepManager.playBeepSoundAndVibrate();
        }
        Intent intent = new Intent();
        intent.putExtra("resultText", rawResult.getText());
        Message message = Message.obtain(handler, R.id.return_scan_result, intent);
        handler.sendMessage(message);
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            ivFlash.setImageResource(cameraManager.getTorchState() ? R.drawable.ic_baseline_flash_off_24 : R.drawable.ic_baseline_flash_on_24);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new ScanningQRCodeHandler(Looper.myLooper(), this, decodeFormats, null, characterSet, cameraManager);
                if (datas != null) {
                    Message message = Message.obtain(handler, R.id.decode_image, datas);
                    handler.sendMessage(message);
                }
            }
        } catch (IOException ioe) {
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            displayFrameworkBugMessageAndExit();
        }
    }

    public void decodeImageFailed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage("未发现二维码");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.show();
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage("很遗憾，Android 相机出现问题。你可能需要重启设备");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {

            }
        });
        builder.show();
    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

}
