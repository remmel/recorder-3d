package com.remmel.recorder3d;

import java.io.File;
import java.io.FilenameFilter;

public class FilenameFilterUtils {
    public static FilenameFilter isDir() {
        return new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        };
    }

    public static FilenameFilter endsWith(String endsWith) {
        return new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return name.endsWith(endsWith);
            }
        };
    }
}
