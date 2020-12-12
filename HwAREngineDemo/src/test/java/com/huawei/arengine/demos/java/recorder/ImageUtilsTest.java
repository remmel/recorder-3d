package com.huawei.arengine.demos.java.recorder;

import android.graphics.Color;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

/**
 * That test will be run on the computer, use the other ImageUtilsTest to run on the android device
 */
public class ImageUtilsTest {
    private static final String DIR = "src/test/resources";
    private static final String DEPTH16 = DIR + "/00000012_depth16.bin";
    private static final String PLY = DIR + "/00000012.ply";
    private static final String JPEG = DIR + "/00000012_image.jpg"; //1440x1080
    private static final String POSES = DIR + "/poses2.csv";
    private static final int W = 240; //width of depth16
    private static final int H = 180; //height of depth16

    private static final int[] COLORS = new int[]{Color.RED, Color.GREEN, Color.BLUE, Color.BLACK, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.WHITE};

    @Test
    public void testCenterDepthRange() throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(DEPTH16));
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        short[][] depth = ImageUtils.depth16ToDepthRangeArray(buffer, W, H);

        short center = depth[W/2][H/2];

        assertEquals("depth(mm)", 1012, center);
    }

    protected List<PlyProp> getTetrahedronApplyPoseAndRgb(Quat4d q, Vector3d v, double scale, int rgb) {
        List<PlyProp> props = PlyProp.getTetrahedron();
        for (PlyProp p : props) {
            p.applyPose(q ,v, scale);
            p.setRgb(rgb);
        }
       return props;
    }

    /**
     * Create multiples tetrahedrons
     */
    @Test
    public void createPlyTetrahedrons() {
        List<PlyProp> world = new ArrayList<>();

        Quat4d q = new Quat4d(0.01591082,0.983769,0.17815615,-0.0141543); //36

        world.addAll(getTetrahedronApplyPoseAndRgb(q, new Vector3d(0, 0, 1), 1, Color.RED));
        world.addAll(getTetrahedronApplyPoseAndRgb(q, new Vector3d(0, 0, 2), 1, Color.GREEN));
        world.addAll(getTetrahedronApplyPoseAndRgb(q, new Vector3d(0, 0, 3), 1, Color.BLUE));
        world.addAll(getTetrahedronApplyPoseAndRgb(q, new Vector3d(0, 0, 4), 1, Color.YELLOW));

        IoUtils.writeBytes(new File(DIR+"/tmp_tetrahedrons_manual.ply"), PlyProp.toString(world).getBytes());
    }

    /**
     * Create multiples tetrahedrons following poses.csv
     */
    @Test
    public void createPlyTetrahedronsFromPosesCsv() throws IOException {
        List<PlyProp> world = new ArrayList<>();

        //"src/test/resources/poses.csv"
        List<String> rows = Files.readAllLines(Paths.get("C:\\Users\\remme\\workspace\\dataset\\2020-11-17_122922\\poses.csv"));
        List<CsvPose> poses = CsvPose.fromCsvRows(rows);

        int i = 0;
        for (CsvPose pose: poses) {
            List<PlyProp> tetrahedron = getTetrahedronApplyPoseAndRgb(pose.getQuat(),pose.getPosition(), 0.05d, getColor(i++));
            world.addAll(tetrahedron);
        }

        IoUtils.writeBytes(new File(DIR+"/tmp_tetrahedrons_posescsv.ply"), PlyProp.toString(world).getBytes());
    }

    protected int getColor(int i){
        int nb = COLORS.length;
        return COLORS[i%nb];
    }
}