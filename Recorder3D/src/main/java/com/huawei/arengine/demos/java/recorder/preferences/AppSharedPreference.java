package com.huawei.arengine.demos.java.recorder.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Size;

import androidx.annotation.StringRes;

import com.huawei.arengine.demos.R;
import com.huawei.hiar.ARConfigBase;

public class AppSharedPreference {
    SharedPreferences sharedPrefs;
    Context context;
    String defaultRepeat;

    public AppSharedPreference(Context context) {
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.context = context;
        this.defaultRepeat = getString(R.string.pref_repeat_default_value);
    }

    protected String getString(@StringRes int resId) {
        return context.getString(resId);
    }

    public ARConfigBase.FocusMode getFocusMode() {
        boolean autofocus = sharedPrefs.getBoolean(getString(R.string.pref_autofocus), false); //TODO default value is duplicated, avoid that
        return autofocus ? ARConfigBase.FocusMode.AUTO_FOCUS : ARConfigBase.FocusMode.FIXED_FOCUS;
    }

    //depth
    public boolean isDepthEnabled() {
        return sharedPrefs.getBoolean(getString(R.string.pref_depth), true); //TODO duplicated defValue
    }

    public long getDepthRepeat() {
        return getPrefInteger(R.string.pref_depth_repeat, defaultRepeat);
    }

    //vga
    public boolean isRgbVgaEnabled() {
        return sharedPrefs.getBoolean(getString(R.string.pref_rgbVga), true); //TODO duplicated defValue
    }

    public long getRgbVgaRepeat() {
        return getPrefInteger(R.string.pref_rgbvga_repeat, defaultRepeat);
    }

    //preview
    public boolean isRgbPreviewEnabled() {
        return sharedPrefs.getBoolean(getString(R.string.pref_rgbPreview), true); //TODO duplicated defValue
    }

    public long getRgbPreviewRepeat() {
        return getPrefInteger(R.string.pref_rgbPreview_repeat, defaultRepeat);
    }

    public Size getRgbPreviewResolution() {
        String str = sharedPrefs.getString(getString(R.string.pref_rbgPreview_resolution), getString(R.string.pref_rbgPreview_resolution_default_value));
        String[] strArray = str.split("x");
        return new Size(Integer.parseInt(strArray[0]), Integer.parseInt(strArray[1])); //TODO check if everything is OK?
    }

    protected int getPrefInteger(@StringRes int resId, String defaultValue) {
        return Integer.parseInt(sharedPrefs.getString(getString(resId), defaultValue));
    }
}
