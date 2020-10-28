package com.huawei.arengine.demos.java.recorder;

import android.graphics.Bitmap;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ImageUtilsTest {
    //        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("test");
    private static final String DIR = "/storage/emulated/0/Android/data/com.huawei.arenginesdk.demo/files/test";
    private static final String DEPTH16 = DIR + "/00000012_depth16.bin";
    private static final String PLY = DIR + "/00000012_b.ply";
    private static final String JPEG = DIR + "/00000012_image.jpg";
    private static final String IMAGE_YUVNV21 =  DIR + "/00000012_image.bin";
    private static final int IMG_W = 1440;
    private static final int IMG_H = 1080;

    @Test
    public void writePlyFromYuvnv21() throws IOException {
        byte[] bytesDepth = Files.readAllBytes(Paths.get(DEPTH16));
        ByteBuffer depthBuffer = ByteBuffer.wrap(bytesDepth);

        byte[] bytesYuvNv21 = Files.readAllBytes(Paths.get(IMAGE_YUVNV21));
        Bitmap bitmap = ImageUtils.n21ToBitmap(bytesYuvNv21, IMG_W, IMG_H);
        ImageUtils.writePly(depthBuffer, bitmap, new File(PLY));
    }

    @Test
    public void writePlyFromJpg() throws IOException {
        byte[] bytesDepth = Files.readAllBytes(Paths.get(DEPTH16));
        ByteBuffer depthBuffer = ByteBuffer.wrap(bytesDepth);

        Bitmap bitmap = ImageUtils.jpgToBitmap(JPEG);
        ImageUtils.writePly(depthBuffer, bitmap, new File(PLY));
    }


    @Test
    public void writeYuvToJpeg() throws IOException {
        byte[] bytesYuvNv21 = Files.readAllBytes(Paths.get(IMAGE_YUVNV21));
        IoUtils.writeYUV(new File(DIR + "/00000012_image2.jpg"), bytesYuvNv21, IMG_W, IMG_H);
    }
}