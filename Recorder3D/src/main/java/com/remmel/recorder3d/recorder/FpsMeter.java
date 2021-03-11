package com.remmel.recorder3d.recorder;

public class FpsMeter {
    protected short frames = 0;
    private long lastInterval;
    private float fps;
    private static final float UPDATE_INTERVAL = 0.5f; //update fps returned only after 0.5s

    public float doFpsCalculate() {
        ++frames;
        long timeNow = System.currentTimeMillis();

        // Convert millisecond to second.
        if (((timeNow - lastInterval) / 1000.0f) > UPDATE_INTERVAL) {
            fps = frames / ((timeNow - lastInterval) / 1000.0f);
            frames = 0;
            lastInterval = timeNow;
        }
        return fps;
    }
}
