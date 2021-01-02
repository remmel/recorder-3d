package com.huawei.arengine.demos.java.recorder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.test.InstrumentationRegistry;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ImageUtilsTest {

    private static final String TAG = ImageUtilsTest.class.getSimpleName();

    private static final File DIR = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("test");
//    private static final String DIR = "/storage/emulated/0/Android/data/com.huawei.arenginesdk.demo/files/test";
    private static final String IMAGE_YUVNV21 =  DIR + "/00000012_image.bin";
    private static final String TUMDEPTH = DIR + "/tum/1305031102.160407.png";
    private static final int IMG_W = 1440;
    private static final int IMG_H = 1080;


    // InputStream is = getClass().getResourceAsStream("toto.txt"); to avoid copying the image in Android phone folder
    @Test
    public void writeYuvToJpeg() throws IOException {
        byte[] bytesYuvNv21 = Files.readAllBytes(Paths.get(IMAGE_YUVNV21));
        IoUtils.writeYUV(new File(DIR + "/tmp_image_writeYuvToJpeg.jpg"), bytesYuvNv21, IMG_W, IMG_H);
    }

    @Test
    //Cannot save bin file on memory as too big (3x)
    public void binToBitmap() throws IOException {
        byte[] bytesYuvNv21 = Files.readAllBytes(Paths.get(IMAGE_YUVNV21));
        Bitmap bitmap = ImageUtils.n21ToBitmapViaDecode(bytesYuvNv21, IMG_W, IMG_H);
        IoUtils.writeBitmapAsPng(new File(DIR + "/tmp_image_writeYuvManualDecode.png"), bitmap); //just to check, we don't care it's PNG

        Bitmap bitmap2 = ImageUtils.n21ToBitmapViaJpg(bytesYuvNv21, IMG_W, IMG_H);
        IoUtils.writeBitmapAsPng(new File(DIR + "/tmp_image_writeYuvViaJpg.png"), bitmap2); //just to check, we don't care it's PNG
    }

    @Test //TODO
    public void tumpng() throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(TUMDEPTH);
        int pixel = bitmap.getPixel(10,10);

        IoUtils.writeBitmapAsPng(new File(DIR + "/tum/remy.png"), bitmap);
    }
}