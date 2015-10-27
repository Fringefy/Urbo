# Urbo #
## About ##
The Urbo SDK is the first real-time landmark visual recognition engine in the world. We believe that mobile apps, and devices in general, need to have true context about their users and their intentions, specifically in the urban environment. Since the primary human sense is visual, if accurate context and intention is required, we believe it is an input which cannot be ignored. That’s why we’ve developed Urbo. 

With the Urbo SDK, any app developer can now integrate this ground breaking technology straight into any app! 

Checkout this video to get a feeling of it:  
  https://www.youtube.com/watch?v=sDj7NlgktGM

## Core Capabilities ##
* **Real time recognition of places using the camera**  
  Imagine pointing the device at a place and asking “What’s that?”
* **Real time tagging of new places, and updating the visual index of existing places**  
  Imagine adding your house/shop for recognition by other users just by taking a single picture of it.
* **Live map of places around you**
* **Private cloud-based database management system**  
  Allows you to manage the places added by your users, or even import places from your existing database / listing straight into the Urbo cloud.

## Usage ##
* The Urbo SDK is implemented as an easily usable Android / iOS library.
* The app layer of the SDK is entirely open source! This means you can easily adapt it to your needs if required.
* The library contains a custom camera view that can be integrated in any app in order to recognize places with ease.
* You can choose to participate in the public Urbo cloud or opt for a private cloud environment where you can manage all the places added by your users and/or add places manually or import from existing DBs.

## Quickstart (Android) ##
Either download the source and reference the `Urbo` module from your project. Or download a pre-packaged binary and include it as a Gradle dependency.

Sample initialization (within activity's `onCreate`):
```
urbo = Urbo.getInstance(this)
		.setListener(this)
		.setDisplayView((ImageView) findViewById(R.id.tag_image));
```
You can add the `CameraView` in XML:
```
<com.fringefy.urbo.CameraView
        android:id="@+id/camera_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
```
And you're good to go!

Be sure to check the sample app included in the source for advanced usage.

## Support / Bugs ##
If you find a bug please report it here. We try to fix everything!

## Contributions ##
Contributions are very welcome. At this stage please contact us directly for guidelines at [urbo@fringefy.com](mailto:urbo@fringefy.com) before starting to work on your contribution. Thanks!