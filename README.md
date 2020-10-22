# HUAWEI AR Engine Demo

This if a fork of [hms-AREngine-demo](https://github.com/HMS-Core/hms-AREngine-demo)
I added 2 features :
- Measure : display the distance (depth) of the object to the camera (center depth pixel)
- Recorder : save camera image and depth of x seconds

## Measure

![Screenshot_resized25](doc/Screenshot_resized25.jpg)

TODO:
- display something to understand where is measured the depth (like coloring the central pixel)

## Recorder

![](doc/00000012_preview_image_resized.jpg) ![](doc/00000012_depth.png)

Features :
- Save depth (240x180) - (fancy png depth, binary depth16)
- Save rgb images (1440x1080)
- Save poses in CSV

TODO:
- save rgbd ply
- understand why it's not possible to save image_preview in a better resolution

