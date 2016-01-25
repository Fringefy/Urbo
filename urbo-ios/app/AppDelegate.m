#import "AppDelegate.h"
#import "Urbo.h"

@interface AppDelegate ()

@end


@implementation AppDelegate

- (BOOL)application:(UIApplication *)application
didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    [Urbo startWithApiKey:@"RnNmIgxtZzcajIZww7NlKnAeYwTjOq9xp9Xu7YkS"];
    [Urbo getInstance].baseUrl = @"https://odie.fringefy.com/";
    return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application {
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
}

- (void)applicationWillTerminate:(UIApplication *)application {
}

@end
