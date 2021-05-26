package com.remmel.recorder3d.recorder.video;

import android.media.Image;

import java.nio.ByteBuffer;

public class BytesImage {
    public byte[] nv21;
    public int w;
    public int h;

    public BytesImage(Image image) {
        nv21 = imageToBytes(image);
        w = image.getWidth();
        h = image.getHeight();
    }

    public static byte[] imageToBytes(Image image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining(); //1555199 bytes
        int uSize = uBuffer.remaining(); //777599 bytes
        int vSize = vBuffer.remaining(); //777599 bytes

        nv21 = new byte[ySize + uSize + vSize]; // 76800+38399+38399=153598

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        //compare size with usage; why java.nio.BufferOverflowException?
        return nv21;
    }
}
