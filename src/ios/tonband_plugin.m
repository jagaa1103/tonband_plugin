/********* tonband_plugin.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
#import "Bluetooth.h"
@interface tonband_plugin : CDVPlugin {
  // Member variables go here.
    Bluetooth *bluetooth;
    CDVPluginResult* pluginResult;
}

-(void)checkBluetooth:(CDVInvokedUrlCommand*)command;
-(void)scan:(CDVInvokedUrlCommand*)command;

@property (nonatomic, strong) NSString* myCallbackId;

@end

@implementation tonband_plugin


-(void)checkBluetooth:(CDVInvokedUrlCommand*)command
{
    self.myCallbackId = command.callbackId;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.myCallbackId];
    NSLog(@":::::: checkBluetooth ::::::");
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(broadcastReceiver:) name:@"tonband_channel" object:nil];
    bluetooth = [[Bluetooth alloc] init];
}

-(void)scan:(CDVInvokedUrlCommand*)command
{
    self.myCallbackId = command.callbackId;
    [self.commandDelegate runInBackground:^{
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.myCallbackId];
        [bluetooth startScan];
    }];
}

-(void)startLoop:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        [bluetooth startLoop];
    }];
}


-(void) broadcastReceiver : (NSNotification * ) notification
{
    NSDictionary *dict = [notification object];
    NSString *time = [self getTime];
    NSString *message = [NSString stringWithFormat:@"%@: %@", time, dict];
    NSLog(@"%@", message);
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.myCallbackId];
}


-(NSString *) getTime
{
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    [formatter setDateFormat:@"HH:mm:ss"];
    NSDate *currentDate = [NSDate date];
    return [formatter stringFromDate:currentDate];
}


@end
