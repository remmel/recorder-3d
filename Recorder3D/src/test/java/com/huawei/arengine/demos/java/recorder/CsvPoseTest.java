package com.huawei.arengine.demos.java.recorder;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

public class CsvPoseTest {
    private static final String POSES = "src/test/resources/poses.csv";

    @Test
    public void fromCsvRows() throws IOException {
        List<String> rows = Files.readAllLines(Paths.get(POSES));
        List<CsvPose> poses = CsvPose.fromCsvRows(rows);
        CsvPose p = poses.get(2);
        assertEquals("frameId", "00000036", p.frameId);
        assertEquals(-176.63390317955128d, p.yaw, 0d);

        List<String>rows2 = CsvPose.toCsvRows(poses);
        assertEquals(rows, rows2);
    }
}