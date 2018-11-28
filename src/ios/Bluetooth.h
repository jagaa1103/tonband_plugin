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
-(Boolean) connect:(NSString *) uuid;
-(void) startLoop;
@property(nonatomic, retain) id<BluetoothProtocol> delegate;

@end
