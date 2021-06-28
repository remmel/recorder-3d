package com.remmel.recorder3d.recorder.video;

import android.graphics.Bitmap;
import android.media.Image;

import com.remmel.recorder3d.recorder.ImageUtils;

public class QueueFrame {

    public Bitmap bitmap;
    //    public byte[] nv21;
//    public int w;
//    public int h

    /** ms since the beginning of the recording sessiong */
    public long time_ms;

    public QueueFrame(Image image, long time_ms) {
        assert(time_ms < 3600*1000); //check if we use right unit. Possible as it cannot last more that 1hr=3600s
        this.time_ms = time_ms;

        this.bitmap = ImageUtils.toBitmap(image);;

//        this.nv21 = ImageUtils.YUV_420_888toNV21(image);
    }
}
