import SwiftRs
import Tauri
import UIKit
import WebKit
import AVFoundation

class PermissionArgs: Decodable {
  // No arguments needed for permission requests
}

class AudioPermissionPlugin: Plugin {
  @objc public func requestPermission(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(PermissionArgs.self)
    
    AVAudioSession.sharedInstance().requestRecordPermission { granted in
      DispatchQueue.main.async {
        invoke.resolve(["granted": granted])
      }
    }
  }
  
  @objc public func checkPermission(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(PermissionArgs.self)
    
    let permission = AVAudioSession.sharedInstance().recordPermission
    let granted = permission == .granted
    
    invoke.resolve(["granted": granted])
  }
}

@_cdecl("init_plugin_audio_permissions")
func initPlugin() -> Plugin {
  return AudioPermissionPlugin()
}
