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
  @State var password: String = ""
  @State var myId: String = ""
  
  public struct CustomTextFieldStyle : TextFieldStyle {
      public func _body(configuration: TextField<Self._Label>) -> some View {
          configuration
            .font(Font.system(size: 20.0)) // set the inner Text Field Font
              .padding(12) // Set the inner Text Field Padding
              //Give it some style
              .background(
                  RoundedRectangle(cornerRadius: 5)
                      .strokeBorder(Color.primary.opacity(0.5), lineWidth: 1))
      }
  }
  
  var body: some View {
    TabView(selection: $selection) {
      NavigationView {
        VStack {
          Spacer(minLength: 100.0)
          VStack(alignment: .center, spacing: 10.0) {
            Spacer(minLength: 20.0)
            SecureField("Case password", text: $password)
              .textFieldStyle(CustomTextFieldStyle())
              .multilineTextAlignment(.center)
              .padding(.horizontal, 20.0)
  //            .frame(minWidth: 150.0, maxWidth: 200.0, minHeight: 64.0, idealHeight: 64.0, maxHeight: 64.0, alignment: .center)
  //            .padding(20)
            Spacer(minLength: 10.0)
            Button(action: {
              // Closure will be called once user taps your button
              print(self.$password)
            }) {
              Text("CONFIRM CASE")
            }
            .frame(minWidth: 0.0, maxWidth: 140.0)
            .padding()
            .foregroundColor(.white)
            .background(Color(red: 1.0, green: 0.4, blue: 0.4))
            .cornerRadius(40)
            .padding(.horizontal, 10)
            Spacer(minLength: 20.0)
          }
          Spacer(minLength: 5.0)
          HStack(alignment: .center, spacing: 10.0) {
            Spacer(minLength: 10.0)
            TextField("My ID: 3", text: $myId)
              .multilineTextAlignment(.center)
              .frame(minWidth: 40.0, maxWidth: 150.0)
              .foregroundColor(.white)
            Spacer(minLength: 10.0)
          }
          Spacer(minLength: 300.0)
        }
        .navigationBarTitle(Text("Trace Together"))
      }
      .tabItem {
        VStack {
          Image("first")
          Text("Track")
        }
      }
      .tag(0)
      TextField("Device token", text: $deviceToken)
        .multilineTextAlignment(.center)
        .onReceive(NotificationCenter.default.publisher(for: .TTRegisteredForPushNotifications), perform: { (output) in
          guard let deviceToken = output.userInfo?["deviceToken"] as? String else {
            return
          }
          self.deviceToken = deviceToken
        })
        .font(.title)
        .tabItem {
          VStack {
            Image("second")
            Text("Debug")
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
