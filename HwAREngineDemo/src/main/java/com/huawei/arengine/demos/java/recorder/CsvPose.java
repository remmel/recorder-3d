package com.huawei.arengine.demos.java.recorder;

import com.huawei.hiar.ARPose;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4f;

public class CsvPose {
    private static final String HEADER = "frame,tx,ty,tz,qx,qy,qz,qw,yaw,pitch,roll,projection";

    protected String frameId;
    public ARPose p;
    protected double yaw;
    protected double pitch;
    protected double roll;
    protected float[] projectionMatrix;

    private static String DELIMITER = ",";

    ////north : https://github.com/google-ar/arcore-android-sdk/issues/119
    public CsvPose(String frameId, ARPose pose, float[] projectionMatrix) {
        this.frameId = frameId;
        this.p = pose;

        Vector4f q = new Vector4f(pose.qx(), pose.qy(), pose.qz(), pose.qw());
        // https://answers.unity.com/questions/416169/finding-pitchrollyaw-from-quaternions.html
        pitch = Math.toDegrees(Math.atan2(2 * q.x * q.w - 2 * q.y * q.z, 1 - 2 * q.x * q.x - 2 * q.z * q.z));
        yaw = Math.toDegrees(Math.atan2(2 * q.y * q.w - 2 * q.x * q.z, 1 - 2 * q.y * q.y - 2 * q.z * q.z));
        roll = Math.toDegrees(Math.asin(2 * q.x * q.y + 2 * q.z * q.w));

        this.projectionMatrix = projectionMatrix;
    }

    public CsvPose(String row) {
        String cols[] = row.split(DELIMITER);
        frameId = cols[0];
        p = new ARPose(
                new float[]{Float.parseFloat(cols[1]), Float.parseFloat(cols[2]), Float.parseFloat(cols[3])},
                new float[]{Float.parseFloat(cols[4]), Float.parseFloat(cols[5]), Float.parseFloat(cols[6]), Float.parseFloat(cols[7])}
        );
        if(cols.length > 8) {
            yaw = Double.parseDouble(cols[8]);
            pitch = Double.parseDouble(cols[9]);
            roll = Double.parseDouble(cols[10]);
            projectionMatrix = parseFloatList(cols[11]);
        }
    }

    //better in java8:  Arrays.stream(cols[11].split(" ")).mapToInt(num ->  Float.parseFloat(num)).toArray();
    protected static float[] parseFloatList(String numbers) {
        String[] numbersArr =  numbers.split(" ");
        float[] floatsArr = new float[numbersArr.length];
        int i = 0;
        for (String num : numbersArr) {
            floatsArr[i] = Float.parseFloat(num);
            i++;
        }
        return floatsArr;
    }

    public Vector3d getPosition() {
        return new Vector3d(this.p.tx(), this.p.ty(), this.p.tz());
    }

    protected Quat4d getQuat() {
        return new Quat4d(this.p.qx(), this.p.qy(), this.p.qz(), this.p.qw());
    }

    public String toCsvRow() {
        StringBuilder sb = new StringBuilder();
        sb.append(frameId).append(DELIMITER)
                .append(p.tx()).append(DELIMITER)
                .append(p.ty()).append(DELIMITER)
                .append(p.tz()).append(DELIMITER)
                .append(p.qx()).append(DELIMITER)
                .append(p.qy()).append(DELIMITER)
                .append(p.qz()).append(DELIMITER)
                .append(p.qw()).append(DELIMITER)
                .append(yaw).append(DELIMITER)
                .append(pitch).append(DELIMITER)
                .append(roll).append(DELIMITER);

        return sb.toString() + join(projectionMatrix, ' ');
    }

    protected static String join(float[] values, char delimiter) {
        StringBuilder sb = new StringBuilder();
        for (float v : values) {
            sb.append(v).append(delimiter);
        }
        return sb.toString();
    }

    public String toString() {
        String str = String.format("Pose t: %.3f %.3f %.3f \nq: %.3f %.3f %.3f %.3f\n", p.tx(), p.ty(), p.tz(), p.qx(), p.qy(), p.qz(), p.qw());
        String angles = String.format("Pitch: %.3f Yaw: %.3f Roll: %.3f", pitch, yaw, roll);
        return str+angles;
    }

    public static List<String> toCsvRows(List<CsvPose> poses) {
        List<String> rows = new ArrayList<>(poses.size()+1);
        rows.add(CsvPose.HEADER);
        for (CsvPose pose : poses) {
            String row = pose.toCsvRow();
            rows.add(row);
        }
        return rows;
    }

    public static List<CsvPose> fromCsvRows(List<String> rows) {
        List<CsvPose> poses = new ArrayList<>(rows.size()+1);
        boolean isHeader = true;
        for (String row : rows) {
            if(isHeader) {isHeader=false; continue;}
            poses.add(new CsvPose(row));
        }
        return poses;
    }
}
