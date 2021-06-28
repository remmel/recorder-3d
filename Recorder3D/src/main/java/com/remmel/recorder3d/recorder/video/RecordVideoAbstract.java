package com.remmel.recorder3d.recorder.video;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.Image;
import android.util.Size;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.huawei.hiar.ARFrame;

import java.io.File;

/***
 * As copying on memory is quicker (1.74 GB : 15s ~  115MB/s ~ 4MB/frame at 30fps) than encoding with the lib I found, I'll create the video later)
 */
abstract public class RecordVideoAbstract {
    private static final String TAG = RecordVideoAbstract.class.getSimpleName();
    protected Activity activity;
    protected Size size;
    static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    //https://github.com/google-ar/sceneform-android-sdk/blob/v1.15.0/samples/videorecording/app/src/main/java/com/google/ar/sceneform/samples/videorecording/VideoRecorder.java

    RecordVideoAbstract(Activity activity, Size size) {
        this.activity = activity;
        this.size = size;
    }

    public boolean requestPermission() {
        if (ActivityCompat.checkSelfPermission(this.activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.activity, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        } else {
            return true;
        }
    }

    public void start(File dir) { //or should return a boolean sucess?
        _start(dir);
        Toast.makeText(this.activity, "Start", Toast.LENGTH_SHORT).show();
    }

    protected abstract void _start(File dir);

    public void stop() {
        _stop();
        Toast.makeText(this.activity, "Stopped", Toast.LENGTH_SHORT).show();
    }

    protected abstract void _stop();

    public void update(ARFrame image, long currentSessionMillis) {
        _update(image.acquirePreviewImage(), currentSessionMillis);
    }

    protected abstract void _update(Image image, long currentSessionMillis);
}
