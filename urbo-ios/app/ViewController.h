#import <UIKit/UIKit.h>
#import "UrboCameraView.h"
#import "Urbo.h"

@interface ViewController : UIViewController <UrboDelegate,UITextFieldDelegate>

@property (weak, nonatomic) IBOutlet UrboCameraView *urboCameraView;
@property (weak, nonatomic) IBOutlet UILabel *stateLabel;
@property (weak, nonatomic) IBOutlet UILabel *poiLabel;
@property (weak, nonatomic) IBOutlet UIView *tagView;
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *bottomConstaraint;
@property (weak, nonatomic) IBOutlet UITextField *textField;
@property (weak, nonatomic) IBOutlet UIImageView *poiImageView;

@end

