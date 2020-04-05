//
//  AppDelegate.swift
//  Trace Together EU
//
//  Created by Daniel Skantze on 2020-04-03.
//  Copyright © 2020 peltarion. All rights reserved.
//

import UIKit
import GRPC
import SwiftProtobuf
import NIO
import UserNotifications

extension NSNotification.Name {
  public static let TTRegisteredForPushNotifications = NSNotification.Name("TTRegisteredForPushNotifications")
}

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
  
  var advertisingService:AdvertisingServiceProtocol?
  var scannerService:ScannerServiceProtocol?
  var deviceRegistry = DeviceRegistry()
  
  var deviceId:Int64 {
    get {
      var uID = UserDefaults.standard.integer(forKey: "userID")
      assert(Int.max == Int64.max , "Need 64 bit platform to run")
      while uID == 0 {
        uID = Int.random(in: 0 ..< Int.max)
        UserDefaults.standard.set(uID, forKey: "userID")
      }
      return Int64(uID)
    }
  }
  
  func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    // Override point for customization after application launch.
    advertisingService = BTLEAdvertisingService(deviceId: "\(deviceId)", serviceName: "Trace Together EU")
    scannerService = BTLEScannerService()
    scannerService!.start()
    advertisingService!.start { (success) in
      print("Service started \(success)")
      registerForPushNotifications()
    }
    DispatchQueue.global(qos: .background).async {
      self.testRegisterGRPC()
    }
    return true
  }
  
  func testRegisterGRPC() {
    let configuration = ClientConnection.Configuration(target: .hostAndPort("34.91.100.207", 80), eventLoopGroup: MultiThreadedEventLoopGroup(numberOfThreads: 1))
    let connection = ClientConnection(configuration: configuration)
    let grpc = Com_Peltarion_Tracetogether_CaseNotifierServiceClient.init(channel: connection)
    //    let cases = Com_Peltarion_Tracetogether_PotentialCases.init()
    //    let
    //    cases.potentialCases.append(s)
    //    grpc.confirmedCase(cases)
    
    let registerCall = grpc.register(SwiftProtobuf.Google_Protobuf_Empty())
    do {
      let response = try registerCall.response.wait()
      print("ID received: \(response.id)")
    } catch {
      print("GRPC failed \(error)")
    }
  }
  
  // MARK: UISceneSession Lifecycle
  
  func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
    // Called when a new scene session is being created.
    // Use this method to select a configuration to create the new scene with.
    return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
  }
  
  func application(_ application: UIApplication, didDiscardSceneSessions sceneSessions: Set<UISceneSession>) {
    // Called when the user discards a scene session.
    // If any sessions were discarded while the application was not running, this will be called shortly after application:didFinishLaunchingWithOptions.
    // Use this method to release any resources that were specific to the discarded scenes, as they will not return.
  }
  
  func application(
    _ application: UIApplication,
    didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
  ) {
    let tokenParts = deviceToken.map { data in String(format: "%02.2hhx", data) }
    let token = tokenParts.joined()
    NotificationCenter.default.post(name: .TTRegisteredForPushNotifications, object: nil, userInfo: ["deviceToken": token])
    print("Device Token: \(token)")
  }
  
  func application(
    _ application: UIApplication,
    didFailToRegisterForRemoteNotificationsWithError error: Error) {
    print("Failed to register: \(error)")
  }
  
  // MARK: Notification integration
  
  func getNotificationSettings() {
    UNUserNotificationCenter.current().getNotificationSettings { settings in
      print("Notification settings: \(settings)")
      guard settings.authorizationStatus == .authorized else { return }
      DispatchQueue.main.async {
        UIApplication.shared.registerForRemoteNotifications()
      }
    }
  }
  
  func registerForPushNotifications() {
    UNUserNotificationCenter.current()
      .requestAuthorization(options: [.alert, .sound, .badge]) {
        [weak self] granted, error in
        
        print("Permission granted: \(granted)")
        guard granted else {
          return
        }
        self?.getNotificationSettings()
    }
  }
}

