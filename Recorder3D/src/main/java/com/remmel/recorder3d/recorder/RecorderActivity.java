package com.remmel.recorder3d.recorder;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.remmel.recorder3d.R;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.remmel.recorder3d.recorder.preferences.AppSharedPreference;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;

public class RecorderActivity extends Activity {
    private static final String TAG = RecorderActivity.class.getSimpleName();

    private static final int OPENGLES_VERSION = 2;

    private ARSession mArSession;

    private GLSurfaceView mSurfaceView;

    private RecorderRenderManager mRecorderRenderManager;

    private DisplayRotationManager mDisplayRotationManager;

    private String message = null;

    private boolean isRemindInstall = false;

    Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recorder_activity_main);

        mSurfaceView = findViewById(R.id.recorderSurfaceview);
        mDisplayRotationManager = new DisplayRotationManager(this);

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);

        // Set the EGL configuration chooser, including for the number of
        // bits of the color buffer and the number of depth bits.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mRecorderRenderManager = new RecorderRenderManager(this, this);
        mRecorderRenderManager.setDisplayRotationManage(mDisplayRotationManager);

        mSurfaceView.setRenderer(mRecorderRenderManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        btnBack = findViewById(R.id.btn_recorder_back);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        Exception exception = null;
        message = null;
        if (mArSession == null) {
            try {
                if (!arEngineAbilityCheck()) {
                    finish();
                    return;
                }
                AppSharedPreference pref = new AppSharedPreference(this);

                mArSession = new ARSession(this);
                ARWorldTrackingConfig config = new ARWorldTrackingConfig(mArSession);
                config.setFocusMode(pref.getFocusMode()); //Autofocus to get a way better img quality, but getFocalLength returns fixed value
//                config.setSemanticMode(ARWorldTrackingConfig.SEMANTIC_PLANE);
                // config.setPowerMode(ARConfigBase.PowerMode.PERFORMANCE_FIRST);
//                config.setEnableItem(ARConfigBase.ENABLE_DEPTH | ARConfigBase.ENABLE_MESH); //default is 1

                Size s = pref.getRgbPreviewResolution();
                config.setPreviewSize(s.getWidth(), s.getHeight()); //default is 1440,1080

                mArSession.configure(config);
                mRecorderRenderManager.setArSession(mArSession);
            } catch (Exception capturedException) {
                exception = capturedException;
                setMessageWhenError(capturedException);
            }
            if (message != null) {
                stopArSession(exception);
                return;
            }
        }
        try {
            mArSession.resume();
        } catch (ARCameraNotAvailableException e) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show();
            mArSession = null;
            return;
        }
        mDisplayRotationManager.registerDisplayListener();
        mSurfaceView.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Take a photo when pressing the volume up or headset cable button
        if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            mRecorderRenderManager.onClickTrigger();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Check whether HUAWEI AR Engine server (com.huawei.arengine.service) is installed on
     * the current device. If not, redirect the user to HUAWEI AppGallery for installation.
     */
    private boolean arEngineAbilityCheck() {
        boolean isInstallArEngineApk = AREnginesApk.isAREngineApkReady(this);
        if (!isInstallArEngineApk && isRemindInstall) {
            Toast.makeText(this, "Please agree to install.", Toast.LENGTH_LONG).show();
            finish();
        }
        Log.d(TAG, "Is Install AR Engine Apk: " + isInstallArEngineApk);
        if (!isInstallArEngineApk) {
            startActivity(new Intent(this, com.huawei.arengine.demos.common.ConnectAppMarketActivity.class));
            isRemindInstall = true;
        }
        return AREnginesApk.isAREngineApkReady(this);
    }

    private void setMessageWhenError(Exception catchException) {
        if (catchException instanceof ARUnavailableServiceNotInstalledException) {
            startActivity(new Intent(this, com.huawei.arengine.demos.common.ConnectAppMarketActivity.class));
        } else if (catchException instanceof ARUnavailableServiceApkTooOldException) {
            message = "Please update HuaweiARService.apk";
        } else if (catchException instanceof ARUnavailableClientSdkTooOldException) {
            message = "Please update this app";
        } else if (catchException instanceof ARUnSupportedConfigurationException) {
            message = "The configuration is not supported by the device!";
        } else {
            message = "exception throw";
        }
    }

    private void stopArSession(Exception exception) {
        Log.i(TAG, "stopArSession start.");
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Creating session error", exception);
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        Log.i(TAG, "stopArSession end.");
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause start.");
        super.onPause();
        if (mArSession != null) {
            mDisplayRotationManager.unregisterDisplayListener();
            mSurfaceView.onPause();
            mArSession.pause();
            mRecorderRenderManager.onPause();
        }
        Log.i(TAG, "onPause end.");
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy start.");
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        super.onDestroy();
        Log.i(TAG, "onDestroy end.");
    }

    @Override
    public void onWindowFocusChanged(boolean isHasFocus) {
        Log.d(TAG, "onWindowFocusChanged");
        super.onWindowFocusChanged(isHasFocus);
        if (isHasFocus) {
            getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}