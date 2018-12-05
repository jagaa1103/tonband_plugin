//
//  Bluetooth.m
//  TonbandTestApp_iOS
//
//  Created by Enkhjargal Gansukh on 16/11/2018.
//  Copyright © 2018 Enkhjargal Gansukh. All rights reserved.
//

#import "Bluetooth.h"

@implementation Bluetooth
NSString *SERVICE_UUID = @"0783B03E-8535-B5A0-7140-A304D2495CB7";
NSString *TX_CHARACTERISTIC = @"0783B03E-8535-B5A0-7140-A304D2495CB8";
NSString *RX_CHARACTERISTIC = @"0783B03E-8535-B5A0-7140-A304D2495CBA";

uint8_t TEMPERATURE_REQ[] = {0xF7, 0x01, 0x01, 0x00, 0xF9};
uint8_t BATTERY_REQ[] = {0xF7, 0x02, 0x01, 0x00, 0xFA};
uint8_t ALARMTEMPERATURE_SET[] = { 0xF7, 0x05, 0x02, 0x00, 0x00, 0x00 };
uint8_t Temperature_CFM_HEADER = 16;
uint8_t BatteryStatus_CFM_HEADER = 32;
uint8_t AlarmTemperature_IND_HEADER = 48;
uint8_t AlarmTemperature_RES_HEADER = 3;
uint8_t AlarmBattery_IND_HEADER = 64;
uint8_t AlarmBattery_RES_HEADER = 4;
uint8_t AlarmTemperature_CFM_HEADER = 80;

CBCentralManager *centralManager = nil;
NSMutableArray<CBPeripheral *> *devices = nil;
CBPeripheral *connectedDevice = nil;

- (instancetype)init
{
    self = [super init];
    if (self) {
        centralManager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
        NSLog(@"============ START_SERVICE ===========");
        [[NSNotificationCenter defaultCenter] postNotificationName:@"tonband_channel" object:@"START_SERVICE"];
    }
    return self;
}

-(void)centralManagerDidUpdateState:(nonnull CBCentralManager *)central {
    switch ([central state]) {
        case CBManagerStateUnknown:
            break;
        case CBManagerStatePoweredOn:
            break;
        case CBManagerStatePoweredOff:
            break;
        case CBManagerStateUnsupported:
            break;
        case CBManagerStateUnauthorized:
            break;
        default:
            break;
    }
}
// ******************* Commands ****************************************
// *********************************************************************
-(void) startScan
{
    NSLog(@"========== startScan ============");
    devices = [[NSMutableArray alloc] init];
    CBUUID *serviceUUID = [CBUUID UUIDWithString:SERVICE_UUID];
    [centralManager scanForPeripheralsWithServices:@[serviceUUID] options:nil];
}
-(void) stopScan
{
    [centralManager stopScan];
}

-(Boolean) connect:(NSString *) uuid
{
    if(connectedDevice != nil) return true;
    CBPeripheral *detectedDevice = nil;
    for(CBPeripheral *peripheral in devices){
        if([[peripheral.identifier UUIDString] isEqualToString:uuid]) detectedDevice = peripheral;
    }
    if(detectedDevice != nil) {
        [centralManager connectPeripheral:detectedDevice options:nil];
        return true;
    }else{
        return false;
    }
}

-(void) disconnect
{
    if(connectedDevice) [centralManager cancelPeripheralConnection:connectedDevice];
}

NSTimer *timer = nil;
-(void) startLoop: (NSString *) time
{
    float t = [time floatValue] * 60;
    timer = [NSTimer scheduledTimerWithTimeInterval:t target:self selector:@selector(sendRequestTemp) userInfo:nil repeats:YES];
}

-(void) resetSettings:(NSString *)time
{
    [self sendRequestTemp];
    if(timer != nil){
        [timer invalidate];
        timer = nil;
    }
    [self startLoop: time];
}
-(void) setAlarmTemperature:(NSString *)tempHex
{
    uint8_t *data = [self hexStringToByte:tempHex];
    ALARMTEMPERATURE_SET[3] = data[1];
    ALARMTEMPERATURE_SET[4] = data[0];
    uint8_t checksum = ALARMTEMPERATURE_SET[0] + ALARMTEMPERATURE_SET[1] + ALARMTEMPERATURE_SET[2] + ALARMTEMPERATURE_SET[3] + ALARMTEMPERATURE_SET[4];
    ALARMTEMPERATURE_SET[5] = checksum;
    [self sendToSensor:ALARMTEMPERATURE_SET :6];
}
-(void) requestBattery
{
    [NSTimer scheduledTimerWithTimeInterval:2.0f repeats:NO block:^(NSTimer *time) {
        [self sendToSensor:BATTERY_REQ :5];
    }];
}

-(void) parseData :(uint8_t[]) data :(NSInteger) length
{
    NSString *header = nil;
    int message = 0;
    
    int l = 0;
    
    if(data[0] != 247) return;
    header = [self parseHeader:data[1]];
    l = data[2];
    uint8_t dataArray[l];
    int counter = 0;
    for(int i=3; i<(l+3); i++){
        dataArray[counter] = data[i];
        counter ++;
    }
    message = [self parseBody:header :dataArray :l];
    NSLog(@"header: %@, body: %d", header, message);
    if(header == nil || message == -1) return;
    NSString *messageString = [NSString stringWithFormat:@"%d", message];
    NSDictionary *dic = @{@"header": header, @"data": messageString};
    [_delegate onDataChanged: dic];
}

