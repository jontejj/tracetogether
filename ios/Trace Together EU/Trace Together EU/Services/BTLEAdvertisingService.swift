//
//  BTLEAdvertisingService.swift
//  Trace Together EU
//
//  Created by Daniel Skantze on 2020-04-03.
//  Copyright © 2020 peltarion. All rights reserved.
//

import Foundation
import CoreBluetooth

class BTLEAdvertisingService : NSObject, CBPeripheralManagerDelegate {
  let deviceId:String
  let serviceName:String
  fileprivate var manager:CBPeripheralManager? = nil
  
  init(deviceId:String, serviceName:String) {
    self.deviceId = deviceId
    self.serviceName = serviceName
    super.init()
  }
  
  private func createBluetoothService() {
    self.manager?.removeAllServices()
    let service = CBMutableService.init(type: kAdvertisingServiceUUID, primary: true)
    let characteristic = CBMutableCharacteristic.init(type: kAdvertisingServiceCharacteristicUUID, properties: .read, value: self.deviceId.data(using: .utf8), permissions: .readable)
    service.characteristics = [characteristic]
    self.manager?.add(service)
  }
  
  private func createBluetoothManager() {
    self.manager = CBPeripheralManager(delegate: self, queue: nil)
  }
  
  func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
    print("peripheralManagerDidUpdateState")
    switch (peripheral.state) {
    case .poweredOn:
      print("poweredOn")
      self.createBluetoothService()
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
  
  func peripheralManager(_ peripheral: CBPeripheralManager, didAdd service: CBService, error: Error?) {
    let advertisingData: [String: Any] = [
        CBAdvertisementDataServiceUUIDsKey: [kAdvertisingServiceUUID]
    ]
    self.manager?.stopAdvertising()
    self.manager?.startAdvertising(advertisingData)
  }

  func peripheralManagerDidStartAdvertising(_ peripheral: CBPeripheralManager, error: Error?) {
    print("peripheralManagerDidStartAdvertising \(error)")
  }
}

extension BTLEAdvertisingService : AdvertisingServiceProtocol {
  func start(completionFn: (Bool) -> Void) {
    if self.manager == nil {
      self.createBluetoothManager()
    }
    completionFn(true)
  }
  
  func stop(completionFn: (Bool) -> Void) {
    completionFn(true)
  }
}
