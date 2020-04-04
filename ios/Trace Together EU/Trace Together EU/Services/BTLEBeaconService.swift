//
//  BTLEBeaconService.swift
//  Trace Together EU
//
//  Created by Daniel Skantze on 2020-04-03.
//  Copyright © 2020 peltarion. All rights reserved.
//

import Foundation

class BTLEAdvertisingService : AdvertisingServiceProtocol {
    func start(completionFn: (Bool) -> Void) {
        completionFn(true)
    }
    
    func stop(completionFn: (Bool) -> Void) {
        completionFn(true)
    }
}
