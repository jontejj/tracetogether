//
//  BTLEScannerService.swift
//  Trace Together EU
//
//  Created by Daniel Skantze on 2020-04-04.
//  Copyright © 2020 peltarion. All rights reserved.
//

import Foundation
import CoreBluetooth

class BTLEScannerService : NSObject, CBCentralManagerDelegate  {
  fileprivate var manager:CBCentralManager? = nil
  var discoveredPeripherals = Set<CBPeripheral>()
  
  override init() {
    
  }
  
  func createBluetoothManager() {
    self.manager = CBCentralManager.init(delegate: self, queue: nil)
  }
  
  // FIXME: Check if more keys need to be added
  func startScanner() {
    self.manager?.scanForPeripherals(withServices: [kAdvertisingServiceUUID], options: [CBCentralManagerScanOptionAllowDuplicatesKey: false])
  }
  
  func centralManagerDidUpdateState(_ central: CBCentralManager) {
    print("centralManagerDidUpdateState")
    switch (central.state) {
    case .poweredOn:
      print("poweredOn")
      self.startScanner()
      //self.createBluetoothService()
    case .poweredOff:
      print("poweredOff")
    case .unsupported:
      print("unsupported")
    case .resetting:
      print("resetting")
    case .unauthorized:
      print("unauthorized")
    case .unknown:
      print("unknown")
    @unknown default:
      fatalError("Unkown BTLE state")
    }
  }
  
  func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
    self.manager?.connect(peripheral)
    discoveredPeripherals.insert(peripheral)
    print(advertisementData)
  }
  
  func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
    peripheral.delegate = self
    peripheral.discoverServices([kAdvertisingServiceUUID])
  }
  
  func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
    peripheral.delegate = nil
    discoveredPeripherals.remove(peripheral)
  }
  
}

extension BTLEScannerService : CBPeripheralDelegate {
  func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
    guard let services = peripheral.services else { return }

    if services.count > 0 {
      peripheral.discoverCharacteristics(nil, for: services[0])
    }
  }
  
  func peripheral(_ peripheral: CBPeripheral, didModifyServices invalidatedServices: [CBService]) {
    guard let services = peripheral.services else { return }
    
    if services.count > 0 {
      peripheral.discoverCharacteristics([kAdvertisingServiceCharacteristicUUID], for: services[0])
    }
  }
  
  func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
    guard let characteristics = service.characteristics else { return }

    for characteristic in characteristics {
      print(characteristic)
      if characteristic.properties.contains(.read) {
        print("\(characteristic.uuid): properties contains .read")
        peripheral.readValue(for: characteristic)
      }
    }
  }
  func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
    switch characteristic.uuid {
      case kAdvertisingServiceCharacteristicUUID:
        guard let data = characteristic.value else {
          print("no value")
          return
        }
        guard let clientId = String.init(data: data, encoding: .utf8) else {
          print("Unparseable clientId")
          return
        }
        print(clientId)
      default:
        print("Unhandled Characteristic UUID: \(characteristic.uuid)")
    }
  }
}

extension BTLEScannerService : ScannerServiceProtocol {
  func start() {
    if self.manager == nil {
      createBluetoothManager()
    }
  }
  
  func stop() {
    
  }
}
