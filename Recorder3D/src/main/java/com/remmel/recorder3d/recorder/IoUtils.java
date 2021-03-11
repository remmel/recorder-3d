package com.remmel.recorder3d.recorder;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

//Multiple paths
//mContext.getFilesDir() /data/user/0/com.remmel.recorder3d/files/remy/myimage302.jpg
//Environment.getExternalStorageDirectory() /storage/emulated/0/remy/myimage460.jpg
//Environment.getDataDirectory() /data/remy/myimage262.jpg
//Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);  /storage/emulated/0/Download/remy/myimage128.jpg
/// this.mContext.getExternalFilesDir("remy") /storage/emulated/0/Android/data/com.remmel.recorder3d/files/remy/myimage264.jpg
public class IoUtils {
    private static final String TAG = IoUtils.class.getSimpleName();

    public static void writeBytes(File f, byte[] data) {
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
            bos.write(data);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Fail saving file:" + f.toString());
        }
    }

    public static void writeBitmapAsPng(File f, Bitmap bitmap) {
        try {
            OutputStream os = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Fail saving file:" + f.toString());
        }
    }

    public static void writeYUV(File f, byte[] nv21, int width, int height) {
        try {
            OutputStream os = new FileOutputStream(f);
            YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            yuv.compressToJpeg(new Rect(0, 0, width, height), 100, os);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "Fail saving file:" + f.toString());
        }
    }
}
