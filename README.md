# Urbo #
## About ##
The Urbo SDK is the world's first real-time landmark visual recognition engine. We believe that mobile apps and devices should have true context about their users and intentions, specifically in urban settings. We developed Urbo bcause we believe machines, just like humans, should utilize vision as their primary sense. 

With the Urbo SDK, any app developer can now integrate this ground breaking technology into any app! 

Checkout this demo video:  
  https://www.youtube.com/watch?v=sDj7NlgktGM

## Core Capabilities ##
* **Real time recognition of places using the camera**  
  Imagine pointing the device at a place and asking “What’s that?”
* **Real time tagging of new places, and updating the visual index of existing places**  
  Imagine taking a single picture of your house/shop and then having other users recognize it by simply using the app. 
* **Live map of places around you**
* **Private cloud-based database management system**  
  Allows you to manage the places added by your users and even import places from other databases.

## Usage ##
* The Urbo SDK is implemented as a simple Android/iOS library.
* The app layer of the SDK is entirely open source! This means you can easily adapt it to your needs.
* The library contains a custom camera view that can be integrated into any app to recognize places with ease.
* You can choose to participate in the public Urbo cloud or opt for a private cloud environment, where you can: manage all the places added by your users, add places manually or import from other databases.

## Quickstart (Android) ##
Either download the source and reference the `Urbo` module from your project or download a pre-packaged binary and include it as a Gradle dependency.

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

## Quickstart (iOS) ##

### Requirements ###
**Include following Frameworks:**

* `CoreMotion.framework` 
* `CoreLocation.framework`
* `AVFoundation.framework`
* `opencv2.framework v3.0`

Urbo requires `AFNetworking` instalation.

Also if you are using cocoapods you should add `AssetsLibrary.framework` and `libjpeg.a`. 

Now, download the source, copy `Urbo` to your project folder and add it to your project.

Add following in your class:
```
#import "UrboCameraView.h"

#import "Urbo.h"
```
and add delegate `UrboDelegate`: 

```
@interface ViewController : UIViewController <UrboDelegate>
```

Create display view with custom class `UrboCameraView.h` or if you are using storyboard, change the default class and create the outlet.

Sample initialization:
```
- (void)viewDidLoad 
{
 [super viewDidLoad];
 [Urbo sharedInstance].delegate = self;
}

-(void) viewDidAppear:(BOOL)animated
{
 [super viewDidAppear:animated];
 [self.urboCameraView unFreeze];
}
```

If you want to use front cammera just use `setFrontCamera:(BOOL)` method of `UrboCameraView` class before `unFreeze`, like this:

```
-(void) viewDidAppear:(BOOL)animated
{
 [super viewDidAppear:animated];
 [self.urboCameraView setFrontCamera:YES];
 [self.urboCameraView unFreeze];
}
```

And at last you should implement `UrboDelegate` method:

```
-(void)urbo:(Urbo *)urbo didChangeState:(int)state withPoi:(POI *)poi
{

}
```

And you're good to go!

Be sure to check the sample app included in the source for advanced usage.


## Support / Bugs ##
If you find a bug please report it here. We try to fix everything!

## Contributions ##
Contributions are very welcome. At this stage please contact us directly for guidelines at [urbo@fringefy.com](mailto:urbo@fringefy.com) before starting to work on your contribution. Thanks!
