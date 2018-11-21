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

-(void) startScan
{
    devices = [[NSMutableArray alloc] init];
    CBUUID *serviceUUID = [CBUUID UUIDWithString:SERVICE_UUID];
    [centralManager scanForPeripheralsWithServices:@[serviceUUID] options:nil];
}

- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error
{
    connectedDevice = nil;
    [[NSNotificationCenter defaultCenter] postNotificationName:@"tonband_channel" object:@"DEVICE_DISCONNECTED"];
}
-(void)centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral
{
    connectedDevice = peripheral;
    [centralManager stopScan];
    connectedDevice.delegate = self;
    [connectedDevice discoverServices:nil];
    NSString *msg = [NSString stringWithFormat:@"%@: %@", @"DEVICE_CONNECTED", [connectedDevice.identifier UUIDString]];
    [[NSNotificationCenter defaultCenter] postNotificationName:@"tonband_channel" object:msg];
}
- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary<NSString *,id> *)advertisementData RSSI:(NSNumber *)RSSI
{
    [[NSNotificationCenter defaultCenter] postNotificationName:@"tonband_channel" object:@"NEW_DEVICE_SCANNED"];
    [devices addObject:peripheral];
    [centralManager connectPeripheral:devices[0] options:nil];
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
    [self parseHeader:uints :len];
}


-(void)sendRequestTemp
{
    NSData *data = [NSData dataWithBytes:TEMPERATURE_REQ length:5];
    if(rxCharacteristic != nil){
        @try{ [connectedDevice writeValue:data forCharacteristic:rxCharacteristic type:CBCharacteristicWriteWithoutResponse]; }
        @catch(NSException *err){ NSLog(@"%@", err.debugDescription); }
    }
}

NSTimer *timer = nil;
-(void) startLoop
{
    [self sendRequestTemp];
    timer = [NSTimer scheduledTimerWithTimeInterval:10.0f target:self selector:@selector(sendRequestTemp) userInfo:nil repeats:YES];
}

-(void) parseHeader :(uint8_t[]) data :(NSInteger) length
{
    
    for(int i = 0; i < length; i++){
        if(i == 0 && data[i] != 247) return;
        if(i == 1) {
            switch (data[i]) {
                case 16:
                    NSLog(@"Header: TEMPERATURE_CFM_HEADER");
                    [[NSNotificationCenter defaultCenter] postNotificationName:@"tonband_channel" object:@"TEMPERATURE_CFM_HEADER"];
                    break;
            }
        }
    }
}

@end


