package com.huawei.arengine.demos.java.recorder.preferences;

import android.os.Bundle;
import android.util.Size;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.huawei.arengine.demos.R;

import java.util.List;

public class RecorderPreferenceFragment extends PreferenceFragmentCompat {
    protected List<Size> resolutions;

    RecorderPreferenceFragment(List<Size> resolutions) {
        super();
        this.resolutions = resolutions;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        //Set Resolutions
        ListPreference lp = findPreference(getString(R.string.pref_rbgPreview_resolution));

        CharSequence entries[] = new String[resolutions.size()];
        CharSequence entryValues[] = new String[resolutions.size()];
        int i = 0;
        for (Size s : resolutions) {
            String label = s.getWidth() + "x" + s.getHeight();
            entries[i] = label;
            entryValues[i] = label;
            i++;
        }

        lp.setEntries(entries);
        lp.setEntryValues(entryValues);
    }
}