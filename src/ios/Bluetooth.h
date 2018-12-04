//
//  Bluetooth.h
//  TonbandTestApp_iOS
//
//  Created by Enkhjargal Gansukh on 16/11/2018.
//  Copyright © 2018 Enkhjargal Gansukh. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>


@protocol BluetoothProtocol <NSObject>
-(void) onScannedDevices: (NSDictionary *) device;
-(void) onConnected: (NSDictionary *) device;
-(void) onDisconnected;
-(void) onDataChanged: (NSDictionary *) data;
@end


@interface Bluetooth : NSObject <CBCentralManagerDelegate, CBPeripheralDelegate>


-(void) startScan;
-(void) stopScan;
-(Boolean) connect:(NSString *) uuid;
-(void) disconnect;
-(void) startLoop: (NSString *) time;
-(void) resetSettings: (NSString *) time;
-(void) setAlarmTemperature: (NSString *) temp;
-(void) requestBattery;

@property(nonatomic, retain) id<BluetoothProtocol> delegate;

@end
