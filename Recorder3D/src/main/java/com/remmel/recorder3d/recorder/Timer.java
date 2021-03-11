package com.remmel.recorder3d.recorder;

/**
 * Usage:
 * Timer timer = new Timer();
 * step1();
 * Log.e(TAG, "step1: " + timer.getElapsedSeconds()+"s");
 * step2();
 * Log.e(TAG, "step2: " + timer.getElapsedSeconds()+"s");
 */
public class Timer {
    long t = System.currentTimeMillis();

    double getElapsedSeconds() {
        long tCur = System.currentTimeMillis();
        long tDelta = tCur - t;
        t = tCur;
        return tDelta / 1000.0;
    }

    double getFps() {
        long tCur = System.currentTimeMillis();
        long tDelta = tCur - t;
        t = tCur;
        return 1000.0 / tDelta;
    }
}
