#import "ARToolKitPlugin.h"
#import <Cordova/CDVPlugin.h>

@implementation ARToolKitPlugin

- (void)greet:(CDVInvokedUrlCommand*)command
{

    CDVPluginResult* pluginResult = nil;
    NSString* name = [command.arguments objectAtIndex:0];
    NSString* msg = [NSString stringWithFormat: @"Hello, %@", name];

    if (name != nil && [name length] > 0) {
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:msg];
    } else {
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
