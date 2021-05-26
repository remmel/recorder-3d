package com.remmel.recorder3d.recorder;

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

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ImageUtils {
    private static final String TAG = ImageUtils.class.getSimpleName();

    private static byte[] YUV_420_888toNV21(Image image) {
        assert(image.getFormat() == ImageFormat.YUV_420_888);
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

    public static Bitmap toBitmap(Image image) {
        byte[] bytesYuvNv21 = ImageUtils.YUV_420_888toNV21(image);
        return ImageUtils.n21ToBitmapViaJpg(bytesYuvNv21, image.getWidth(), image.getHeight());
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

        IoUtils.writeBitmapAsPng(f, bitmap);
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

    protected static short[] depth16ToDepthRangeArray2(ByteBuffer byteBuffer, int w, int h) {
        byteBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sBuffer = byteBuffer.asShortBuffer();
        short[] depth = new short[w*h];
//        short d = sBuffer.get(21690);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                short depthSample = sBuffer.get(); //depth16[y*width + x];
                short depthRange = (short) (depthSample & 0x1FFF);
//                short depthConfidence = (short) ((depthSample >> 13) & 0x7);
                depth[y*w + x] = depthRange;
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

    public static void writePly(Image acquireDepthImage, Image acquirePreviewImage, File f) {
        ByteBuffer depthBuffer = acquireDepthImage.getPlanes()[0].getBuffer();
        Bitmap rgbBitmap = ImageUtils.toBitmap(acquirePreviewImage);
        PlyUtils.writePly(PlyUtils.getPly(depthBuffer, rgbBitmap), f.getPath(), true);
    }

    /**
     * Write the mesh of the current frame generated by AREngine as .obj
     * Flag must be set ARConfigBase.ENABLE_MESH
     * ARSceneMesh contains also the depth array (240x180) - not used here
     * https://developer.huawei.com/consumer/en/doc/development/HMS-Plugin-References-V1/arscenemesh-0000001058295697-V1
     */
    public static void writeObj(ARSceneMesh arSceneMesh, File f) {
        FloatBuffer fb = arSceneMesh.getVertices();
        if(fb.remaining() == 0) return; //nothing yet to save

        StringBuilder text = new StringBuilder();

        float dstV[] = new float[3];
        text.append("# vertices=").append(fb.remaining()/3).append('\n'); //fb.remaining()%3 == 0
        while(fb.hasRemaining()) {
            fb.get(dstV);
            text.append(String.format("v %f %f %f\n", dstV[0], dstV[1], dstV[2]));
        }

        IntBuffer ib = arSceneMesh.getTriangleIndices();
        text.append("# faces=").append(ib.remaining()/3).append('\n');
        int dstT[] = new int[3];
        while(ib.hasRemaining()) {
            ib.get(dstT);
            text.append(String.format("f %d %d %d\n", dstT[0]+1, dstT[1]+1, dstT[2]+1));
        }

        IoUtils.writeBytes(f, text.toString().getBytes());
        arSceneMesh.release();
    }

    /**
     * Works sometimes, don't know when/why; provides a point cloud of ~300 points of last poses (merge last depths I guess)
     * This is not enough to be able to do something
     */
    public static void writePly(ARPointCloud arPointCloud, File f) {
        FloatBuffer fb = arPointCloud.getPoints();
        if(fb.remaining() == 0) {
            arPointCloud.release();
            return; //nothing yet to save
        }

        float dst[] = new float[4];
        List<PlyProp> props = new ArrayList<>(fb.remaining());
        while(fb.hasRemaining()) {
           fb.get(dst);
           props.add(new PlyProp(dst[0], dst[1], dst[2]));
        }
        arPointCloud.release();
        PlyUtils.writePly(props, f.getPath(), false);
    }

    protected static Bitmap jpgToBitmap(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    // TODO do better conversion
    // https://github.com/silvaren/easyrs/blob/master/easyrs/src/main/java/io/github/silvaren/easyrs/tools/Nv21Image.java
    // https://stackoverflow.com/questions/32276522/convert-nv21-byte-array-into-bitmap-readable-format
    //opencv  Imgproc.cvtColor / Imgproc.COLOR_YUV2RGBA_NV21
    //RenderScript  ScriptIntrinsicYuvToRGB
    protected static Bitmap n21ToBitmapViaJpg(byte[] nv21, int width, int height){
        YuvImage yuvimage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
        byte[] jdata = baos.toByteArray();
        BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bitmapFatoryOptions);
        return bmp;
    }

    protected static Bitmap n21ToBitmapViaDecode(byte[] nv21, int w, int h) {
        int rgb[] = decodeYUV420SP(nv21, w, h);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(rgb, 0, w, 0, 0, w, h);
        return bitmap;
    }

    // code from Ketai project n21ToRgb
    protected static int[] decodeYUV420SP(byte[] yuv420sp, int width, int height) {

        final int frameSize = width * height;

        int[] rgb = new int[frameSize];

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                r = r > 262143 ? 262143 : r < 0 ? 0 : r; //clamp(r, 0, 262143)
                g = g > 262143 ? 262143 : g < 0 ? 0 : g;
                b = b > 262143 ? 262143 : b < 0 ? 0 : b;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
        return rgb;
    }

    public static void writeDepth16binInPng16GrayscaleTum(String bin, int width, int height, String png) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(bin));
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sBuffer = buffer.asShortBuffer();
        short[] depthTum = new short[width*height];

        Mat mat = Mat.eye(height, width, CvType.CV_16UC1); //max is 65536 == 65meters / 16 bits = 2 bytes

        int i=0;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                short depthSample = sBuffer.get(); //depth16[y*width + x];
                short depthMm = (short) (depthSample & 0x1FFF);
//                short depthConfidence = (short) ((depthSample >> 13) & 0x7);
                depthTum[h*width+w] = (short)(depthMm * 5); //tum rgbd is 5==1mm / 5000==1m
            }
        }

        mat.put(0, 0, depthTum);
        Imgcodecs.imwrite(png, mat);
        //buffer.clear();
    }

    public static void writeDepthArrayInPng16GrayscaleTum(short[] depthInMm, int width, int height, String png) {
        Mat mat = Mat.eye(height, width, CvType.CV_16UC1); //max is 65536 == 65meters / 16 bits = 2 bytes
//        int size = height*width;
//        for(int i=0; i< size; i++) //more efficent way to do that with opencv?
//            depthInMm[i]*=5; //FIXME why I don't need to multiply that by 5, as I want it in "tum" format where 5000 = 1m, see test
        mat.put(0, 0, depthInMm);
        Imgcodecs.imwrite(png, mat);
    }

    public static void writeDepth16binInPng16GrayscaleTumBulk(String dir) throws IOException {
        File d = new File(dir);

        String[] filenames = d.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("_depth16.bin");
            }
        });

        for (String binfn: filenames) {
            writeDepth16binInPng16GrayscaleTum(dir + "/" + binfn,240, 180, dir + "/" + binfn + "640.png");
        }
    }

    public static void resizeRgb(String srcPath, String dstPath, int w, int h) {
        Mat src = Imgcodecs.imread(srcPath);
        Mat dst = new Mat();
        Imgproc.resize(src, dst, new Size(w, h));
        Imgcodecs.imwrite(dstPath, dst);
    }

    public static void resizeDepthPng(String srcPath, String dstPath, int w, int h) {
        Mat src = Imgcodecs.imread(srcPath);
        Mat dst = new Mat(w, h, CvType.CV_16UC1);
        Imgproc.resize(src, dst, new Size(w, h));
        Imgcodecs.imwrite( dstPath, dst);
    }
}
