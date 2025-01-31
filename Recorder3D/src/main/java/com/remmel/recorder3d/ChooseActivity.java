package com.remmel.recorder3d;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.huawei.arengine.demos.common.PermissionManager;
import com.remmel.recorder3d.dataset.DatasetActivity;
import com.remmel.recorder3d.dataset.DatasetWebviewActivity;
import com.remmel.recorder3d.measure.MeasureActivity;
import com.remmel.recorder3d.recorder.IoUtils;
import com.remmel.recorder3d.recorder.RecorderActivity;
import com.remmel.recorder3d.recorder.RecorderRenderManager;
import com.remmel.recorder3d.recorder.preferences.RecorderPreferenceActivity;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;

/**
 * This class provides the permission verification and sub-AR example redirection functions.
 *
 * @author HW
 * @since 2020-03-31
 */
public class ChooseActivity extends Activity {
    private static final String TAG = ChooseActivity.class.getSimpleName();
    protected LinearLayout llFileManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_choose);
        // AR Engine requires the camera permission.
        PermissionManager.checkPermission(this);

        TextView tvHeader = findViewById(R.id.choose_txt_header);
        tvHeader.setText(getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME);

        llFileManager = findViewById(R.id.choose_linearlayout);
    }

    @Override
    protected void onResume() {
        renderDirtyFileManager();
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!PermissionManager.hasPermission(this)) {
            Toast.makeText(this, "This application needs camera permission.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy start.");
        super.onDestroy();
        Log.i(TAG, "onDestroy end.");
    }

    /**
     * Choose activity.
     *
     * @param view View
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_measure:
                startActivity(new Intent(this, MeasureActivity.class));
                break;
            case R.id.btn_recorder:
                startActivity(new Intent(this, RecorderActivity.class));
                break;
            case R.id.btn_recorder_settings:
                startActivity(new Intent(this, RecorderPreferenceActivity.class));
                break;

            case R.id.btn_fileexplorer:
                File dir = this.getExternalFilesDir(null);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse(dir.getAbsolutePath());
                intent.setDataAndType(uri, "*/*");
                startActivity(Intent.createChooser(intent, "Open folder"));

                break;
            default:
                Log.e(TAG, "onClick error!");
        }
    }

    protected void renderDirtyFileManager() {
        File filesDir = this.getExternalFilesDir(null);
        llFileManager.removeAllViewsInLayout();

        TextView tvFolder = new TextView(this);
        tvFolder.setText(filesDir + ":");
        tvFolder.setTextSize(10);
        llFileManager.addView(tvFolder);

        String[] directories = filesDir.list(FilenameFilterUtils.isDir());
        Arrays.sort(directories);
        ArrayUtils.reverse(directories);

        for (String dir : directories) {
            Button bt = new Button(this);

            File f = new File(filesDir, dir);
            int nbRgbVga = f.list(FilenameFilterUtils.endsWith(RecorderRenderManager.FN_SUFFIX_IMAGEVGAJPG)).length;
            int nbRgb = f.list(FilenameFilterUtils.endsWith(RecorderRenderManager.FN_SUFFIX_IMAGEJPG)).length;
            int nbDepth = f.list(FilenameFilterUtils.endsWith(RecorderRenderManager.FN_SUFFIX_DEPTH16BIN)).length;

            bt.setText(dir+ " RGBVGA("+nbRgbVga+") RGB("+nbRgb+") Depth("+nbDepth+")");
            bt.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);

            ChooseActivity self = this; //hum... alternative?
            bt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(self, DatasetActivity.class);
                    Bundle b = new Bundle();
                    b.putString(DatasetWebviewActivity.BUNDLE_KEY_DATASET, dir); //Your id
                    intent.putExtras(b);
                    startActivity(intent);
                }
            });

            llFileManager.addView(bt);
        }
    }

    protected static String getLastestVersion() {
        //https://api.github.com/repos/remmel/recorder-3d/releases/latest
        //https://raw.githubusercontent.com/remmel/recorder-3d/master/build.gradle
        String lastestUrl = "https://api.github.com/repos/remmel/recorder-3d/releases/latest";
        try {
            String out = new Scanner(new URL(lastestUrl).openStream(), "UTF-8").useDelimiter("\\A").next();
            String ver = StringUtils.substringBetween(out, "tag_name\":\"v", "\",");
            return ver;
        } catch (IOException e) {
           return null;
        }
    }
}