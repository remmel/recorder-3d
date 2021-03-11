package com.huawei.arengine.demos.java.recorder;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

// if need to convert a whole directory
public class BulkDemoTest {

    @Test
    public void bulkconvertDepth() throws IOException {
        org.bytedeco.javacpp.Loader.load(org.bytedeco.javacpp.opencv_java.class);
        ImageUtils.writeDepth16binInPng16GrayscaleTumBulk("E:\\dataset\\2020-12-17_141504");
    }

    @Test
    public void bulkResizeRgb() {
        org.bytedeco.javacpp.Loader.load(org.bytedeco.javacpp.opencv_java.class);

        String dir = "E:\\dataset\\2020-12-17_141504";
        int w = 640, h = 480;

        Arrays.stream(new File(dir).list())
                .filter(s -> s.endsWith("_depth16.bin.png"))
                .forEach(fn ->
                        ImageUtils.resizeDepthPng(dir + "/" + fn,
                                dir + "/" + fn.substring(0,8) + "_image" + w + ".jpg"
                                , w, h)
                );
    }

    @Test
    public void bulkresizePngDepth() {
        org.bytedeco.javacpp.Loader.load(org.bytedeco.javacpp.opencv_java.class);

        String dir = "E:\\dataset\\2020-12-17_141504";
        int w = 320, h = 240;

        Arrays.stream(new File(dir).list())
                .filter(s -> s.endsWith("_depth16.bin.png"))
                .forEach(fn ->
                        ImageUtils.resizeDepthPng(dir + "/" + fn,
                                dir + "/" + fn.substring(0, 8) + "_depth24_" + w + ".bin.png"
                                , w, h)
                );
    }
}
