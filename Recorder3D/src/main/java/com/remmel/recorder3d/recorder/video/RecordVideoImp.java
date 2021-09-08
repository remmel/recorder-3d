package com.remmel.recorder3d.recorder.video;

import android.app.Activity;
import android.media.Image;
import android.util.Size;

import com.remmel.recorder3d.recorder.RecorderRenderManager;

import java.io.File;



/**
 * Because of useless conversion done by BitmapToVideoEncoder, fps is not great:
 * 1440x1080 is 16fps
 * 1280x960 is 18fps - keeping only ImageUtils.toBitmap drop to 20fps
 * 640x480 is 30fps
 *
 * Alternative is to use https://github.com/android/camera-samples/tree/main/Camera2Video which uses MediaRecorder only, with a surface given to `device.createCaptureSession`
 * but this is using Camera2APi, not sure if I can get both RGB and Depth images at the same time:
 * - https://developer.android.com/training/camera2/multi-camera
 * - https://developer.android.com/training/camera2/multiple-camera-streams-simultaneously
 *
 * TODO try grafika alternative : https://stackoverflow.com/questions/47869061/providing-video-recording-functionality-with-arcore
 */
public class RecordVideoImp extends RecordVideoAbstract {

    BitmapToVideoEncoder bitmapToVideoEncoder;

    public RecordVideoImp(Activity activity, Size size) {
        super(activity, size);
        bitmapToVideoEncoder = new BitmapToVideoEncoder();
    }

    @Override
    protected void _start(File dir) {
        bitmapToVideoEncoder.startEncoding(size.getWidth(), size.getHeight(), new File(dir, RecorderRenderManager.FN_VIDEO));
    }

    @Override

    //queue of Image : Caused by: com.huawei.hiar.exceptions.ARFatalException: Unknown error in ArImage.getPlanes().
    protected void _update(Image image, long currentSessionMillis) {
        bitmapToVideoEncoder.queueFrame(new QueueFrame(image, currentSessionMillis));
    }



    @Override
    protected void _stop() {
        bitmapToVideoEncoder.stopEncoding();
    }
}
