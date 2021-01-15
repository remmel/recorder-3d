package com.huawei.arengine.demos.java.measure;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARSession;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MeasureRenderManager implements GLSurfaceView.Renderer {

    private static final String TAG = MeasureRenderManager.class.getSimpleName();

    private TextureDisplay mTextureDisplay = new TextureDisplay();

    private ARSession mSession;

    private Activity mActivity;

    private Context mContext;

    TextView depthTextView;

    int frame = 0;

    private DisplayRotationManager mDisplayRotationManager;

    public MeasureRenderManager(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;
        depthTextView = activity.findViewById(R.id.measureDepthTextView);
    }

    /**
     * Set ARSession, which will update and obtain the latest data in OnDrawFrame.
     *
     * @param arSession ARSession.
     */
    public void setArSession(ARSession arSession) {
        if (arSession == null) {
            Log.e(TAG, "setSession error, arSession is null!");
            return;
        }
        mSession = arSession;
    }

    /**
     * Set the DisplayRotationManage object, which will be used in onSurfaceChanged and onDrawFrame.
     *
     * @param displayRotationManager DisplayRotationManage is a customized object.
     */
    public void setDisplayRotationManage(DisplayRotationManager displayRotationManager) {
        if (displayRotationManager == null) {
            Log.e(TAG, "SetDisplayRotationManage error, displayRotationManage is null!");
            return;
        }
        mDisplayRotationManager = displayRotationManager;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        mTextureDisplay.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mTextureDisplay.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
        mDisplayRotationManager.updateViewportRotation(width, height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            return;
        }
        if (mDisplayRotationManager.getDeviceRotation()) {
            mDisplayRotationManager.updateArSessionDisplayGeometry(mSession);
        }

        try {
            mSession.setCameraTextureName(mTextureDisplay.getExternalTextureId());
            ARFrame arFrame = mSession.update();
            mTextureDisplay.onDrawFrame(arFrame);
            updateDepthInfo(arFrame);

        } catch (ArDemoRuntimeException e) {
            Log.e(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (Throwable t) {
            // This prevents the app from crashing due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread: ", t);
        }
    }

    private void updateDepthInfo(ARFrame arFrame) {
        //update every 5 frames
        if(frame>5) frame = 0;
        else {frame++; return; }

        Image image = arFrame.acquireDepthImage();

        DepthInfo diCenter = getDepthInfo(image, 1/2f, 1/2f);
        DepthInfo diLeft = getDepthInfo(image, 1/2f, 1/8f);
        DepthInfo diRight = getDepthInfo(image, 1/2f, 7/8f);
        depthTextView.setText(
                "L:"+diLeft.toString() +"\n"
                + "C:" +diCenter.toString() +"\n"
                + "R:"+diRight.toString()
        );
    }

    protected DepthInfo getDepthInfo(Image image, float x, float y) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer shortDepthBuffer = buffer.asShortBuffer();
        int w = image.getWidth(); //240
        int h = image.getHeight(); //180
        int depthSample = shortDepthBuffer.get((int)(w*h*x+w*y));
        return new DepthInfo(depthSample);
    }

    public class DepthInfo {
        public DepthInfo(int depthSample) {
            range = (short) (depthSample & 0x1FFF); //https://developer.android.com/reference/android/graphics/ImageFormat#DEPTH16
            confidence = (short) ((depthSample >> 13) & 0x7);
            confidenceRatio = confidence == 0 ? 1.f : (confidence - 1) / 7.f;
        }

        short range;
        short confidence;
        float confidenceRatio;

        public int getPercentage() {
            return (int)(confidenceRatio *100);
        }

        @Override
        public String toString() {
            return "Depth: "+this.range + "mm Confidence: " + this.getPercentage() + "%  (" + this.confidence+")";
        }
    }
}
