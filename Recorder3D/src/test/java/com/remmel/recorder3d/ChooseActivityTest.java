package com.remmel.recorder3d;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChooseActivityTest {

    @Test
    public void testLastVersion() { //TODO move that
        String version = ChooseActivity.getLastestVersion();
        assertNotNull(version);
    }
}