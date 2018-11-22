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
-(NSDictionary *) onScannedDevices: (NSDictionary *) device;
-(NSDictionary *) onConnected: (NSDictionary *) device;
-(NSDictionary *) onDataChanged;
@end


@interface Bluetooth : NSObject <CBCentralManagerDelegate, CBPeripheralDelegate>


-(void) startScan;
-(Boolean) connect:(NSString *) uuid;
-(void) startLoop;
@property(nonatomic, retain) id<BluetoothProtocol> delegate;

@end