-(NSString *) parseHeader: (uint8_t)data
{
    NSString *header = nil;
    switch (data) {
        case 16:
            header = @"TEMPERATURE_CFM_HEADER";
            break;
        case 32:
            header = @"BATTERY_CFM_HEADER";
            break;
        case 48:
            header = @"ALARMTEMPERATURE_IND_HEADER";
            break;
        case 64:
            header = @"ALARMBATTERY_IND_HEADER";
            break;
        case 80:
            header = @"ALARMTEMPERATURE_CFM_HEADER";
            break;
    }
    return header;
}

-(int) parseBody: (NSString *)header :(uint8_t[]) data :(int) length
{
    int parsedData = -1;
    if([header isEqualToString:@"TEMPERATURE_CFM_HEADER"]) {
        parsedData = ((uint8_t)data[1] << 8) | ((uint8_t)data[0]);
    }else if([header isEqualToString:@"BATTERY_CFM_HEADER"]){
        parsedData = (int)data[0];
    }else if([header isEqualToString:@"ALARMTEMPERATURE_IND_HEADER"]){
        parsedData = ((uint8_t)data[1] << 8) | ((uint8_t)data[0]);
    }else if([header isEqualToString:@"ALARMBATTERY_IND_HEADER"]){
        parsedData = (int)data[0];
    }else if([header isEqualToString:@"ALARMTEMPERATURE_CFM_HEADER"]){
        
    }
    return parsedData;
}

-(void) sendToSensor :(uint8_t[]) data :(int) length
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        dispatch_async(dispatch_get_main_queue(), ^(void) {
            NSData *d = [NSData dataWithBytes:data length:length];
            NSLog(@"sendToSensor :: %@", d);
            if(rxCharacteristic != nil){
                @try{ [connectedDevice writeValue:d forCharacteristic:rxCharacteristic type:CBCharacteristicWriteWithoutResponse]; }
                @catch(NSException *err){ NSLog(@"%@", err.debugDescription); }
            }
        });
    });
}




// *********************************************************************
// *********************************************************************

- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error
{
    connectedDevice = nil;
//    [[NSNotificationCenter defaultCenter] postNotificationName:@"tonband_channel" object:@"DEVICE_DISCONNECTED"];
    NSLog(@"DEVICE_DISCONNECTED");
    
    [_delegate onDisconnected];
}
-(void)centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral
{
    connectedDevice = peripheral;
    [centralManager stopScan];
    connectedDevice.delegate = self;
    [connectedDevice discoverServices:nil];
    NSDictionary *dict = @{@"uuid": [connectedDevice.identifier UUIDString], @"name": connectedDevice.name};
    [_delegate onConnected: dict];
}
- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary<NSString *,id> *)advertisementData RSSI:(NSNumber *)RSSI
{
    NSDictionary *device = @{@"uuid": [peripheral.identifier UUIDString], @"name": peripheral.name};
    [_delegate onScannedDevices:device];
    [devices addObject:peripheral];
}

- (void)peripheral:(CBPeripheral *)peripheral didDiscoverServices:(NSError *)error
{
    for(CBService *service in peripheral.services){
        if([service.UUID.UUIDString isEqualToString:SERVICE_UUID] == YES){
            [peripheral discoverCharacteristics:nil forService:service];
        }
    }
}


CBCharacteristic *rxCharacteristic = nil;
CBCharacteristic *txCharacteristic = nil;
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverCharacteristicsForService:(CBService *)service error:(NSError *)error
{
    for(CBCharacteristic *characteristic in service.characteristics) {
        if([characteristic.UUID.UUIDString isEqualToString:RX_CHARACTERISTIC] == YES) {
            rxCharacteristic = characteristic;
        }
        if([characteristic.UUID.UUIDString isEqualToString:TX_CHARACTERISTIC] == YES) {
            txCharacteristic = characteristic;
            [connectedDevice setNotifyValue:YES forCharacteristic: txCharacteristic];
        }
    }
}

- (void)peripheral:(CBPeripheral *)peripheral didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error
{
    NSLog(@"%@", characteristic.value);
    uint8_t *uints = (uint8_t*)[characteristic.value bytes];
    NSUInteger len = [characteristic.value length]/sizeof(uint8_t);
    memcpy(uints, [characteristic.value bytes], len);
    [self parseData:uints :len];
}


-(void)sendRequestTemp
{
   [self sendToSensor:TEMPERATURE_REQ :5];
}

-(uint8_t*) hexStringToByte :(NSString *) hexString
{
    uint8_t *result = (uint8_t *)malloc(sizeof(uint8_t) * ([hexString length] / 2));
    for(int i = 0; i < [hexString length]; i += 2) {
        NSRange range = { i, 2 };
        NSString *subString = [hexString substringWithRange:range];
        unsigned value;
        [[NSScanner scannerWithString:subString] scanHexInt:&value];
        result[i / 2] = (uint8_t)value;
    }
    return result;
}

@end


