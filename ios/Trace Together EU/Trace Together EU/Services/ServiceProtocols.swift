//
//  ServiceProtocols.swift
//  Trace Together EU
//
//  Created by Daniel Skantze on 2020-04-03.
//  Copyright © 2020 peltarion. All rights reserved.
//

import Foundation

typealias AdvertisingServiceResultCallback = (Bool) -> Void

protocol AdvertisingServiceProtocol {
  func start(completionFn:AdvertisingServiceResultCallback) -> Void
  func stop(completionFn:AdvertisingServiceResultCallback) -> Void
}

extension NSNotification.Name {
    public static let TTScannerServiceDeviceClientIdDiscovered = NSNotification.Name("TTScannerDeviceClientIdDiscovered")
}

let ScannerServiceDeviceIdKey = "clientId"

protocol ScannerServiceProtocol {
  func start()
  func stop()
}
