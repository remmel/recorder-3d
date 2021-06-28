package com.remmel.recorder3d.recorder;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARCameraIntrinsics;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARSession;
import com.remmel.recorder3d.R;
import com.remmel.recorder3d.recorder.preferences.AppSharedPreference;
import com.remmel.recorder3d.recorder.video.RecordVideoAbstract;
import com.remmel.recorder3d.recorder.video.RecordVideoImp;

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

    public static final String FN_SUFFIX_DEPTH16BIN = "_depth16.bin";
    public static final String FN_SUFFIX_IMAGEVGAJPG = "_image_vga.jpg";
    public static final String FN_SUFFIX_IMAGEJPG = "_image.jpg";
    public static final String FN_VIDEO = "video.mp4";

    private final RecordVideoAbstract recordVideoAbstract;

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

    ImageButton btnTrigger;
    boolean isRecording = false;
    MediaPlayer _shootMP;

    TextView tvModePhoto;
    TextView tvModeRepeat;
    TextView tvModeVideo;
    Mode mode = Mode.PHOTO;

    public enum Mode {
        PHOTO, //last only 1 frame
        REPEAT, //is repeated every x frame
        VIDEO, //all the frame
    };

    AppSharedPreference pref;

    private static final float RAD2DEG = (float) (180 / Math.PI);

    protected FpsMeter fpsMeter = new FpsMeter();

    protected Long startTimeMs; //I want to store it here, as CsvPose and Video must be synced

    public RecorderRenderManager(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;
        pref = new AppSharedPreference(context);

//        fPose = new File(dir, "poses.csv");
        poseTextView = activity.findViewById(R.id.recorderPoseTextView);

        btnTrigger = activity.findViewById(R.id.btn_recorder_trigger);
        btnTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               onClickTrigger();
            }
        });

        tvModePhoto = activity.findViewById(R.id.btn_recorder_mode_photo);
        tvModeRepeat = activity.findViewById(R.id.btn_recorder_mode_repeat);
        tvModeVideo = activity.findViewById(R.id.btn_recorder_mode_video);

        tvModePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = Mode.PHOTO;
                renderOnClickMode();
            }
        });

        tvModeRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = Mode.REPEAT;
                renderOnClickMode();
            }
        });

        tvModeVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recordVideoAbstract.requestPermission())
                    mode = Mode.VIDEO;
                renderOnClickMode();
            }
        });

        AppSharedPreference pref = new AppSharedPreference(context);

        renderOnClickMode();
        renderTriggerButtonIcon();

        recordVideoAbstract = new RecordVideoImp(mActivity, pref.getRgbPreviewResolution());
    }

    protected void onClickTrigger() {
        initDir();
        switch (this.mode) {
            case PHOTO:
                isRecording = true;
                shootSound();
                break;
            case REPEAT:
                isRecording = !isRecording;
                break;
            case VIDEO:
                isRecording = !isRecording;
                if(isRecording) {
                    startTimeMs = System.currentTimeMillis();
                    recordVideoAbstract.start(dir);
                } else
                    recordVideoAbstract.stop();
                break;
        }

        if(isRecording && startTimeMs == null) //set only once after recording starts
                startTimeMs = System.currentTimeMillis(); //I do that here instead in the update, as it must be synced with the audio recording

        renderTriggerButtonIcon();
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

        ARPose arPose = arFrame.getCamera().getPose();

        String numFrameStr = String.format("%08d", numFrame);

        // FIXME what returns arFrame.getTimestampNs? eg: 33302714788000 (=33302714ms when System.currentTimeMillis() = 1622076039377)
//        Log.d(TAG, "arFrame.getTimestampNs="+arFrame.getTimestampNs() + " ms=" + arFrame.getTimestampNs()/1000000 + " currtime="+System.currentTimeMillis()+" diff="+(arFrame.getTimestampNs()/1000000-System.currentTimeMillis()));

        Long currentSessionTimeMs = startTimeMs != null ? System.currentTimeMillis() - startTimeMs : null;

        CsvPose csvPose = new CsvPose(numFrameStr, arPose, currentSessionTimeMs);
        renderPoseDebugInfo(csvPose);

        if (arPose.tx() == 0 || arPose.ty() == 0 || arPose.tz() == 0) return; //pose not ready yet

        boolean repeatDepth = mode == Mode.REPEAT && pref.isDepthEnabled() && numFrame % pref.getDepthRepeat() == 0;
        boolean repeatRgbVga = mode == Mode.REPEAT && pref.isRgbVgaEnabled() && numFrame % pref.getRgbVgaRepeat() == 0;
        boolean repeatRgbPreview = mode == Mode.REPEAT && pref.isRgbPreviewEnabled() && numFrame % pref.getRgbPreviewRepeat() == 0;

        if (isRecording && (
                mode == Mode.PHOTO
                        || mode == Mode.REPEAT && (repeatDepth || repeatRgbVga || repeatRgbPreview)
                        || mode == Mode.VIDEO)) {

            csvPoses.add(csvPose);
            numFrameSaved++;

            if (mode == Mode.PHOTO || repeatDepth || mode == Mode.VIDEO)
                ImageUtils.writeImageDepth16(arFrame.acquireDepthImage(), new File(dir, numFrameStr + FN_SUFFIX_DEPTH16BIN)); // 0.001s

            if (mode == Mode.PHOTO || repeatRgbVga)
                ImageUtils.writeImageYuvJpg(arFrame.acquireCameraImage(), new File(dir, numFrameStr + FN_SUFFIX_IMAGEVGAJPG));

            if (mode == Mode.PHOTO || repeatRgbPreview)
                ImageUtils.writeImageYuvJpg(arFrame.acquirePreviewImage(), new File(dir, numFrameStr + FN_SUFFIX_IMAGEJPG));

            if(mode == Mode.VIDEO)
                recordVideoAbstract.update(arFrame, currentSessionTimeMs);


            //        ImageUtils.writeImageN21Bin(arFrame.acquirePreviewImage(), new File(dir, numFrameStr+"_image.bin")); //0.007s
            //        ImageUtils.writeImageDepthNicePng(arFrame.acquireDepthImage(), new File(dir, numFrameStr+"_depth.png")); // 0.08s
            //        ImageUtils.writePly(arFrame.acquireDepthImage(), arFrame.acquirePreviewImage(), new File(dir, numFrameStr+".ply")); //0.15s

            if(mode == Mode.PHOTO) {
                        ImageUtils.writeObj(arFrame.acquireSceneMesh(), new File(dir, "scene_mesh_"+numFrame+".obj"));
            }
            //        ImageUtils.writePly(arFrame.acquirePointCloud(), new File(dir, "point_cloud_"+numFrame+".ply"));

            if(mode == Mode.PHOTO) {
                isRecording = false;
                renderTriggerButtonIcon(); //put back normal icon as photo is taken
            }
        }


    }

    private void renderPoseDebugInfo(CsvPose csvPose) {
        float fps = fpsMeter.doFpsCalculate();
//        String debugInstrinsics = debugCameraInstrinsics(arFrame.getCamera());
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                poseTextView.setText("FPS: " + fps
                        + "\n" + csvPose.toString() + " " + numFrame
                        + "\nFrame saved:" + numFrameSaved
                        + "\nTime:"+ (csvPose.timems ==null ? " -" : csvPose.timems/1000)); //"\n" + debugInstrinsics
            }
        });
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


    protected void renderOnClickMode() {
        tvModePhoto.setPaintFlags(Paint.ANTI_ALIAS_FLAG);
        tvModeRepeat.setPaintFlags(Paint.ANTI_ALIAS_FLAG);
        tvModeVideo.setPaintFlags(Paint.ANTI_ALIAS_FLAG);

        switch (this.mode) {
            case PHOTO:
                tvModePhoto.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
                break;
            case REPEAT:
                tvModeRepeat.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
                break;
            case VIDEO:
                tvModeVideo.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
                break;
        }

        renderTriggerButtonIcon();
    }

    protected void renderTriggerButtonIcon() {
        switch (mode) {
            case PHOTO:
                if(isRecording) btnTrigger.setImageResource(R.drawable.ic_btn_camera_videorecording);
                else  btnTrigger.setImageResource(R.drawable.ic_btn_camera_photo);
                break;
            case REPEAT:
            case VIDEO:
                if(isRecording) btnTrigger.setImageResource(R.drawable.ic_btn_camera_videorecording);
                else btnTrigger.setImageResource(R.drawable.ic_btn_camera_video);
                break;
        }
    }

}
