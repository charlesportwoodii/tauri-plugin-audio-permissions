import SwiftRs
import Tauri
import UIKit
import WebKit
import AVFoundation

class PermissionArgs: Decodable {
  var permissionType: String? = "audio" // Default to audio
}

class NotificationArgs: Decodable {
  var title: String?
  var message: String?
}

class AudioPermissionPlugin: Plugin {
  private let audioSession: AVAudioSession = AVAudioSession.sharedInstance()
  private var isSessionActive: Bool = false

  @objc public func requestPermission(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(PermissionArgs.self)
    let permissionType = args.permissionType ?? "audio"

    switch permissionType.lowercased() {
    case "notification":
      // iOS doesn't require POST_NOTIFICATIONS permission like Android 13+
      // Notifications are handled automatically by the system for audio sessions
      // Return granted for API consistency
      invoke.resolve(["granted": true])
    default:
      // Audio permission - properly async using completion handler
      // The invoke callback is only resolved when the user responds to the permission dialog
      audioSession.requestRecordPermission { granted in
        DispatchQueue.main.async {
          invoke.resolve(["granted": granted])
        }
      }
    }
  }

  @objc public func checkPermission(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(PermissionArgs.self)

    let permission = audioSession.recordPermission
    let granted = permission == .granted

    invoke.resolve(["granted": granted])
  }

  @objc public func startForegroundService(_ invoke: Invoke) throws {
    // Check if we have permission first
    guard audioSession.recordPermission == .granted else {
      invoke.reject("Audio permission not granted")
      return
    }

    do {
      // Configure audio session for background recording
      try audioSession.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker, .allowBluetooth])
      try audioSession.setActive(true)

      isSessionActive = true
      invoke.resolve(["started": true])
    } catch {
      invoke.reject("Failed to start audio session: \(error.localizedDescription)")
    }
  }

  @objc public func stopForegroundService(_ invoke: Invoke) throws {
    do {
      try audioSession.setActive(false, options: .notifyOthersOnDeactivation)
      isSessionActive = false
      invoke.resolve(["stopped": true])
    } catch {
      invoke.reject("Failed to stop audio session: \(error.localizedDescription)")
    }
  }

  @objc public func updateNotification(_ invoke: Invoke) throws {
    // iOS automatically displays audio session in Control Center
    // Parse args for API compatibility but no manual notification needed
    let _ = try invoke.parseArgs(NotificationArgs.self)
    invoke.resolve(["updated": true])
  }

  @objc public func isServiceRunning(_ invoke: Invoke) throws {
    invoke.resolve(["running": isSessionActive])
  }
}

@_cdecl("init_plugin_audio_permissions")
func initPlugin() -> Plugin {
  return AudioPermissionPlugin()
}
