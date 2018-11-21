/********* tonband_plugin.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
#import "Bluetooth.h"
@interface tonband_plugin : CDVPlugin {
  // Member variables go here.
    Bluetooth *bluetooth;
}

-(void)coolMethod:(CDVInvokedUrlCommand*)command;
-(void)checkBluetooth:(CDVInvokedUrlCommand*)command;
-(void)scan:(CDVInvokedUrlCommand*)command;
@end

@implementation tonband_plugin

- (void)coolMethod:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSString* echo = [command.arguments objectAtIndex:0];

    if (echo != nil && [echo length] > 0) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:echo];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


-(void)checkBluetooth:(CDVInvokedUrlCommand*)command
{
    NSLog(@":::::: checkBluetooth ::::::");
    bluetooth = [[Bluetooth alloc] init];
}

-(void)scan:(CDVInvokedUrlCommand*)command
{
    NSLog(@":::::: startScan ::::::");
    if(bluetooth == nil){
        bluetooth = [[Bluetooth alloc] init];
    }
    [bluetooth startScan];
}


@end
