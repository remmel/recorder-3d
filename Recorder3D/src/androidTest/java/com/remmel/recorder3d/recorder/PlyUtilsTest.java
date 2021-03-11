package com.remmel.recorder3d.recorder;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.test.InstrumentationRegistry;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PlyUtilsTest {

    private static final String TAG = PlyUtilsTest.class.getSimpleName();

    private static final String DIR_DATASET = "/storage/emulated/0/Android/data/com.remmel.recorder3d/files/2020-12-17_141504";
    private static final File DIR = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("test");
    private static final String DEPTH16 = DIR + "/00000012_depth16.bin";
    private static final String IMAGE_YUVNV21 =  DIR + "/00000012_image.bin";
    private static final int IMAGE_YUVNV21_W = 1440;
    private static final int IMAGE_YUVNV21_H = 1080;
    private static final String PLY = DIR + "/tmp_00000012.ply";
    private static final String PLY_BIN = DIR + "/tmp_00000012_bin.ply";
    private static final String JPEG = DIR + "/00000012_image.jpg";

    @Test
    public void writePlyFromBinYuvnv21() throws IOException {
        ByteBuffer depthBuffer = ByteBuffer.wrap(Files.readAllBytes(Paths.get(DEPTH16)));

        byte[] bytesYuvNv21 = Files.readAllBytes(Paths.get(IMAGE_YUVNV21));
        Bitmap bitmap = ImageUtils.n21ToBitmapViaJpg(bytesYuvNv21, IMAGE_YUVNV21_W, IMAGE_YUVNV21_H);
//        Bitmap bitmap = ImageUtils.n21ToBitmapViaDecode(bytesYuvNv21, IMAGE_YUVNV21_W, IMAGE_YUVNV21_H);

        PlyUtils.writePly(depthBuffer, bitmap, new File(PLY));
    }

    @Test
    public void writePlyFromJpg() throws IOException {
        PlyUtils.writePly(DEPTH16, JPEG, PLY, true);
        PlyUtils.writePly(DEPTH16, JPEG, PLY_BIN, false);
    }

    @Test
    /**
     * Tests optimization 830 jpg 3624x2448 :
     * Ascii - 30min - 1.10GB (13min to "prepand" header in new file)
     * Ascii - no prepand, random access - 20min
     * Ascii - use FOS 12.7min
     * Bin - use FOS 9.4m - 401MB
     */
    public void mergePly() throws IOException {
        Timer t = new Timer();
        PlyUtils.merge(DIR_DATASET + "/poses_agisoft.csv", DIR_DATASET+"/ply/tmp_frames_agisoft.ply", false);
        Log.d(TAG, "bin: "+t.getElapsedSeconds());
    }

    @Test
    public void bulkWritePly() throws IOException {
        PlyUtils.bulkWritePly(new File("/storage/emulated/0/Android/data/com.remmel.recorder3d/files/2021-02-15_025939"));
    }

    @Test
    public void cropRgbdVideo(){

    }
}