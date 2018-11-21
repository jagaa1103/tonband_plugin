//
//  Bluetooth.h
//  TonbandTestApp_iOS
//
//  Created by Enkhjargal Gansukh on 16/11/2018.
//  Copyright © 2018 Enkhjargal Gansukh. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>

@interface Bluetooth : NSObject <CBCentralManagerDelegate, CBPeripheralDelegate>

-(void) startScan;
-(void) startLoop;

@end
