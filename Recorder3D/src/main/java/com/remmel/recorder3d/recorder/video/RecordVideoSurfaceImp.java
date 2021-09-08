package com.remmel.recorder3d.recorder.video;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

/**
 * WIP: This is not working, try that code in normal app (not using AREngine which is also using camera).
 * Or maybe because no handler set. Try to use SharedCamera?
 * Inspired by https://github.com/android/camera-samples/tree/main/Camera2Video
 */
public class RecordVideoSurfaceImp extends RecordVideoAbstract {

    CameraManager cameraManager;
    MediaRecorder mediaRecorder;

    public RecordVideoSurfaceImp(Activity activity, Size size) {
        super(activity, size);
        cameraManager = (CameraManager) activity.getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    protected void _start(File dir) {
        Surface surface = MediaCodec.createPersistentInputSurface();

        int w = this.size.getWidth(), h = this.size.getHeight();

        try {
            cameraManager.openCamera("0", new CameraDevice.StateCallback(){

                @Override
                public void onOpened(@NonNull CameraDevice device) {
                    try {
                        device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).addTarget(surface);

                        File f = new File(dir, "video_surface.mp4");

                        mediaRecorder = new MediaRecorder();
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        mediaRecorder.setOutputFile(f);
                        mediaRecorder.setVideoEncodingBitRate(10_000_000);
                        mediaRecorder.setVideoFrameRate(30);
                        mediaRecorder.setVideoSize(w, h);
                        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        mediaRecorder.setInputSurface(surface);
                        try {
                            mediaRecorder.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                        mediaRecorder.start();


                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }

                }

                @Override
                public void onDisconnected(@NonNull CameraDevice device) {}

                @Override
                public void onError(@NonNull CameraDevice device, int error) {}
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void _stop() {
        mediaRecorder.release();
    }

    @Override
    protected void _update(Image image, long currentSessionMillis) {

    }
}
