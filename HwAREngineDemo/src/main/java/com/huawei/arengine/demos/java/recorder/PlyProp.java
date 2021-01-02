package com.huawei.arengine.demos.java.recorder;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
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
            "element vertex %010d\n" + //to force having fixed header size
            //I could put spaces instead of 0s, check if it works on main software (Meshlab & CloudCompare ok).
            // In order to be able to write over the header
            "property float x\n" +
            "property float y\n" +
            "property float z\n" +
            "property uchar red\n" +
            "property uchar green\n" +
            "property uchar blue\n" +
            "end_header\n";

    final static String HEADER_PLY_BIN = "ply\n" +
            "format binary_big_endian 1.0\n" +
            "element vertex %010d\n" + //to force having fixed header size, see above
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

    public String toString() {
        return this.x+" "+this.y+' '+this.z+' '+this.r+' '+this.g+' '+this.b+'\n';
    }

    public byte[] toBytes(boolean isAscii) {
        if(isAscii) {
            //string concatenation with "+" same speed as StringBuffer
            return this.toString().getBytes();
        } else {
            return ByteBuffer.allocate(3*4+3).putFloat(this.x).putFloat(this.y).putFloat(this.z)
                    .put((byte)this.r).put((byte)this.g).put((byte)this.b)
                    .array();
        }
    }

    public static String getHeader(int size, boolean isAscii) {
        return String.format(isAscii ? HEADER_PLY : HEADER_PLY_BIN, size);
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
