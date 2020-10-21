package com.huawei.arengine.demos.java.recorder;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;
import com.huawei.hiar.ARPointCloud;
import com.huawei.hiar.ARSceneMesh;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class ImageUtils {
    private static final String TAG = ImageUtils.class.getSimpleName();

    private static byte[] YUV_420_888toNV21(Image image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
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

    public static void writeImageYuv(Image image, File f) {
        if(image.getFormat() != ImageFormat.YUV_420_888)
            throw new RuntimeException("Expected image format is YUV_420_888, but is:"+image.getFormat());

        byte[] nv21 = YUV_420_888toNV21(image);
        IoUtils.writeYUV(f, nv21, image.getWidth(), image.getHeight());
        Log.i(TAG, "Image ("+image.getWidth()+"x"+image.getHeight()+") saved in " +f.getPath());
    }

    public static void writeImageDepth(Image image, File f) {
        if(image.getFormat() != ImageFormat.DEPTH16)
            throw new RuntimeException("Expected image format is DEPTH16, but is:"+image.getFormat());

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sBuffer = buffer.asShortBuffer();

        int width = image.getWidth();
        int height = image.getHeight();
        //        int stride = plane.getRowStride(); //why? always width/2?

//        short depth16[] = new short[width*height]; //TODO check if quicker using that array
//        sBuffer.get(depth16);

        short[][] depth = new short[width][height];
        Short min = null;
        Short max = null;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
//                short depthSample = depth16[y*width + x];
                short depthSample = sBuffer.get();
                short depthRange = (short) (depthSample & 0x1FFF);
                if(depthRange != 0) {
                    if(min == null || depthRange < min) min = depthRange;
                    if(max == null || depthRange > max) max = depthRange;
                }
//                short depthConfidence = (short) ((depthSample >> 13) & 0x7);
                depth[x][y] = depthRange;
            }
        }

        // Transform depth array to nice image for debugging purpose
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

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

        IoUtils.writePNG(f, bitmap);
        Log.i(TAG, "Image depth ("+image.getWidth()+"x"+image.getHeight()+") saved in " +f.getPath());
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
}
