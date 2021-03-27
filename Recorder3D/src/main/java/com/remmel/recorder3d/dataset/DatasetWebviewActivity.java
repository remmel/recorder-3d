package com.remmel.recorder3d.dataset;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.webkit.internal.AssetHelper;

import com.remmel.recorder3d.R;
import com.remmel.recorder3d.recorder.preferences.AppSharedPreference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class DatasetWebviewActivity extends Activity {
    private static final String TAG = DatasetWebviewActivity.class.getSimpleName();
    static final String FILES_DOMAIN = "local.net"; //could use WebViewAssetLoader.DEFAULT_DOMAIN
    static final int TYPE_POSEVIEWER = 0;
    static final int TYPE_VIDEO3D = 1;
    public static final String BUNDLE_KEY_DATASET = "dataset"; //store the directoy name eg "2020-12-31_121200"
    static final String BUNDLE_KEY_TYPE = "type";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_dataset_webview);

        Bundle b = getIntent().getExtras();
        String dataset = b.getString(BUNDLE_KEY_DATASET);
        int type = b.getInt(BUNDLE_KEY_TYPE);

        TextView tv = findViewById(R.id.datasetwebview_txt);
        tv.setText(dataset);

        WebView wv = findViewById(R.id.webview);
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowContentAccess(true);

        //WebViewAssetLoader seems to be only for assets files, in different directory
        wv.setWebViewClient(getWebViewClient(this.getExternalFilesDir(null)));

        AppSharedPreference pref = new AppSharedPreference(this);
        String domain = pref.getWebviewUrl();

        String webappPage;
        switch (type) {
            case TYPE_POSEVIEWER:
                webappPage = "pose-viewer.html";
                break;

            case TYPE_VIDEO3D:
                webappPage = "video3d-editor.html";
                break;

            default:
                throw new RuntimeException("Unknow type: " + type);
        }

        String url = domain + "/" + webappPage + "?datasetType=RECORDER3D&datasetFolder=https://" + FILES_DOMAIN + "/" + dataset;
        wv.loadUrl(url);
    }

    protected static WebViewClient getWebViewClient(File dir) {
        return new WebViewClient() {
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                              WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (request.getUrl().getAuthority().equals(FILES_DOMAIN)) {
                    String suffixPath = uri.getPath();
                    File f = new File(dir, suffixPath);
                    if (f.exists()) {
                        @SuppressLint("RestrictedApi")
                        String mimeType = AssetHelper.guessMimeType(suffixPath);
                        InputStream is = null;
                        try {
                            is = new FileInputStream(f);
                        } catch (FileNotFoundException e) {
                            Log.e(TAG, "Cannot open InputStream: " + e.getMessage());
                        }
                        WebResourceResponse res = new WebResourceResponse(mimeType, "", is);
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Access-Control-Allow-Origin", "*");
                        res.setResponseHeaders(headers);
                        return res;
                    }
                }

                return null;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); //in https://localhost don't have proper ssl certificate
            }
        };
    }
}
