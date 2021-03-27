package com.remmel.recorder3d.dataset;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.remmel.recorder3d.FilenameFilterUtils;
import com.remmel.recorder3d.R;
import com.remmel.recorder3d.recorder.ImageUtils;
import com.remmel.recorder3d.recorder.PlyUtils;
import com.remmel.recorder3d.recorder.RecorderRenderManager;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;


public class DatasetActivity extends Activity {
    private static final String TAG = DatasetActivity.class.getSimpleName();

    File datasetFolder;
    ProgressBar bar;
    TextView tv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_dataset);

        Bundle b = getIntent().getExtras();
        String dataset = b.getString(DatasetWebviewActivity.BUNDLE_KEY_DATASET);

        datasetFolder = this.getExternalFilesDir(dataset);
        if (!datasetFolder.exists())
            throw new RuntimeException("Folder " + datasetFolder.getAbsolutePath() + " doesn't exist");

        tv = findViewById(R.id.dataset_txt);
        tv.setText(dataset);

        bar = findViewById(R.id.progBar);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dataset_btnply:
                writePly();
                break;
            case R.id.dataset_btnpng:
                writePng();
                break;
            case R.id.dataset_btnwebviewerposes:
                startActivityWebViewer(DatasetWebviewActivity.TYPE_POSEVIEWER);
                break;
            case R.id.dataset_btnwebviewervideo:
                startActivityWebViewer(DatasetWebviewActivity.TYPE_VIDEO3D);
                break;
            default:
                Log.e(TAG, "onClick error!");
        }
    }

    protected void startActivityWebViewer(int type){
        Intent intent = new Intent(this, DatasetWebviewActivity.class);
        Bundle b = new Bundle();
        b.putString("dataset", datasetFolder.getName()); //Your id
        b.putInt("type", type);
        intent.putExtras(b);
        startActivity(intent);
    }

    protected void writePly() {
        String[] fns = datasetFolder.list(FilenameFilterUtils.endsWith(RecorderRenderManager.FN_SUFFIX_DEPTH16BIN));
        Arrays.sort(fns);
        bar.setProgress(0);
        bar.setMax(fns.length);
        new Thread(new Runnable() {
            public void run() {
                for(int i = 0; i< fns.length; i++) {
                    String fn = fns[i];
                    tv.setText((i+1)+"/"+fns.length + " : Convert " + fn + " in .ply");
                    doWork(datasetFolder, fn);
                    bar.setProgress(i+1);
                }
            }
            private void doWork(File dir, String fn) {//TODO handle if no png
                String numFrame = fn.substring(0, 8);
                String depth16 = dir + "/" + fn;
                String jpg = dir + "/" + numFrame + RecorderRenderManager.FN_SUFFIX_IMAGEJPG; //TODO handle if not exists
                String ply = dir + "/" + numFrame + ".ply";
                try {
                    PlyUtils.writePly(depth16, jpg, ply, false);
                    Log.d(TAG, "convert to ply "+numFrame+" in "+dir);
                } catch (IOException e) {
                    e.printStackTrace();
                    tv.setText(e.getMessage());
                }
            }

        }).start();
    }

    protected void writePng() {
        org.bytedeco.javacpp.Loader.load(org.bytedeco.javacpp.opencv_java.class);
        String[] fns = datasetFolder.list(FilenameFilterUtils.endsWith(RecorderRenderManager.FN_SUFFIX_DEPTH16BIN));
        Arrays.sort(fns);
        bar.setProgress(0);
        bar.setMax(fns.length);
        new Thread(new Runnable() {
            public void run() {
                for(int i = 0; i< fns.length; i++) {
                    String fn = fns[i];
                    tv.setText((i+1)+"/"+fns.length + " : Convert " + fn + " in .png");
                    doWork(datasetFolder, fn);
                    bar.setProgress(i+1);
                }
            }
            private void doWork(File dir, String fn) {//TODO handle if no png
                String numFrame = fn.substring(0, 8);
                String depth16 = dir + "/" + fn;
                try {
                    ImageUtils.writeDepth16binInPng16GrayscaleTum(depth16, 240, 180, depth16+".png");
                    Log.d(TAG, "convert to PNG "+numFrame+" in "+dir);
                } catch (IOException e) {
                    e.printStackTrace();
                    tv.setText(e.getMessage());
                }

            }

        }).start();
    }
}
