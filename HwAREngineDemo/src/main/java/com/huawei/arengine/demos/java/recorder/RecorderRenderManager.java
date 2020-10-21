package com.huawei.arengine.demos.java.recorder;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARSession;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class RecorderRenderManager implements GLSurfaceView.Renderer {

    private static final String TAG = RecorderRenderManager.class.getSimpleName();

    private static final int PROJ_MATRIX_OFFSET = 0;

    private static final float PROJ_MATRIX_NEAR = 0.1f;

    private static final float PROJ_MATRIX_FAR = 100.0f;

    private static final float MATRIX_SCALE_SX = -1.0f;

    private static final float MATRIX_SCALE_SY = -1.0f;

    private TextureDisplay mTextureDisplay = new TextureDisplay();

    private ARSession mSession;

    private Activity mActivity;

    private Context mContext;

    private DisplayRotationManager mDisplayRotationManager;

    long numFrame = 0;

    private File dir; //folder where the rgb & depth images will be saved

    public RecorderRenderManager(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;

        String datestr = (new SimpleDateFormat("yyyy-MM-dd_HHmmss")).format(new Date());
        dir = this.mContext.getExternalFilesDir(datestr);
        if (!dir.exists()) {
            dir.mkdirs();
        }
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
            ARCamera arCamera = arFrame.getCamera();

            // The size of the projection matrix is 4 * 4.
            float[] projectionMatrix = new float[16];

            arCamera.getProjectionMatrix(projectionMatrix, PROJ_MATRIX_OFFSET, PROJ_MATRIX_NEAR, PROJ_MATRIX_FAR);
            mTextureDisplay.onDrawFrame(arFrame);
            updateFrameRecording(arFrame);

        } catch (ArDemoRuntimeException e) {
            Log.e(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (Throwable t) {
            // This prevents the app from crashing due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread: ", t);
        }
    }

    private void updateFrameRecording(ARFrame arFrame) {
        numFrame++;
        if(numFrame % 12 != 0) return; //only save part of frames

        //arFrame.getCamera().getPose().

        String numFrameStr = String.format("%08d", numFrame);

//        ImageUtils.writeImageYuv(arFrame.acquireCameraImage(), new File(dir, numFrameStr+"_camera_image.jpg"));
        ImageUtils.writeImageYuv(arFrame.acquirePreviewImage(), new File(dir, numFrameStr+"_preview_image.jpg"));
        ImageUtils.writeImageDepth(arFrame.acquireDepthImage(), new File(dir, numFrameStr+"_depth.png"));
//        ImageUtils.writeObj(arFrame.acquireSceneMesh(), "scene_mesh_"+numFrame+".obj", dir);
//        ImageUtils.writeObj( arFrame.acquirePointCloud(), "scene_mesh_"+numFrame+".obj", dir);
    }
}
