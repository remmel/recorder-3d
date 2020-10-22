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
        Image image = arFrame.acquireDepthImage();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);

        ShortBuffer shortDepthBuffer = buffer.asShortBuffer();

        int w = image.getWidth(); //240
        int h = image.getHeight(); //180

        int depthSample = shortDepthBuffer.get((w*h/2)+w/2);
        short depthRange = (short) (depthSample & 0x1FFF); //https://developer.android.com/reference/android/graphics/ImageFormat#DEPTH16
        short depthConfidence = (short) ((depthSample >> 13) & 0x7);
        float depthPercentage = depthConfidence == 0 ? 1.f : (depthConfidence - 1) / 7.f;

        depthTextView.setText("Depth: "+depthRange + "mm Confidence: " + (int)(depthPercentage*100) + "% _" + depthConfidence);
    }
}
