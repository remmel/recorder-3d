package com.huawei.arengine.demos.java.recorder;

import java.util.List;

public class PlyProp {


    public float x;
    public float y;
    public float z;
    public int r;
    public int g;
    public int b;

    final static String HEADER_PLY = "ply\n" +
            "format ascii 1.0\n" +
            "element vertex %d\n" +
            "property float x\n" +
            "property float y\n" +
            "property float z\n" +
            "property uchar red\n" +
            "property uchar green\n" +
            "property uchar blue\n" +
            "end_header\n";

    public PlyProp(float x, float y, float z, int red, int green, int blue) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = red;
        this.g = green;
        this.b = blue;
    }

    public static String toString(List<PlyProp> props) {
        StringBuffer sb = new StringBuffer();
        sb.insert(0, String.format(HEADER_PLY, props.size()));

        for (PlyProp p: props) {
            sb.append(p.x+" "+p.y+" "+p.z+" "+ " "+p.r+" "+p.g+" "+p.b+"\n");
        }
        return sb.toString();
    }
}
