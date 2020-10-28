package com.huawei.arengine.demos.java.recorder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;
import com.huawei.hiar.ARPointCloud;
import com.huawei.hiar.ARSceneMesh;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ImageUtils {
    private static final String TAG = ImageUtils.class.getSimpleName();

    private static byte[] YUV_420_888toNV21(Image image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining(); //1555199 bytes
        int uSize = uBuffer.remaining(); //777599 bytes
        int vSize = vBuffer.remaining(); //777599 bytes

        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21; //3110398 bytes
    }

    public static void writeImageRgb(Image image, File f) {
        if(image.getFormat() != ImageFormat.JPEG)
            throw new RuntimeException("Expected image format is JPEG, but is:"+image.getFormat());

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] data = new byte[buffer.capacity()];
        buffer.get(data); //TODO avoid saving in intermediate byte[]
        IoUtils.writeBytes(f, data);
        Log.i(TAG, "Image ("+image.getWidth()+"x"+image.getHeight()+") saved in " +f.getPath());
    }

    public static void writeImageYuvJpg(Image image, File f) {
        if(image.getFormat() != ImageFormat.YUV_420_888)
            throw new RuntimeException("Expected image format is YUV_420_888, but is:"+image.getFormat());

        byte[] nv21 = YUV_420_888toNV21(image);
        IoUtils.writeYUV(f, nv21, image.getWidth(), image.getHeight());
        Log.i(TAG, "Image YUV ("+image.getWidth()+"x"+image.getHeight()+") saved in " +f.getPath());
    }

    public static void writeImageN21Bin(Image image , File f) {
        assert(image.getFormat() == ImageFormat.YUV_420_888);
        byte[] nv21 = YUV_420_888toNV21(image);
        IoUtils.writeBytes(new File(f.getPath()), nv21);
    }

    public static void writeImageDepth16(Image image, File f) { //w and h will be lost
        if(image.getFormat() != ImageFormat.DEPTH16)
            throw new RuntimeException("Expected image format is DEPTH16, but is:"+image.getFormat());

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        try {
            FileChannel fc = new FileOutputStream(f).getChannel();
            fc.write(buffer);
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "Error writing image depth16: " +f.getPath());
        }
    }

    //TODO add "tum" depth png : https://vision.in.tum.de/data/datasets/rgbd-dataset/file_formats
    // monochrome / 5000 <=> 1000mm / 16 bits
    public static void writeImageDepthNicePng(Image image, File f) {
        if(image.getFormat() != ImageFormat.DEPTH16)
            throw new RuntimeException("Expected image format is DEPTH16, but is:"+image.getFormat());

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();

        int w = image.getWidth();
        int h = image.getHeight();

        short[][] depth = depth16ToDepthRangeArray(buffer, w, h);
        Bitmap bitmap = depthArrayToFancyRgbHueBitmap(depth, w, h);

        IoUtils.writePNG(f, bitmap);
        Log.i(TAG, "Image depth ("+image.getWidth()+"x"+image.getHeight()+") saved in " +f.getPath());
    }

    protected static short[][] depth16ToDepthRangeArray(ByteBuffer byteBuffer, int w, int h) {
        byteBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sBuffer = byteBuffer.asShortBuffer();
        short[][] depth = new short[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                short depthSample = sBuffer.get(); //depth16[y*width + x];
                short depthRange = (short) (depthSample & 0x1FFF);
//                short depthConfidence = (short) ((depthSample >> 13) & 0x7);
                depth[x][y] = depthRange;
            }
        }
        return depth;
    }

    //for PLY
    protected static short[][] depth16ToDepth16Array(ByteBuffer byteBuffer, int w, int h) {
        byteBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sBuffer = byteBuffer.asShortBuffer();
        short[][] depth = new short[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                short depthSample = sBuffer.get();
                depth[x][y] = depthSample;
            }
        }
        return depth;
    }

    protected static Bitmap depthArrayToFancyRgbHueBitmap(short[][] depth, int w, int h) {
        // Transform depth array to nice image for debugging purpose
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int val = depth[x][y]; //between 60 and 4000;
//                int hue = normalize(val, min,max, 0, 360);
                int hue = val % 360;
                int c = Color.HSVToColor(new float[]{hue,1f, 1f});
//                int gray = normalize(val, 60,4000, 0, 255);
//                int c = Color.rgb(gray,gray,gray);
//                int c = normalize(val, 60,5000, Color.BLACK, Color.WHITE);
                bitmap.setPixel(x, y, c);
            }
        }
        return bitmap;
    }

    private static int normalize(int value, int inputMin, int inputMax, int outputMin, int outputMax) {
        int diffInput = inputMax-inputMin;
        int diffOutput = outputMax-outputMin;
        return ((value-inputMin)*diffOutput/diffInput)+outputMin; //order changed to avoid float
    }

    //Wavefront
    public static void writeObj(ARSceneMesh arSceneMesh, File f) {
        FloatBuffer fb = arSceneMesh.getVertices();
        if(fb.remaining() == 0) return; //nothing yet to save
        int i=0;

        StringBuilder text = new StringBuilder();
        while(fb.hasRemaining()) {
            //fb.position();
            float value = fb.get();
            if(i%3==0) text.append("\nv ");
            text.append(value).append(" ");
            i++;
        }
        text.append("\n# vertices=").append(i/3);
/*
        i=0;
        IntBuffer ib = arSceneMesh.getTriangleIndices();
        while(ib.hasRemaining()) {
            int value = ib.get();
            if(i%3==0) text.append("\nf ");
            text.append(value+1).append(" ");
            i++;
        }
        text.append("\n# faces=").append(i/3);
*/

        IoUtils.writeBytes(f, text.toString().getBytes());
    }

    public static void writeObj(ARPointCloud arPointCloud, File f) {
        FloatBuffer fb = arPointCloud.getPoints();
        if(fb.remaining() == 0) return; //nothing yet to save
        // TODO
    }

    public static void writePly(Image acquireDepthImage, Image acquirePreviewImage, File file) {
        ByteBuffer depthBuffer = acquireDepthImage.getPlanes()[0].getBuffer();

        byte[] bytesYuvNv21 = YUV_420_888toNV21(acquirePreviewImage);
        Bitmap bitmap = n21ToBitmap(bytesYuvNv21, acquirePreviewImage.getWidth(), acquirePreviewImage.getHeight());

        writePly(depthBuffer, bitmap, file);
    }

    public static void writePly(ByteBuffer depthBuffer, Bitmap bitmap, File plyOutput) {
        final String HEADER_PLY = "ply\n" +
                "format ascii 1.0\n" +
                "element vertex %d\n" +
                "property float x\n" +
                "property float y\n" +
                "property float z\n" +
                "property uchar red\n" +
                "property uchar green\n" +
                "property uchar blue\n" +
                "end_header\n";
        int w = 240; //TODO that data must be params
        int h = 180;
        float fx = 248; //mean 1m deep is 0.54 meter max left? was 548
        float fy = 248;
        int wcolor = 1440;
        int hcolor = 1080;

        //depth
        short[][] depth = ImageUtils.depth16ToDepthRangeArray(depthBuffer, w, h);


        StringBuffer sb = new StringBuffer();

        //TODO take into account translation + rotation between 2 cameras (rgb & d)

        int points = 0;
        for (int x = 0; x<w ; x++) {
            for(int y = 0; y<h; y++) {
                float z = depth[x][y] / 1000f; //base unit is meter, not millimeter
                if(z == 0f || z>2f) continue;
                int cx = x - w/2;
                int cy = y - h/2;

                int xcolor = x*6; //color image is 6 times bigger
                int ycolor = y*6;

                int pixel = bitmap.getPixel(x*6, y*6); //6 times better res
                int redValue = Color.red(pixel);
                int blueValue = Color.blue(pixel);
                int greenValue = Color.green(pixel);

                float xw = (float)cx * z / fx;
                float yw = (float)cy * z / fy;
                sb.append(xw+" "+yw+" "+z+" "+ " "+redValue+" "+blueValue+" "+greenValue+"\n"); // # "+x+" "+y+"_"+cx+" "+cy+"\n");
                points++;
            }
        }
        sb.insert(0, String.format(HEADER_PLY, points));
        try {
            Files.write(Paths.get(plyOutput.getPath()), sb.toString().getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Fail saving ply file:" + plyOutput.getPath());
        }
    }

    private static Bitmap jpgToBitmap(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    // TODO do better conversion
    protected static Bitmap n21ToBitmap(byte[] nv21, int width, int height){
        YuvImage yuvimage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
        byte[] jdata = baos.toByteArray();
        BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888; //TODO not the right colors, because of that?
//        bitmapFatoryOptions.
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bitmapFatoryOptions);
        return bmp;

        // TODO avoid going through JPEG
//        int colorY = n21[wcolor*hcolor*0 + xcolor +ycolor*1440]; //1555200 bytes
//        int colorCr = n21[wcolor*hcolor*1 + xcolor+ycolor*720]; //777599 bytes (1px = 0.5byte)
//        int colorCb = n21[(int)(wcolor*hcolor*1.5 +xcolor+ycolor*720)];
//
//        int redValue = (int) (colorY + 1.402*(colorCr-128));
//        int blueValue = (int) (colorY - 0.34414*(colorCb-128)-0.71414*(colorCr-128));
//        int greenValue = (int) (colorY + 1.772*(colorCb-128));
    }

    protected static byte[] n21ToRgb(byte[] yuvn21, int width, int height) {
        return null;
    }


}
