package com.remmel.recorder3d.recorder;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.remmel.recorder3d.R;
import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.remmel.recorder3d.recorder.preferences.AppSharedPreference;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARCameraIntrinsics;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class RecorderRenderManager implements GLSurfaceView.Renderer {

    private static final String TAG = RecorderRenderManager.class.getSimpleName();

    private static final int PROJ_MATRIX_OFFSET = 0;

    private static final float PROJ_MATRIX_NEAR = 0.1f;

    private static final float PROJ_MATRIX_FAR = 100.0f;

    private static final float MATRIX_SCALE_SX = -1.0f;

    private static final float MATRIX_SCALE_SY = -1.0f;
    public static final String FN_SUFFIX_DEPTH16BIN = "_depth16.bin";
    public static final String FN_SUFFIX_IMAGEVGAJPG = "_image_vga.jpg";
    public static final String FN_SUFFIX_IMAGEJPG = "_image.jpg";

    private TextureDisplay mTextureDisplay = new TextureDisplay();

    private ARSession mSession;

    private Activity mActivity;

    private Context mContext;

    private DisplayRotationManager mDisplayRotationManager;

    long numFrame = 0;
    long numFrameSaved = 0;

    private File dir; //folder where the rgb & depth images will be saved

    private List<CsvPose> csvPoses = new ArrayList<>();

    TextView poseTextView;

    Button btnPhoto;
    boolean takePhoto = false;
    MediaPlayer _shootMP;
    Switch btnVideo;

    AppSharedPreference pref;

    private static final float RAD2DEG = (float) (180 / Math.PI);

    protected FpsMeter fpsMeter = new FpsMeter();

    public RecorderRenderManager(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;
        pref = new AppSharedPreference(context);

//        fPose = new File(dir, "poses.csv");
        poseTextView = activity.findViewById(R.id.recorderPoseTextView);

        btnPhoto = activity.findViewById(R.id.btn_recorder_photo);
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        btnVideo = activity.findViewById(R.id.btn_recorder_switch);

        AppSharedPreference pref = new AppSharedPreference(context);
    }

    protected void takePhoto() {
        takePhoto = true;
        shootSound();
    }

    protected void initDir() {
        if(dir == null) {
            String datestr = (new SimpleDateFormat("yyyy-MM-dd_HHmmss")).format(new Date());
            dir = this.mContext.getExternalFilesDir(datestr);
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

        float fps = fpsMeter.doFpsCalculate();

        ARPose arPose = arFrame.getCamera().getPose();

        String debugInstrinsics = debugCameraInstrinsics(arFrame.getCamera());

        String numFrameStr = String.format("%08d", numFrame);

        CsvPose csvPose = new CsvPose(numFrameStr, arPose);
        poseTextView.setText("FPS: " + fps + "\n" + csvPose.toString() + " " + numFrame + "\n" + debugInstrinsics + "\nFrame saved:" + numFrameSaved);

        if (arPose.tx() == 0 || arPose.ty() == 0 || arPose.tz() == 0) return; //pose not ready yet

        boolean videoDepth = btnVideo.isChecked() && pref.isDepthEnabled() && numFrame % pref.getDepthRepeat() == 0;
        boolean videoRgbVga = btnVideo.isChecked() && pref.isRgbVgaEnabled() && numFrame % pref.getRgbVgaRepeat() == 0;
        boolean videoRgbPreview = btnVideo.isChecked() && pref.isRgbPreviewEnabled() && numFrame % pref.getRgbPreviewRepeat() == 0;

        if (takePhoto || videoDepth || videoRgbVga || videoRgbPreview) {

            initDir(); //the 1st time or getDir() maybe nicer
            csvPoses.add(csvPose);
            numFrameSaved++;

            if (takePhoto || videoDepth)
                ImageUtils.writeImageDepth16(arFrame.acquireDepthImage(), new File(dir, numFrameStr + FN_SUFFIX_DEPTH16BIN)); // 0.001s

            if (takePhoto || videoRgbVga)
                ImageUtils.writeImageYuvJpg(arFrame.acquireCameraImage(), new File(dir, numFrameStr + FN_SUFFIX_IMAGEVGAJPG));

            if (takePhoto || videoRgbPreview)
                ImageUtils.writeImageYuvJpg(arFrame.acquirePreviewImage(), new File(dir, numFrameStr + FN_SUFFIX_IMAGEJPG));


            //        ImageUtils.writeImageN21Bin(arFrame.acquirePreviewImage(), new File(dir, numFrameStr+"_image.bin")); //0.007s
            //        ImageUtils.writeImageDepthNicePng(arFrame.acquireDepthImage(), new File(dir, numFrameStr+"_depth.png")); // 0.08s
            //        ImageUtils.writePly(arFrame.acquireDepthImage(), arFrame.acquirePreviewImage(), new File(dir, numFrameStr+".ply")); //0.15s
            //        ImageUtils.writeObj(arFrame.acquireSceneMesh(), new File(dir, "scene_mesh_"+numFrame+".obj"));
            //        ImageUtils.writePly(arFrame.acquirePointCloud(), new File(dir, "point_cloud_"+numFrame+".ply"));

            takePhoto = false;
        }


    }

    private String debugCameraInstrinsics(ARCamera arCamera) {

        ARCameraIntrinsics arCameraIntrinsics = arCamera.getCameraImageIntrinsics();
//        float[] distortions = arCameraIntrinsics.getDistortions();
        float[] focalLength = arCameraIntrinsics.getFocalLength();
        float[] principalPoint = arCameraIntrinsics.getPrincipalPoint(); //9441
        int[] imageSize = arCameraIntrinsics.getImageDimensions(); //1080 1440
        float fovX = (float) (Math.atan2(imageSize[0] / 2, focalLength[0]) * RAD2DEG * 2);
        float fovY = (float) (Math.atan2(imageSize[1] / 2, focalLength[1]) * RAD2DEG * 2);

//        float[] projMatrix = new float[4*4];
//        arCamera.getProjectionMatrix(projMatrix, PROJ_MATRIX_OFFSET, PROJ_MATRIX_NEAR, PROJ_MATRIX_FAR);
//        float cx = imageSize[0] * (1 - projMatrix[2*4+0]) / 2; //= focalLength[0] = 1440*(1-0.001512431)/2 = 718.911
//        float cy = imageSize[1] * (1 - projMatrix[2*4+1]) / 2; //= focalLength[1] = 1080*(1-0.009666785)/2 = 534.780
//        float fx = imageSize[0] * projMatrix[0*4+0] / 2; //principalPoint[0] = 1440*1.4902/2=1072.944
//        float fy = imageSize[1] * projMatrix[1*4+1] / 2; //principalPoint[1] = 1080*3.0466316/2=1645.18 ?!? seems incorrect!

        return String.format("Focal Length: (%.2f, %.2f)"
                        + "\nPrincipal Point: (%.2f, %.2f)"
                        + "\nImage Dimensions: (%d, %d)"
                        + "\nField of View: (%.2f˚, %.2f˚)",
                focalLength[0], focalLength[1],
                principalPoint[0], principalPoint[1],
                imageSize[0], imageSize[1],
                fovX,
                fovY
        );
    }


    public void onPause() {
        writePoses();
    }

    protected void writePoses() {
        if (this.csvPoses.size() == 0) return; //nothing to save
        File f = new File(dir, "poses.csv");
        try {
            Files.write(f.toPath(), CsvPose.toCsvRows(this.csvPoses));
//            PlyUtils.bulkWritePly(this.fPose.getParentFile()); //takes too much time to be done here
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Cannot save poses: " + f);
        }
        Toast.makeText(mContext, "Saved in " + f, Toast.LENGTH_LONG).show();
    }

    public void shootSound() {
        AudioManager meng = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

        if (volume != 0) {
            if (_shootMP == null)
                _shootMP = MediaPlayer.create(mContext, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            if (_shootMP != null)
                _shootMP.start();
        }
    }
}
