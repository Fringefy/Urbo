#import "ViewController.h"
#import "APIClient.h"
#import "POI.h"
#import "Snapshot.h"

@interface ViewController ()
{
    POI *recognisedPoi;
    int lastRecognizedSnapshotId;
    Snapshot *returnedSnapshot;
}
@end


@implementation ViewController


- (void)viewDidLoad {
    [super viewDidLoad];
    self.textField.delegate = self;
    [Urbo getInstance].delegate = self;
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(keyboardShown:)
                                                 name:UIKeyboardWillShowNotification
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(keyboardHidden:)
                                                 name:UIKeyboardDidHideNotification
                                               object:nil];
}

-(void) viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    [self.urboCameraView unFreeze];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
}

-(void)urboDidChangeState:(StateId)state withPoi:(POI *)poi andSnapshotId:(int)snapshotId;
{
    dispatch_async(dispatch_get_main_queue(), ^{
        self.poiLabel.text = @"";
        switch (state) {
            case SEARCH:
                NSLog(@"SEARCH \n");
                self.stateLabel.text = @"SEARCH";
                break;
            case RECOGNITION:
                lastRecognizedSnapshotId = snapshotId;
                recognisedPoi = poi;
                self.poiLabel.text = poi.name;
                NSLog(@"RECOGNITION \n");
                self.stateLabel.text = @"RECOGNITION";
                break;
            case NO_RECOGNITION:
                NSLog(@"NO_RECOGNITION \n");
                self.stateLabel.text = @"NO_RECOGNITION";
                break;
            case NON_INDEXABLE:
                NSLog(@"NON_INDEXABLE \n");
                self.stateLabel.text = @"NON_INDEXABLE";
                break;
            case BAD_ORIENTATION:
                NSLog(@"BAD_ORIENTATION \n");
                self.stateLabel.text = @"BAD_ORIENTATION";
                break;
            case MOVING:
                NSLog(@"MOVING \n");
                self.stateLabel.text = @"MOVING";
                break;
            default:
                NSLog(@"Error");
        }
    });
}

-(IBAction) tagAction
{
    self.tagView.hidden = NO;
    [[Urbo getInstance] takeSnapshot];
}

-(IBAction)applyPoi:(id)sender
{
    [self.textField resignFirstResponder];
    POI *newPOI = [[POI alloc] init];
    newPOI.name = self.textField.text;
    newPOI.type = @(0);
    [[Urbo getInstance] tagSnapshot:returnedSnapshot poi:newPOI];
    self.poiImageView.image = nil;
    self.tagView.hidden = YES;
    [self.urboCameraView unFreeze];
}

- (void)keyboardShown:(NSNotification*)notification
{
    NSDictionary* keyboardInfo = [notification userInfo];
    NSValue* keyboardFrameBegin = [keyboardInfo valueForKey:UIKeyboardFrameBeginUserInfoKey];
    CGRect keyboardFrameBeginRect = [keyboardFrameBegin CGRectValue];
    self.bottomConstaraint.constant = keyboardFrameBeginRect.size.height;
}
- (void)keyboardHidden:(NSNotification*)notification
{
    self.bottomConstaraint.constant = 0;
}

-(void)urboOnSnapshot:(Snapshot *)snapshot
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.poiImageView setImage:snapshot.snapshotImage];
        [self.urboCameraView freeze];
        returnedSnapshot = snapshot;
    });

}

@end
