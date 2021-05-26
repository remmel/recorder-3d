package com.remmel.recorder3d.recorder.video;

import android.app.Activity;
import android.media.Image;
import android.util.Size;

import com.remmel.recorder3d.recorder.ImageUtils;

import java.io.File;

//1440x1080 is 16fps
//1280x960 is 18fps - keeping only ImageUtils.toBitmap drop to 20fps
//640x480 is 30fps
public class RecordVideo2 extends RecordVideoAbstract {

    BitmapToVideoEncoder bitmapToVideoEncoder;
//    BytesToVideoEncoder bitmapToVideoEncoder;

    public RecordVideo2(Activity activity, Size size) {
        super(activity, size);
        bitmapToVideoEncoder = new BitmapToVideoEncoder();
    }

    @Override
    protected void _start() {
        bitmapToVideoEncoder.startEncoding(size.getWidth(), size.getHeight(), new File(activity.getExternalFilesDir(""), "recordvideo2.mp4"));
    }

    @Override

    //queue of Image : Caused by: com.huawei.hiar.exceptions.ARFatalException: Unknown error in ArImage.getPlanes().
    protected void _update(Image image) {
        bitmapToVideoEncoder.queueFrame(ImageUtils.toBitmap(image));
//        bitmapToVideoEncoder.queueFrame(new BytesImage(image));
//        image.close();
    }



    @Override
    protected void _stop() {
        bitmapToVideoEncoder.stopEncoding();
    }
}
