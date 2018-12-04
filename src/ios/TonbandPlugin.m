/********* tonband_plugin.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
#import "Bluetooth.h"
@interface TonbandPlugin : CDVPlugin <BluetoothProtocol>{
  // Member variables go here.
    Bluetooth *bluetooth;
    CDVPluginResult* pluginResult;
}
-(void)startService:(CDVInvokedUrlCommand*)command;
-(void)stopService:(CDVInvokedUrlCommand*)command;
-(void)checkBluetooth:(CDVInvokedUrlCommand*)command;
-(void)startScan:(CDVInvokedUrlCommand*)command;
-(void)stopScan:(CDVInvokedUrlCommand*)command;
-(void)connect:(CDVInvokedUrlCommand*)command;
-(void)startLoop:(CDVInvokedUrlCommand*)command;
-(void)resetSettings:(CDVInvokedUrlCommand*)command;
-(void)setAlarmTemperature:(CDVInvokedUrlCommand*)command;
-(void)requestBattery:(CDVInvokedUrlCommand*)command;

@property (nonatomic, strong) NSString* scanCallback;
@property (nonatomic, strong) NSString* connectionCallbackId;
@property (nonatomic, strong) NSString* dataCallbackId;

@end

@implementation TonbandPlugin

-(void)startService:(CDVInvokedUrlCommand*)command
{
    NSLog(@"::::::::: startService ::::::::::");
    bluetooth = [[Bluetooth alloc] init];
    bluetooth.delegate = self;
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(broadcastReceiver:) name:@"tonband_channel" object:nil];
}

- (void)stopService:(CDVInvokedUrlCommand *)command
{
    
}

-(void)checkBluetooth:(CDVInvokedUrlCommand*)command
{
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

-(void)startScan:(CDVInvokedUrlCommand*)command
{
    _scanCallback = command.callbackId;
    [self.commandDelegate runInBackground:^{
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:_scanCallback];
        [bluetooth startScan];
    }];
}

- (void)stopScan:(CDVInvokedUrlCommand *)command
{
    
    [bluetooth stopScan];
}

-(void)connect:(CDVInvokedUrlCommand*)command
{
    _connectionCallbackId = command.callbackId;
    [bluetooth connect:command.arguments[0]];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

-(void)disconnect:(CDVInvokedUrlCommand*)command
{
    [bluetooth disconnect];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


-(void)startLoop:(CDVInvokedUrlCommand*)command
{
    _dataCallbackId = command.callbackId;
    NSString *time = command.arguments[0];
    [bluetooth resetSettings: time];
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:_dataCallbackId];
}

- (void)resetSettings:(CDVInvokedUrlCommand *)command
{
    [bluetooth resetSettings: command.arguments[0]];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)setAlarmTemperature:(CDVInvokedUrlCommand *)command
{
    [bluetooth setAlarmTemperature:command.arguments[0]];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)requestBattery:(CDVInvokedUrlCommand *)command
{
    [bluetooth requestBattery];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}


-(void) broadcastReceiver : (NSNotification * ) notification
{
    NSDictionary *dict = [notification object];
    NSString *time = [self getTime];
    NSString *message = [NSString stringWithFormat:@"%@: %@", time, dict];
    NSLog(@"%@", message);
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    [pluginResult setKeepCallbackAsBool:YES];
    if([message containsString:@"DEVICE_CONNECTED"]) [self.commandDelegate sendPluginResult:pluginResult callbackId:self.connectionCallbackId];
}


-(NSString *) getTime
{
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    [formatter setDateFormat:@"HH:mm:ss"];
    NSDate *currentDate = [NSDate date];
    return [formatter stringFromDate:currentDate];
}


- (void)onConnected: (NSDictionary *) device {
    NSError *error = nil;
    @try{
        NSData *data = [NSJSONSerialization dataWithJSONObject:device options:NSJSONWritingPrettyPrinted error:&error];
        if(error == nil){
            NSString *jsonString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:jsonString];
            [pluginResult setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:_connectionCallbackId];
        }else{
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
            [pluginResult setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:_connectionCallbackId];
        }
    }@catch(NSException *error){
        NSLog(@"%@", error.description);
    }
}

-(void) onDisconnected
{
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:_connectionCallbackId];
}

- (void)onDataChanged: (NSDictionary *) data {
    NSError *error = nil;
    NSData *dataObject = [NSJSONSerialization dataWithJSONObject:data options:NSJSONWritingPrettyPrinted error:&error];
    if(error == nil){
        NSString *jsonString = [[NSString alloc] initWithData:dataObject encoding:NSUTF8StringEncoding];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:jsonString];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:_dataCallbackId];
    }else{
        NSLog(@"onScannedDevices: Error: %@", error.description);
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:_dataCallbackId];
    }
}

- (void)onScannedDevices: (NSDictionary *) device {
    NSError *error = nil;
    NSData *data = [NSJSONSerialization dataWithJSONObject:device options:NSJSONWritingPrettyPrinted error:&error];
    if(error == nil){
        NSString *jsonString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        NSLog(@"onScannedDevices: %@", jsonString);
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:jsonString];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:_scanCallback];
    }else{
        NSLog(@"onScannedDevices: Error: %@", error.description);
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:_scanCallback];
    }
}

@end
