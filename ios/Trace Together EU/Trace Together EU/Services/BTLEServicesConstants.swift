//
//  BTLEServicesConstants.swift
//  Trace Together EU
//
//  Created by Daniel Skantze on 2020-04-04.
//  Copyright © 2020 peltarion. All rights reserved.
//

import Foundation
import CoreBluetooth

let kAdvertisingServiceUUID = CBUUID(string: "3650CFEB-9C74-41D2-A21F-A1099FB0B1F0")
let kAdvertisingServiceCharacteristicUUID = CBUUID(string: "F165F05C-3FD0-4B47-88ED-E61B542E19B4")

// Does not seem to work:

//let kAdvertisingServiceUUID = CBUUID(string: "1000b81d-0000-1000-8000-00805f9b34fb")
//let kAdvertisingServiceCharacteristicUUID = CBUUID(string: "00002902-0000-1000-8000-00805f9b34fb")

