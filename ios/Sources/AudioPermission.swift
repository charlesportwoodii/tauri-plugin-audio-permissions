import Foundation
import AVFoundation

class AudioPermission {
    private let audioSession: AVAudioSession = AVAudioSession.sharedInstance()

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
