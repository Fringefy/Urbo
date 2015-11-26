#import <UIKit/UIKit.h>
#import "UrboCameraView.h"
#import "Urbo.h"

@interface ViewController : UIViewController <UrboDelegate>

@property (weak, nonatomic) IBOutlet UrboCameraView *urboCameraView;
@property (weak, nonatomic) IBOutlet UILabel *stateLabel;
@property (weak, nonatomic) IBOutlet UILabel *poiLabel;

@end

