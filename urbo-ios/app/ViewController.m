#import "ViewController.h"
#import "APIClient.h"
#import "POI.h"

@interface ViewController ()
{
	POI *recognisedPoi;
}
@end


@implementation ViewController


- (void)viewDidLoad {
	[super viewDidLoad];
	[Urbo sharedInstance].delegate = self;
}

-(void) viewDidAppear:(BOOL)animated
{
	[super viewDidAppear:animated];
	[self.urboCameraView unFreeze];
}

- (void)didReceiveMemoryWarning {
	[super didReceiveMemoryWarning];
}

-(void)urbo:(Urbo *)urbo didChangeState:(State)state withPoi:(POI*)poi
{
	dispatch_async(dispatch_get_main_queue(), ^{
		self.poiLabel.text = @"";
		switch(state) {
			case STATE_SEARCH:
				NSLog(@"STATE_SEARCH \n");
				self.stateLabel.text = @"STATE_SEARCH";
				break;
			case STATE_RECOGNITION:
				recognisedPoi = poi;
				self.poiLabel.text = poi.name;
				NSLog(@"STATE_RECOGNITION \n");
				self.stateLabel.text = @"STATE_RECOGNITION";
				break;
			case STATE_NO_RECOGNITION:
				NSLog(@"STATE_NO_RECOGNITION \n");
				self.stateLabel.text = @"STATE_NO_RECOGNITION";
				break;
			case STATE_NON_INDEXABLE:
				NSLog(@"STATE_NON_INDEXABLE \n");
				self.stateLabel.text = @"STATE_NON_INDEXABLE";
				break;
			case STATE_BAD_ORIENTATION:
				NSLog(@"STATE_BAD_ORIENTATION \n");
				self.stateLabel.text = @"STATE_BAD_ORIENTATION";
				break;
			case STATE_MOVING:
				NSLog(@"MOVING \n");
				self.stateLabel.text = @"MOVING";
				break;
			default:
				NSLog(@"Error");
		}
	});
}

@end
