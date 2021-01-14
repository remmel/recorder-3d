package com.huawei.arengine.demos.java.recorder;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PlyUtils {

    private static final String TAG = PlyUtils.class.getSimpleName();

    public static List<PlyProp> getPly(ByteBuffer depthBuffer, Bitmap rgb){
        int w = 240; //TODO that data must be params
        int h = 180;
        //depth
        short[][] depth = ImageUtils.depth16ToDepthRangeArray(depthBuffer, w, h);
        return getPly(depth, w, h, rgb);
    }

    // TODO get intrinsics rgb & d and extrinsics between them
    //TODO create Bitmap interface

    protected static List<PlyProp> getPly(short[][] depthInMm, int w, int h, Bitmap rgb){
//        float fx = 170; //mean 1m depth min/max world x [-0.7m,0.7m] (240/2/170=0.7) right? means Math.atan(0.7,1)*180/3.14*2=70° horizontal fov? depthfx = rgbfx / 6 as it should be similar
//        float fy = 170;
        float fx = 178.8f;
        float fy = 178.8f;

        //if depth=240x180 and rgb=1440x1080 then ratio=6 // rgb is 6x bigger than depth
        float ratioRgbdW = rgb != null ? (rgb.getWidth() / (float)w) : 0;
        float ratioRgbdH = rgb != null ? (rgb.getHeight() / (float)h) : 0;

        List<PlyProp> plyProps = new ArrayList<>();

        for (int x = 0; x<w ; x++) {
            for(int y = 0; y<h; y++) {
                float z = depthInMm[x][y] / 1000f; //base unit is meter, not millimeter
                if(z == 0f) continue; //no depth value
                if(y == 0) continue; //first row contains incorrect values
                int cx = x - w/2;
                int cy = y - h/2;
                float xw = (float)cx * z / fx;
                float yw = (float)cy * z / fy;

                int pixel = rgb != null ? rgb.getPixel((int)(x*ratioRgbdW), (int)(y*ratioRgbdH)) : 0;
                plyProps.add(new PlyProp(xw, yw, z,pixel));
            }
        }
        return plyProps;
    }

    public static List<PlyProp> getPlyFromPng16(String depthPngPath) {
        org.bytedeco.javacpp.Loader.load(org.bytedeco.javacpp.opencv_java.class);
        Mat png = Imgcodecs.imread(depthPngPath, CvType.CV_16UC1);

        int w = png.cols();
        int h = png.rows();

        short[] depth16 = new short[w * h];
        png.get(0, 0, depth16);

        // short[w*h] => short[w][h]
        // TODO avoid that studpid conv
        short[][] depthInMm = new short[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                depthInMm[x][y] = depth16[x+y*w];  //divided by 5?
            }
        }
        return getPly(depthInMm, w, h, null);
    }

    public static List<PlyProp> getPlyFromPcl(String pclPath) throws IOException { //, String rgbPath
        ByteBuffer byteBuffer = ByteBuffer.wrap(Files.readAllBytes(Paths.get(pclPath)));
        byteBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int nbPoints = byteBuffer.asIntBuffer().get();
        FloatBuffer fb = byteBuffer.asFloatBuffer();
        fb.get(); //sizeof(int) == sizeof(float) == 4
        int remaining = fb.remaining();

        if(remaining != nbPoints * 4) throw new RuntimeException("Number of points ("+nbPoints+") written in file do not match remaining size of file ("+remaining+" ints)");
        List<PlyProp> plyProps = new ArrayList<>(nbPoints);
        float[] xyzw = new float[4];
        for(int i = 0; i<nbPoints;i++) {
            fb.get(xyzw);
            plyProps.add(new PlyProp(xyzw[0], xyzw[1], xyzw[2]));
        }
        return plyProps;
    }

    public static List<PlyProp> getPly(String depthPath, String rgbPath) throws IOException {
        ByteBuffer depthBuffer = ByteBuffer.wrap(Files.readAllBytes(Paths.get(depthPath)));
        Bitmap bitmap = ImageUtils.jpgToBitmap(rgbPath);
        return getPly(depthBuffer, bitmap);
    }

    public static void writePly(ByteBuffer depthBuffer, Bitmap rgb, File plyOutput) {
        List<PlyProp> plyProps = getPly(depthBuffer, rgb);
        PlyUtils.writePly(plyProps, plyOutput.getPath(), true);
    }

    public static void writePly(List<PlyProp> props, String path, boolean isAscii) {
        final FileOutputStream fos;
        try {
            fos = new FileOutputStream(path);
            fos.write(PlyProp.getHeader(props.size(), isAscii).getBytes());
            for (PlyProp prop: props) {
                fos.write(prop.toBytes(isAscii));
            }
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error writePly:"+e.getMessage());
            e.printStackTrace();
        }
    }

    public static String toString(List<PlyProp> props) {
        StringBuffer sb = new StringBuffer();
        sb.append(PlyProp.getHeader(props.size(), true));
        for (PlyProp p: props) {
            sb.append(p.toString()).append('\n');
        }
        return sb.toString();
    }

    /**
     * Optimizations:
     * - In general avoid wrapping fos in PrintStream or DataOutputStream
     * - fos.write(prop.toString().getBytes()) 2x quicker than ps.println(prop.toString());
     * - quicker to allocate ByteBuffer for a PlyProp than 6 write (2-3x)
     * - TODO use RandomAccessFile to write whole file FileChannel rwChannel = new RandomAccessFile("textfile.txt", "rw").getChannel();
     *   ByteBuffer wrBuf = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, buffer.length * nbVertrex);or (){wrBuf.put(buffer); } rwChannel.close();
     */
    public static void merge(String posesPath, String plyPath, boolean isAscii) throws IOException {
        String dir =  Paths.get(posesPath).getParent().toString();
        List<CsvPose> poses = CsvPose.fromCsvRows(Files.readAllLines(Paths.get(posesPath)));
        final FileOutputStream fos = new FileOutputStream(plyPath);
        fos.write(PlyProp.getHeader(0, isAscii).getBytes());  //dumb header, will be overwrite after //1,040,536

        int nbVertrex = 0;
        for (CsvPose pose : poses) {
            if(pose.p.tx() == 0.0f) continue; //pose not set yet //TO be removed should not save unset pose

            Log.v(TAG, "write frame: "+pose.frameId);

//            if(Integer.parseInt(pose.frameId) > 81) continue;

            List<PlyProp> propsPose = getPly(dir + "/" + pose.frameId+"_depth16.bin", dir + "/" + pose.frameId+"_image.jpg");
//            propsPose.addAll(PlyProp.getTetrahedron(0.005f, Color.RED));
            nbVertrex += propsPose.size();

            //local to world + merge
            for(PlyProp prop : propsPose) {
                prop.applyPose(pose.getQuat(), pose.getPosition(), 1);
                fos.write(prop.toBytes(isAscii));
            }
        }
        fos.close();

        //write over, the right number of vertrex. The size of the header is the same than before
        //alternative could be to create a new file, but it multiplies by 2 the process duration
        RandomAccessFile raf = new RandomAccessFile(plyPath, "rw");
        raf.writeBytes(PlyProp.getHeader(nbVertrex, isAscii));
        raf.close();
    }
}
