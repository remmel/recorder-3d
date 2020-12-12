package com.huawei.arengine.demos.java.recorder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.Matrix;
import android.renderscript.Matrix2f;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

public class ImageUtilsTest {
    //        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("test");
    private static final String DIR = "/storage/emulated/0/Android/data/com.huawei.arenginesdk.demo/files/2020-11-26_121940";
    private static final String DEPTH16 = DIR + "/00000012_depth16.bin";
    private static final String PLY = DIR + "/00000012_test.ply";
    private static final String JPEG = DIR + "/00000012_image.jpg";
    private static final String IMAGE_YUVNV21 =  DIR + "/00000012_image.bin";
    private static final String TUMDEPTH = DIR + "/tum/1305031102.160407.png";
    private static final int IMG_W = 1440;
    private static final int IMG_H = 1080;

    @Test
    public void writePlyFromYuvnv21ViaJpg() throws IOException {
        byte[] bytesDepth = Files.readAllBytes(Paths.get(DEPTH16));
        ByteBuffer depthBuffer = ByteBuffer.wrap(bytesDepth);

        byte[] bytesYuvNv21 = Files.readAllBytes(Paths.get(IMAGE_YUVNV21));
        Bitmap bitmap = ImageUtils.n21ToBitmapViaJpg(bytesYuvNv21, IMG_W, IMG_H);
        ImageUtils.writePly(depthBuffer, bitmap, new File(PLY));
    }

    @Test
    public void writePlyFromYuvnv21ViaDecode() throws IOException {
        byte[] bytesDepth = Files.readAllBytes(Paths.get(DEPTH16));
        ByteBuffer depthBuffer = ByteBuffer.wrap(bytesDepth);

        byte[] bytesYuvNv21 = Files.readAllBytes(Paths.get(IMAGE_YUVNV21));
        Bitmap bitmap = ImageUtils.n21ToBitmapViaDecode(bytesYuvNv21, IMG_W, IMG_H);
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
        IoUtils.writeYUV(new File(DIR + "/00000012_image_writeYuvToJpeg.jpg"), bytesYuvNv21, IMG_W, IMG_H);
    }

    @Test
    public void writeYuvToJpegManualDecode() throws IOException {
        byte[] bytesYuvNv21 = Files.readAllBytes(Paths.get(IMAGE_YUVNV21));
        Bitmap bitmap = ImageUtils.n21ToBitmapViaDecode(bytesYuvNv21, IMG_W, IMG_H);
        IoUtils.writePNG(new File(DIR + "/00000012_image_writeYuvToJpegManualDecode.png"), bitmap);
    }

    @Test //TODO
    public void tumpng() throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(TUMDEPTH);
        int pixel = bitmap.getPixel(10,10);

        IoUtils.writePNG(new File(DIR + "/tum/remy.png"), bitmap);
    }

    @Test
    public void mergePly() throws IOException {
        List<String> rows = Files.readAllLines(Paths.get(DIR + "/poses.csv"));

        List<CsvPose> poses = CsvPose.fromCsvRows(rows);

        List<PlyProp> mergePlyProps = new ArrayList<>();

        for (CsvPose pose : poses) {
            if(pose.p.tx() == 0.0f) continue; //pose not set yet
            byte[] bytesDepth = Files.readAllBytes(Paths.get(DIR + "/" + pose.frameId+"_depth16.bin"));
            ByteBuffer depthBuffer = ByteBuffer.wrap(bytesDepth);

            byte[] bytesYuvNv21 = Files.readAllBytes(Paths.get(DIR + "/" + pose.frameId+"_image.bin"));
            Bitmap bitmap = ImageUtils.n21ToBitmapViaDecode(bytesYuvNv21, IMG_W, IMG_H);
            List<PlyProp> props = ImageUtils.getPly(depthBuffer, bitmap);

            PlyProp.addTetrahedron(props);

            for(PlyProp prop : props) {
                prop.applyPose(pose.getQuat(), pose.getPosition(), 1);
                mergePlyProps.add(prop);
            }
        }

        IoUtils.writeBytes(new File(DIR+"/tmp_merge.ply"), PlyProp.toString(mergePlyProps).getBytes());
    }
}