//
//  ComposeView.swift
//  iosApp
//
//  Bridge between Compose Multiplatform and SwiftUI
//

import SwiftUI
import ExampleApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: UIViewControllerRepresentableContext<ComposeView>) -> UIViewController {
        return MainKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: UIViewControllerRepresentableContext<ComposeView>) {
        // No updates needed
    }
}
