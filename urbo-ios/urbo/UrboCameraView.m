#import "UrboCameraView.h"
#import "POI.h"
#import "PexesoBridge.h"
#import "Urbo.h"

@implementation UrboCameraView
{
    AVCaptureSession *captureSession;
    BOOL useFrontCamera;
}

-(void)layoutSubviews
{
    [self initLiveFeed];
}

-(void) setFrontCamera:(BOOL) useFront
{
    useFrontCamera = useFront;
    [self setInputDevice];
}

-(void) initLiveFeed
{
    AVCaptureVideoPreviewLayer * previewLayer = [[AVCaptureVideoPreviewLayer alloc] init];
    previewLayer.bounds = self.bounds;
    previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    previewLayer.position=CGPointMake(CGRectGetMidX(self.bounds),
                                      CGRectGetMidY(self.bounds));
    captureSession = [[AVCaptureSession alloc] init];
    [self setInputDevice];
    AVCaptureVideoDataOutput *output = [[AVCaptureVideoDataOutput alloc] init];
    [captureSession addOutput:output];
    [(AVCaptureConnection *) output.connections[0]
     setVideoOrientation:AVCaptureVideoOrientationPortrait];
    dispatch_queue_t queue = dispatch_queue_create("urboQueue", NULL);
    [output setSampleBufferDelegate:self queue:queue];
    output.videoSettings = @{(id)kCVPixelBufferPixelFormatTypeKey:
                                 @(kCVPixelFormatType_32BGRA)};
    [previewLayer setSession:captureSession];
    [self.layer addSublayer:previewLayer];
}

- (void)setInputDevice
{
    if ([[AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo] count] > 1)
    {
        NSError *error;
        AVCaptureDeviceInput *newVideoInput;
        if (useFrontCamera)
        {
            captureSession.sessionPreset = AVCaptureSessionPreset640x480;
            newVideoInput = [[AVCaptureDeviceInput alloc]
                             initWithDevice:[self cameraWithPosition:
                                             AVCaptureDevicePositionFront]
                             error:&error];
            [[Urbo getInstance].pexeso initLiveFeed:480 height:640];
        }
        else
        {
            captureSession.sessionPreset = AVCaptureSessionPreset1280x720;
            newVideoInput = [[AVCaptureDeviceInput alloc]
                             initWithDevice:[self cameraWithPosition:
                                             AVCaptureDevicePositionBack]
                             error:&error];
            [[Urbo getInstance].pexeso initLiveFeed:720 height:1280];
        }
        
        if (newVideoInput != nil)
        {
            [captureSession beginConfiguration];
            for (AVCaptureDeviceInput *input in captureSession.inputs)
            {
                [captureSession removeInput:input];
            }
            [captureSession addInput:newVideoInput];
            [captureSession commitConfiguration];
        }
    }
}

- (AVCaptureDevice *) cameraWithPosition:(AVCaptureDevicePosition) position
{
    NSArray *devices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    for (AVCaptureDevice *device in devices)
    {
        if ([device position] == position)
        {
            return device;
        }
    }
    return nil;
}

-(void)captureOutput:(AVCaptureOutput *)captureOutput
didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer
      fromConnection:(AVCaptureConnection *)connection
{
    [[Urbo getInstance].pexeso pushFrame:sampleBuffer];
}

- (void) unFreeze
{
    [captureSession startRunning];
}

-(void) freeze
{
    [captureSession stopRunning];
}

- (void)dealloc
{
    [[Urbo getInstance].pexeso stopLiveFeed];
}

@end
