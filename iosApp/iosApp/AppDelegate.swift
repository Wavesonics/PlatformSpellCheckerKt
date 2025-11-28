//
//  AppDelegate.swift
//  iosApp
//
//  App delegate for UIKit-based app
//

import UIKit
import ExampleApp

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {

        window = UIWindow(frame: UIScreen.main.bounds)
        window?.rootViewController = MainKt.MainViewController()
        window?.makeKeyAndVisible()

        return true
    }
}
