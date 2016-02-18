#import "ViewController.h"
#import "APIClient.h"
#import "POI.h"
#import "PoiVote.h"
#import "Snapshot.h"

@interface ViewController ()
{
    Snapshot *lastSnapshot;
}
@end


@implementation ViewController


- (void)viewDidLoad {
    [super viewDidLoad];
    [Urbo start:self withApiKey:NSLocalizedStringFromTable(@"ApiKey", @"ApiKey", nil)];
    self.textField.delegate = self;
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

-(void)onStateChanged:(StateId)stateId withSnapshot:(Snapshot*)snapshot;
{
    dispatch_async(dispatch_get_main_queue(), ^{
        self.poiLabel.text = @"";
        switch (stateId) {
            case SEARCH:
                logIt([NSString stringWithFormat:@"SEARCH"]);
                self.stateLabel.text = @"SEARCH";
                break;
            case RECOGNITION:
                lastSnapshot = snapshot;
                self.poiLabel.text = snapshot.poi.name;
                logIt([NSString stringWithFormat:@"RECOGNITION %@", snapshot.poi.name]);
                self.stateLabel.text = @"RECOGNITION";
                break;
            case NO_RECOGNITION:
                logIt([NSString stringWithFormat:@"NO_RECOGNITION"]);
                self.stateLabel.text = @"NO_RECOGNITION";
                break;
            case NON_INDEXABLE:
                logIt([NSString stringWithFormat:@"NON_INDEXABLE"]);
                self.stateLabel.text = @"NON_INDEXABLE";
                break;
            case BAD_ORIENTATION:
                logIt([NSString stringWithFormat:@"BAD_ORIENTATION"]);
                self.stateLabel.text = @"BAD_ORIENTATION";
                break;
            case MOVING:
                logIt([NSString stringWithFormat:@"MOVING"]);
                self.stateLabel.text = @"MOVING";
                break;
            case COLD_START:
                logIt([NSString stringWithFormat:@"COLD_START"]);
                self.stateLabel.text = @"COLD_START";
                break;
            default:
                [self onError:CODE_ERROR message:[NSString
                    stringWithFormat:@"UNexpected state %d", (int)stateId]];
        }
    });
}

void logIt(NSString *string) {

    static NSDateFormatter *timeFormatter = nil;
    static NSString *writePath = nil;

    NSLog(@"%@", string);
    if (timeFormatter == nil) {
        timeFormatter = [[NSDateFormatter alloc] init];
        [timeFormatter setDateFormat:@"HH:mm:ss.SSS"];
    }
    NSDate *date = [NSDate date];
    string = [NSString stringWithFormat:@"%@ - %@\n",
              [timeFormatter stringFromDate:date], string];

    if (writePath == nil) {
        NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
        [dateFormatter setDateFormat:@"YYYY-MM-dd"];
        NSString *path = [NSString stringWithFormat:@"%@-logFile.txt",
                          [dateFormatter stringFromDate:date]];
        writePath = [[
                      NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES)
                      objectAtIndex:0] stringByAppendingPathComponent:path];
    }
    NSFileHandle *fileHandle = [NSFileHandle fileHandleForWritingAtPath:writePath];
    if (fileHandle) {
        [fileHandle seekToEndOfFile];
        [fileHandle writeData:[string dataUsingEncoding:NSUTF8StringEncoding]];
        [fileHandle closeFile];
    }
    else {
        [string writeToFile:writePath atomically:YES encoding:NSUTF8StringEncoding error:nil];
    }
}


-(void)onError:(SeverityCode)errorCode message:(NSString *)message
{
    switch (errorCode) {
        case CODE_DBG:
            logIt([NSString stringWithFormat:@"DEBUG: %@", message]);
            break;
        case CODE_INFORMATION:
            logIt([NSString stringWithFormat:@"INFO: %@", message]);
            break;
        case CODE_WARNING:
            logIt([NSString stringWithFormat:@"WARN: %@", message]);
            break;
        case CODE_ERROR:
            logIt([NSString stringWithFormat:@"ERROR: %@", message]);
            break;
        default:
            logIt([NSString stringWithFormat:@"Error %d, %@", (int)errorCode, message]);
    }
}

-(IBAction) tagAction
{
    self.tagView.hidden = NO;
    if (lastSnapshot) {
        [self.poiImageView setImage:lastSnapshot.snapshotImage];
        [self.textField setText:lastSnapshot.poi.name];
        [self.urboCameraView freeze];
    }
    else {
        [[Urbo getInstance] takeSnapshot];
    }
}

-(IBAction)applyPoi:(id)sender
{
    [self.textField resignFirstResponder];

    if (self.textField.text.length == 0) {
        [[Urbo getInstance] rejectRecognition:lastSnapshot];
    }
    else if ([self.textField.text isEqual:lastSnapshot.poi.name]) {
        [[Urbo getInstance] confirmRecognition:lastSnapshot];
    }
    else {
        NSString* newPOIname = [self.textField.text
            stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
        POI* newPOI = [[POI alloc] init:newPOIname];
        newPOI.firstComment = @"from Urbo app for iOS";
        for (id vote in lastSnapshot.votes) {
            POI* poi = ((PoiVote*)vote).poi;
            if ([poi.name isEqual:newPOIname]) {
                newPOI = poi;
                break;
            }
        }
        [[Urbo getInstance] tagSnapshot:lastSnapshot poi:newPOI];
    }
    lastSnapshot = nil;
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

-(void)onSnapshot:(Snapshot *)snapshot
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.poiImageView setImage:snapshot.snapshotImage];
        [self.urboCameraView freeze];
        lastSnapshot = snapshot;
    });

}

@end
