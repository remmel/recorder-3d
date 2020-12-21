package com.huawei.arengine.demos.java.recorder;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

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

    //https://github.com/kotlin-graphics/glm
    public PlyProp(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = 0;
        this.g = 0;
        this.b = 0;
    }

    public PlyProp(float x, float y, float z, int color) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.setRgb(color);
    }

    public PlyProp(float x, float y, float z, int red, int green, int blue) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = red;
        this.g = green;
        this.b = blue;
    }

    public static String toString(List<PlyProp> props) {
        StringBuffer sb = new StringBuffer(props.size());
        sb.insert(0, String.format(HEADER_PLY, props.size()));

        for (PlyProp p: props) {
            sb.append(p.x+" "+p.y+" "+p.z+" "+ " "+p.r+" "+p.g+" "+p.b+"\n");
        }
        return sb.toString();
    }

    public void set(float[] v){
        this.x = v[0];
        this.y = v[1];
        this.z = v[2];
    }

    public void set(double[] v){
        this.x = (float)v[0];
        this.y = (float)v[1];
        this.z = (float)v[2];
    }

    protected Point3d getPoint3d() { return new Point3d(this.x, this.y, this.z); }

    protected void applyPose(Quat4d q, Vector3d v, double scale) {
        Matrix4d m4 = new Matrix4d();
        Point3d p = this.getPoint3d();
        m4.set(q, v, scale);
        m4.transform(p);

        this.x = (float)p.x;
        this.y = (float)p.y;
        this.z = (float)p.z;
    }

    protected void setRgb(int rgb) {
        this.r = (rgb >> 16) & 0xFF;
        this.g = (rgb >> 8) & 0xFF;
        this.b = rgb & 0xFF;
    }

    protected static List<PlyProp> getTetrahedron() {
        List<PlyProp> props = new ArrayList<>();
        props.add(new PlyProp(0,0,0));
        props.add(new PlyProp(-.2f, -.15f,.3f));
        props.add(new PlyProp(.2f, -.15f, .3f));
        props.add(new PlyProp(.2f, .15f, .3f));
        props.add(new PlyProp(-.2f, .15f, .3f));
        return props;
    }

    protected static List<PlyProp> getTetrahedron(float scale, int color) {
        List<PlyProp> props = new ArrayList<>();
        props.add(new PlyProp(0,0,0, color));
        props.add(new PlyProp(-2f*scale, -1.5f*scale,3f*scale, color));
//        props.add(new PlyProp(0f*scale, -1.5f*scale,3f*scale, color));
        props.add(new PlyProp(2f*scale, -1.5f*scale, 3f*scale, color));
//        props.add(new PlyProp(2f*scale, 0f*scale, 3f*scale, color));
        props.add(new PlyProp(2f*scale, 1.5f*scale, 3f*scale, color));
//        props.add(new PlyProp(0f*scale, 1.5f*scale, 3f*scale, color));
        props.add(new PlyProp(-2f*scale, 1.5f*scale, 3f*scale, color));
//        props.add(new PlyProp(-2f*scale, 0f*scale, 3f*scale, color));
        return props;
    }
}
