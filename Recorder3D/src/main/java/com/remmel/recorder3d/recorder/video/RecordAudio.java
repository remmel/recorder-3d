package com.remmel.recorder3d.recorder.video;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;

public class RecordAudio {
    boolean isRecoding = false;
    private static final String TAG = RecordAudio.class.getSimpleName();
    MediaRecorder recorder;
    static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    public void startRecording(File dir) {
        recorder = new MediaRecorder();
//        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(new File(dir, "audio.3gp"));
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        recorder.setPreviewDisplay();

//        mediaRecorder.getS

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        recorder.start();
        isRecoding = true;
    }

    public static void requestAudioPermission(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    public void onStop() {
        isRecoding = false;
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }
}
