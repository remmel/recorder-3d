package com.huawei.arengine.demos.java.recorder;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ImageUtilsTest {
    public static final String DEPTH16 = "src/test/resources/00000012.depth16";

    @Test
    public void testCenterDepthRange() throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(DEPTH16));
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        short[][] depth = ImageUtils.depth16ToDepthArray(buffer, 240, 180);

        short center = depth[120][90];

        assertEquals("depth(mm)", 984, center);
//        Bitmap bitmap = ImageUtils.depthArrayToFancyRgbHueBitmap(depth, 240, 180);
//        IoUtils.writePNG(new File(DEPTHIMG), bitmap);
    }

//    protected static void writeImage() {
//        img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
//        for (int x = 0; x < img.getWidth(); x++) {
//            for (int y = 0; y < img.getHeight(); y++) {
//                img.setRGB(x, y, Color.RED.getRGB());
//            }
//        }
//    }
}