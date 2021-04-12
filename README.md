# 3D Recorder

3D Recorder allows you to save the RGB and Depth images along their world poses (rotation and position) from your Huawei phone (with a Tof camera). [💾 Download last Android APK](https://github.com/remmel/recorder-3d/releases).

Thoses image can be visualized on the mobile itself or on my [online viewer](https://remmel.github.com/image-processing-js/pose-viewer.html).

You can choose how often to save the images and the rgb resolution (up to 3968x2976). Thus if you choose a small resolution (eg 1440x1080) you can create a [3d video](https://remy-mellet.com/image-processing-js/rgbds-viewer.html).  
If you choose a too big resolution the fps will drop and thus a few images per second will be saved.

<img src="doc/home.jpg" height="480" /> <img src="doc/capture.jpg" height="480" /> <img src="doc/dataset-home.jpg" height="480" /> <img src="doc/poses-viewer.jpg" height="480" /> <img src="doc/3d-editor.jpg" height="480" /> <img src="doc/preferences.jpg" height="480" />

Others :

<img src="Recorder3D/src/test/resources/00000012_image.jpg" width="240" /> ![](Recorder3D/src/test/resources/00000012_depth.png) <img src="doc/plymeshlab.png" width="240" />

## Features
- Save depth (240x180, binary DEPTH16 and 16bits grayscale PNG "OpenCV CV_16UC1")
- Save 2 rgb images (1 VGA + 1 with choosen resolution max 3264x2448)
- Save poses in CSV [download sample](doc/poses.csv)
- Export RGBD into PLY files [download sample](Recorder3D/src/test/resources/00000012.ply)
- Poses viewer and video editor (webview)

## Bonus feature : Measure

Measure the distance of the center (depth) of the object to the camera (center depth pixel)  
<img src="doc/measure.jpg" height="480" />

## Android smarphone

That app has only been tested on Honor View 20 which has a tof sensor. This is the cheapest Huawei phone with Tof, you can find 2nd hand around 180€.
