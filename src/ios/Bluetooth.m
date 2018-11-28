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

NSTimer *timer = nil;
-(void) startLoop
{
    [self sendRequestTemp];
    timer = [NSTimer scheduledTimerWithTimeInterval:10.0f target:self selector:@selector(sendRequestTemp) userInfo:nil repeats:YES];
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
    if(header == nil || message == 0) return;
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
            header = @"BatteryStatus_CFM_HEADER";
            break;
        case 48:
            header = @"AlarmTemperature_IND_HEADER";
            break;
        case 3:
            header = @"AlarmTemperature_RES_HEADER";
            break;
        case 4:
            header = @"AlarmBattery_RES_HEADER";
            break;
        case 64:
            header = @"AlarmBattery_IND_HEADER";
            break;
        case 80:
            header = @"AlarmTemperature_CFM_HEADER";
            break;
    }
    return header;
}

-(int) parseBody: (NSString *)header :(uint8_t[]) data :(int) length
{
    int parsedData = 0;
    if([header isEqualToString:@"TEMPERATURE_CFM_HEADER"]) {
        parsedData = ((uint8_t)data[1] << 8) | ((uint8_t)data[0]);
    }else if([header isEqualToString:@"AlarmTemperature_IND_HEADER"] || [header isEqualToString:@"AlarmTemperature_IND_HEADER"]){
        parsedData = ((uint8_t)data[1] << 8) | ((uint8_t)data[0]);
    }else {
        parsedData = data[0];
    }
    return parsedData;
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
//    NSString *msg = [NSString stringWithFormat:@"%@: %@", @"DEVICE_CONNECTED", [connectedDevice.identifier UUIDString]];
//    [[NSNotificationCenter defaultCenter] postNotificationName:@"tonband_channel" object:msg];
}
- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary<NSString *,id> *)advertisementData RSSI:(NSNumber *)RSSI
{
//    [[NSNotificationCenter defaultCenter] postNotificationName:@"tonband_channel" object:@"NEW_DEVICE_SCANNED"];
    NSDictionary *device = @{@"uuid": [peripheral.identifier UUIDString], @"name": peripheral.name};
    [_delegate onScannedDevices:device];
    [devices addObject:peripheral];
//    [centralManager connectPeripheral:devices[0] options:nil];
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
    NSData *data = [NSData dataWithBytes:TEMPERATURE_REQ length:5];
    if(rxCharacteristic != nil){
        @try{ [connectedDevice writeValue:data forCharacteristic:rxCharacteristic type:CBCharacteristicWriteWithoutResponse]; }
        @catch(NSException *err){ NSLog(@"%@", err.debugDescription); }
    }
}

@end


