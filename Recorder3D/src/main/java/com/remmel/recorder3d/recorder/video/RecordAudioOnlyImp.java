package com.remmel.recorder3d.recorder.video;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.Image;
import android.media.MediaRecorder;
import android.util.Log;
import android.util.Size;

import com.remmel.recorder3d.recorder.RecorderRenderManager;

import java.io.File;
import java.io.IOException;


/**
 * If I only want to save audio (no video, rgb+d jpg/png)
 */
public class RecordAudioOnlyImp extends RecordVideoAbstract {

    static final String AUDIO_FN = "audio.3gp";
    MediaRecorder audioRecorder;

    public RecordAudioOnlyImp(Activity activity, Size size) {
        super(activity, size);
    }

    @Override
    protected void _start(File dir) {
        File faudio = new File(dir, AUDIO_FN);

        audioRecorder = new MediaRecorder();
        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        audioRecorder.setOutputFile(faudio);
        audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            audioRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        audioRecorder.start();
    }

    @Override
    protected void _update(Image image, long currentSessionMillis) {}

    @Override
    protected void _stop() {
        audioRecorder.release();
    }
}
