package com.remmel.recorder3d.recorder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


/**
 * Bug in lib, see below
 * build.gradle/android: testOptions {
 *         unitTests {
 *             includeAndroidResources = true
 *         }
 *     }
 */
//@RunWith(RobolectricTestRunner.class) //to uncomment and add `testImplementation 'org.robolectric:robolectric:4.4'` when bug fixed, see below
public class RobolectricTest {

    private static final String DIR = "src/test/resources";
    private static final String JPEG =  DIR + "/00000012_image.jpg";


    /**
     * That code doesn't success, as the Bitmap is full black
     * https://github.com/robolectric/robolectric/issues/3743
     */
    @Test
    public void readBitmap() {
        assertTrue("file exists", new File(JPEG).exists());
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(JPEG, options);
        assertEquals("width", 1440, bitmap.getWidth());

        int rgb = bitmap.getPixel(1440/2, 1080/2);
        assertNotEquals("color must no be perfect black", 0, rgb);

        //org.robolectric.shadows.ShadowBitmapFactory.decodeFile
    }
}
