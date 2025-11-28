import Foundation
import AVFoundation

class AudioPermission {
    // Lazy initialization - only access AVAudioSession when actually needed
    private var audioSession: AVAudioSession {
        return AVAudioSession.sharedInstance()
    }

    func checkPermission() -> Bool {
        let permission = audioSession.recordPermission
        let granted = permission == .granted
        return granted
    }

    func requestPermission(completion: @escaping (Bool) -> Void) {
        audioSession.requestRecordPermission { granted in
            DispatchQueue.main.async {
                completion(granted)
            }
        }
    }
}
