//
//  ContentView.swift
//  Trace Together EU
//
//  Created by Daniel Skantze on 2020-04-03.
//  Copyright © 2020 peltarion. All rights reserved.
//

import SwiftUI

struct ContentView: View {
  @State private var selection = 0
  @State var deviceToken = "..."
  
  var body: some View {
    TabView(selection: $selection){
      TextField("Device token", text: $deviceToken)
        .onReceive(NotificationCenter.default.publisher(for: .TTRegisteredForPushNotifications), perform: { (output) in
          guard let deviceToken = output.userInfo?["deviceToken"] as? String else {
            return
          }
          self.deviceToken = deviceToken
        })
        .font(.title)
        .tabItem {
          VStack {
            Image("first")
            Text("First")
          }
      }
      .tag(0)
      Text("Second View")
        .font(.title)
        .tabItem {
          VStack {
            Image("second")
            Text("Second")
          }
      }
      .tag(1)
    }
  }
}

struct ContentView_Previews: PreviewProvider {
  static var previews: some View {
    ContentView()
  }
}
