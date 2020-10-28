package com.huawei.arengine.demos.java.recorder;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ImageUtilsTest {
    private static final String DEPTH16 = "src/test/resources/00000012.depth16.bin";
    private static final String PLY = "src/test/resources/00000012.ply";
    private static final String JPEG = "src/test/resources/00000012_image.jpg"; //1440x1080

//    private static final String DEPTH16 = "C:/Users/remme/workspace/dataset/2020-10-26_111919/00000012.depth16";
//    private static final String PLY = "C:/Users/remme/workspace/dataset/2020-10-26_111919/00000012.ply";
    private static final int W = 240; //width of depth16
    private static final int H = 180; //height of depth16

    @Test
    public void testCenterDepthRange() throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(DEPTH16));
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        short[][] depth = ImageUtils.depth16ToDepthRangeArray(buffer, W, H);

        short center = depth[W/2][H/2];

        assertEquals("depth(mm)", 977, center);
    }
}