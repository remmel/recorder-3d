package com.huawei.arengine.demos.java.recorder.preferences;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huawei.arengine.demos.R;

import java.util.ArrayList;
import java.util.List;

public class RecorderPreferenceActivity extends AppCompatActivity {
    private static final String TAG = RecorderPreferenceActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.recorder_activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new RecorderPreferenceFragment(getSupportedResolution()))
                .commit();
    }

    /**
     * https://developer.huawei.com/consumer/en/doc/HMSCore-References/config_base-0000001050119488-V5#EN-US_TOPIC_0000001050128723__section1231643615527
     * https://github.com/HMS-Core/hms-AREngine-demo/issues/7
     */
    private List<Size> getSupportedResolution() {
        List<Size> sizes = new ArrayList<>();
        try{
            CameraManager cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics("0"); //cameraManager.getCameraIdList()
            Size[] supportedPreviewSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(SurfaceTexture.class);

            for (Size s: supportedPreviewSizes) { //nice arrayfilter could be used
                if((float)s.getHeight() / s.getWidth() == 3f/4)
                    sizes.add(s);
            }
        }catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return sizes;
    }
}
