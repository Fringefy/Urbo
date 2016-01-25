#import <Foundation/Foundation.h>
#import "RecoEvent.h"
#import "POI.h"

@interface Snapshot : NSObject

@property (nonatomic) POI *poi;
@property (nonatomic) RecoEvent *recoEvent;
@property (nonatomic) UIImage *snapshotImage;
@property (nonatomic) NSArray *votes;

@end
