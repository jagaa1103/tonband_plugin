/********* tonband_plugin.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
#import "Bluetooth.h"
@interface tonband_plugin : CDVPlugin <BluetoothProtocol>{
  // Member variables go here.
    Bluetooth *bluetooth;
    CDVPluginResult* pluginResult;
}
-(void)startService:(CDVInvokedUrlCommand*)command;
-(void)checkBluetooth:(CDVInvokedUrlCommand*)command;
-(void)scan:(CDVInvokedUrlCommand*)command;
-(void)connect:(CDVInvokedUrlCommand*)command;

@property (nonatomic, strong) NSString* myCallbackId;

@end

@implementation tonband_plugin

-(void)startService:(CDVInvokedUrlCommand*)command
{
    NSLog(@"::::::::: startService ::::::::::");
    bluetooth = [[Bluetooth alloc] init];
    bluetooth.delegate = self;
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(broadcastReceiver:) name:@"tonband_channel" object:nil];
}

-(void)checkBluetooth:(CDVInvokedUrlCommand*)command
{
    self.myCallbackId = command.callbackId;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.myCallbackId];
    NSLog(@":::::: checkBluetooth ::::::");
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

-(void)connect:(CDVInvokedUrlCommand*)command
{
    NSLog(@"tonband_plugin >> %@", command.arguments[0]);
    [bluetooth connect:command.arguments[0]];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


-(void)startLoop:(CDVInvokedUrlCommand*)command
{
//    [self.commandDelegate runInBackground:^{
        [bluetooth startLoop];
//    }];
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.myCallbackId];
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


- (void)onConnected: (NSDictionary *) device {
    NSLog(@"onConnected: name: %@, uuid: %@", device[@"name"], device[@"uuid"]);
}

- (void)onDataChanged {
    NSLog(@"onDataChanged");
}

- (void)onScannedDevices: (NSDictionary *) device {
    NSError *error = nil;
    NSData *data = [NSJSONSerialization dataWithJSONObject:device options:NSJSONWritingPrettyPrinted error:&error];
    if(error == nil){
        NSString *jsonString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        NSLog(@"onScannedDevices: %@", jsonString);
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:jsonString];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:_myCallbackId];
    }else{
        NSLog(@"onScannedDevices: Error: %@", error.description);
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:_myCallbackId];
    }
}

@end
