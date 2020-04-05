//
//  DeviceRegistry.swift
//  Trace Together EU
//
//  Created by Daniel Skantze on 2020-04-04.
//  Copyright © 2020 peltarion. All rights reserved.
//

import Foundation
import Combine

enum EntryType : Int16, Codable {
  case appeared = 0
  case disappeared = 1
}

struct DeviceIdEntry : Codable {
  var type:EntryType
  var timestamp:TimeInterval
  var deviceId:Int64
}

class DeviceRegistry {
  var deviceIds = [DeviceIdEntry]()
  var deviceIdSubscriber:AnyCancellable?
  let saveQueue = DispatchQueue(label: "DeviceRegistrySaveQueue")

  init() {
    self.restoreState()
    deviceIdSubscriber = self.createDeviceIdSubscriber()
  }
  
  func createDeviceIdSubscriber() -> AnyCancellable {
    return NotificationCenter.default
      .publisher(for: .TTScannerServiceDeviceClientIdDiscovered)
      .map( { $0.userInfo != nil ? $0.userInfo![ScannerServiceDeviceIdKey] : nil } )
      .sink { (output) in
        guard let deviceId = output as? String else {
          return
        }
        self.addDeviceEntry(deviceId: deviceId)
    }
  }
  
  func addDeviceEntry(deviceId:String) {
    guard let deviceIdInt = Int64(deviceId) else {
      fatalError("Incorrect format of deviceId")
    }
    let entry = DeviceIdEntry(type: .appeared, timestamp: Date().timeIntervalSince1970, deviceId: deviceIdInt)
    self.deviceIds.append(entry)
    let deviceIdsCopy = self.deviceIds
    print("Added entry \(entry)")
    self.saveQueue.async {
      self.saveState(ids:deviceIdsCopy)
    }
  }
}

extension DeviceRegistry {
  var tmpSaveFilePath:URL {
    get {
      return getSaveUrl(filename: "devices.json.tmp")
    }
  }
  var saveFilePath:URL {
    get {
      getSaveUrl(filename: "devices.json")
    }
  }
  var swapSaveFilePath:URL {
    get {
      return getSaveUrl(filename: "devices.json.tmp2")
    }
  }
  func getSaveUrl(filename:String) -> URL {
      let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
      let documentsDirectory = paths[0]
    return documentsDirectory.appendingPathComponent(filename, isDirectory: false)
  }
  
  func saveState(ids:[DeviceIdEntry]) {
    do {
      let data = try JSONEncoder().encode(ids)
      try data.write(to: tmpSaveFilePath)
      try FileManager.default.moveItem(at: tmpSaveFilePath, to: swapSaveFilePath)
      try FileManager.default.removeItem(at: saveFilePath)
      try FileManager.default.moveItem(at: swapSaveFilePath, to: saveFilePath)
    } catch {
      fatalError("Failed to save state \(error)")
    }
  }
  
  func restoreState() {
    var data:Data?
    do {
      data = try Data.init(contentsOf: saveFilePath)
    } catch {
      print("Unable to read file, creating new")
      return
    }
    do {
      self.deviceIds = try JSONDecoder().decode([DeviceIdEntry].self, from: data!)
    } catch {
      fatalError("Failed to restore state \(error)")
    }
  }
}
