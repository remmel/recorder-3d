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
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARSession;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MeasureRenderManager implements GLSurfaceView.Renderer {

    private static final String TAG = MeasureRenderManager.class.getSimpleName();

    private static final int PROJ_MATRIX_OFFSET = 0;

    private static final float PROJ_MATRIX_NEAR = 0.1f;

    private static final float PROJ_MATRIX_FAR = 100.0f;

    private static final float MATRIX_SCALE_SX = -1.0f;

    private static final float MATRIX_SCALE_SY = -1.0f;

    private TextureDisplay mTextureDisplay = new TextureDisplay();

    private ARSession mSession;

    private Activity mActivity;

    private Context mContext;

    TextView depthTextView;

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
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            return;
        }

        try {
            mSession.setCameraTextureName(mTextureDisplay.getExternalTextureId());
            ARFrame arFrame = mSession.update();
            ARCamera arCamera = arFrame.getCamera();

            // The size of the projection matrix is 4 * 4.
            float[] projectionMatrix = new float[16];

            arCamera.getProjectionMatrix(projectionMatrix, PROJ_MATRIX_OFFSET, PROJ_MATRIX_NEAR, PROJ_MATRIX_FAR);
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

        depthTextView.setText("Depth: "+depthRange + "mm Confidence:"+depthConfidence + " - "+(int)(depthPercentage*100) + "%");
    }
}
