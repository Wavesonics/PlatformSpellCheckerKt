//
//  SceneDelegate.swift
//  iosApp
//
//  Scene delegate for modern iOS lifecycle
//

import UIKit
import ExampleApp

class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard let windowScene = (scene as? UIWindowScene) else { return }

        window = UIWindow(windowScene: windowScene)
        window?.rootViewController = MainKt.MainViewController()
        window?.makeKeyAndVisible()
    }
}
