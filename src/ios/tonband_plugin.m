/********* tonband_plugin.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
#import "Bluetooth.h"
@interface tonband_plugin : CDVPlugin {
  // Member variables go here.
    Bluetooth *bluetooth;
    NSString *callbackId;
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
    callbackId = command.callbackId;
    NSLog(@":::::: checkBluetooth ::::::");
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(broadcastReceiver:) name:@"tonband_channel" object:nil];
    bluetooth = [[Bluetooth alloc] init];
}

-(void)scan:(CDVInvokedUrlCommand*)command
{
    callbackId = command.callbackId;
    NSLog(@":::::: startScan ::::::");
    if(bluetooth == nil){
        bluetooth = [[Bluetooth alloc] init];
    }
    [bluetooth startScan];
}

-(void) broadcastReceiver : (NSNotification * ) notification
{
    NSDictionary *dict = [notification object];
    NSLog(@"%@", dict);
    NSString *time = [self getTime];
//    _textView.text = [NSString stringWithFormat:@"%@\n%@ >> %@", _textView.text, time, dict];
    NSString *message = [NSString stringWithFormat:@"%@: %@", time, dict];
    NSLog(@"%@", message);
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}


-(NSString *) getTime
{
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    [formatter setDateFormat:@"HH:mm:ss"];
    NSDate *currentDate = [NSDate date];
    return [formatter stringFromDate:currentDate];
}


@end
