package com.huawei.arengine.demos.java.recorder;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

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

    public void multiplytest() {
        double[] vector = this.getArray4d();

        double[][] matrix = new double[][]{
                { 1f, 0f, 0f, 0f},
                { 0f, 1f, 0f, 0f},
                { 0f, 0f, 1f, 0f},
                { 0f, 0f, 0f, 1f}
        };

        double[] vector2 = multiply(matrix, vector);

        this.set(vector2);

        Vector3f v;
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

    public void setNormalize(float[] v){
        this.x = v[0]/v[3]; //and normalize
        this.y = v[1]/v[3];
        this.z = v[2]/v[3];
    }

    public float[] getArray4f(){
        return new float[]{this.x, this.y, this.z, 1f};
    }

    public double[] getArray4d(){
        return new double[]{this.x, this.y, this.z, 1d};
    }

    protected Point3d getPoint3d() { return new Point3d(this.x, this.y, this.z); }

    protected Vector3d getVector3d(){
        return new Vector3d(this.x, this.y, this.z);
    }

    protected void setVector3d(Vector3d v) {
        this.x = (float)v.x;
        this.y = (float)v.y;
        this.z = (float)v.z;
    }

    //https://www.codota.com/web/assistant/code/rs/5c65b48c1095a5000172e3c9#L711
    public void rotate(double yaw, double pitch, double roll) {
        double cosc = Math.cos(yaw);
        double sinc = Math.sin(yaw);
        double cosb = Math.cos(pitch);
        double sinb = Math.sin(pitch);
        double cosa = Math.cos(roll);
        double sina = Math.sin(roll);
//        Matrix3d rotationMatrixToPack = new Matrix3d();

        double[][] matrix = new double[][]{
                {cosc * cosb, cosc * sinb * sina - sinc * cosa, cosc * sinb * cosa + sinc * sina, 0f},
                { sinc * cosb, sinc * sinb * sina + cosc * cosa, sinc * sinb * cosa - cosc * sina, 0f},
                { -sinb, cosb * sina, cosb * cosa, 0f},
                { 0f, 0f, 0f, 1f}
        };

        // Introduction to Robotics, 2.64
//        rotationMatrixToPack.setElement(0, 0, cosc * cosb);
//        rotationMatrixToPack.setElement(0, 1, cosc * sinb * sina - sinc * cosa);
//        rotationMatrixToPack.setElement(0, 2, cosc * sinb * cosa + sinc * sina);
//        rotationMatrixToPack.setElement(1, 0, sinc * cosb);
//        rotationMatrixToPack.setElement(1, 1, sinc * sinb * sina + cosc * cosa);
//        rotationMatrixToPack.setElement(1, 2, sinc * sinb * cosa - cosc * sina);
//        rotationMatrixToPack.setElement(2, 0, -sinb);
//        rotationMatrixToPack.setElement(2, 1, cosb * sina);
//        rotationMatrixToPack.setElement(2, 2, cosb * cosa);



       double[] v = multiply(matrix, this.getArray4d());

       this.set(v);

    }

    protected void applyPose(Quat4d q, Vector3d v, double scale) {
        Matrix4d m4 = new Matrix4d();
        Point3d p = this.getPoint3d();
        m4.set(q, v, scale);
        m4.transform(p);

        this.x = (float)p.x;
        this.y = (float)p.y;
        this.z = (float)p.z;
    }

    protected void setRgb(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    protected void setRgb(int rgb) {
        this.r = (rgb >> 16) & 0xFF;
        this.g = (rgb >> 8) & 0xFF;
        this.b = rgb & 0xFF;
    }

    protected void rotateQ(Quat4d q) {


        Vector3d v = this.getVector3d();

        Vector3d u = new Vector3d(q.x, q.y, q.z);
        double s = q.w;
        u.dot(v);

//        Vector3d v2 = 2.0f * u.dot(v) * u
//                + (s*s - u.dot(u)) * v
//                + 2.0f * s * cross(u, v);

    }

    protected static double[] multiply(double[][] a, double[] x) {
        int m = a.length;
        int n = a[0].length;
        if (x.length != n) throw new RuntimeException("Illegal matrix dimensions.");
        double[] y = new double[m];
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                y[i] += a[i][j] * x[j];
        return y;
    }

    public static void addTetrahedron(List<PlyProp> props) {
        List<PlyProp> tetrahedron = PlyProp.getTetrahedron(0.005f);
        for (PlyProp prop: tetrahedron) {
            prop.setRgb(Color.RED);
        }
        props.addAll(tetrahedron);
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

    protected static List<PlyProp> getTetrahedron(float scale) {
        List<PlyProp> props = new ArrayList<>();
        props.add(new PlyProp(0,0,0));
        props.add(new PlyProp(-2f*scale, -1.5f*scale,3f*scale));
//        props.add(new PlyProp(0f*scale, -1.5f*scale,3f*scale));
        props.add(new PlyProp(2f*scale, -1.5f*scale, 3f*scale));
//        props.add(new PlyProp(2f*scale, 0f*scale, 3f*scale));
        props.add(new PlyProp(2f*scale, 1.5f*scale, 3f*scale));
//        props.add(new PlyProp(0f*scale, 1.5f*scale, 3f*scale));
        props.add(new PlyProp(-2f*scale, 1.5f*scale, 3f*scale));
//        props.add(new PlyProp(-2f*scale, 0f*scale, 3f*scale));
        return props;
    }
}
