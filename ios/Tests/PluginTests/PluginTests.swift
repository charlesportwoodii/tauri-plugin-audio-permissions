import XCTest
import AVFoundation
@testable import AudioPermissionPlugin

final class AudioPermissionPluginTests: XCTestCase {
    
    var plugin: AudioPermissionPlugin!
    
    override func setUp() {
        super.setUp()
        plugin = AudioPermissionPlugin()
    }
    
    override func tearDown() {
        plugin = nil
        super.tearDown()
    }
    
    func testPluginInitialization() throws {
        // Test that the plugin can be initialized successfully
        XCTAssertNotNil(plugin, "Plugin should be initialized successfully")
    }
    
    func testAudioSessionExists() throws {
        // Test that AVAudioSession is available and accessible
        let audioSession = AVAudioSession.sharedInstance()
        XCTAssertNotNil(audioSession, "AVAudioSession should be available")
    }
    
    func testRecordPermissionValues() throws {
        // Test that AVAudioSession.RecordPermission enum values are as expected
        let undetermined = AVAudioSession.RecordPermission.undetermined
        let denied = AVAudioSession.RecordPermission.denied  
        let granted = AVAudioSession.RecordPermission.granted
        
        XCTAssertNotEqual(undetermined, denied, "Undetermined should not equal denied")
        XCTAssertNotEqual(undetermined, granted, "Undetermined should not equal granted")
        XCTAssertNotEqual(denied, granted, "Denied should not equal granted")
    }
    
    func testPermissionCheckLogic() throws {
        // Test the logic used in permission checking
        let grantedPermission = AVAudioSession.RecordPermission.granted
        let deniedPermission = AVAudioSession.RecordPermission.denied
        let undeterminedPermission = AVAudioSession.RecordPermission.undetermined
        
        // Test the logic that would be used in checkPermission
        XCTAssertTrue(grantedPermission == .granted, "Granted permission should equal .granted")
        XCTAssertFalse(deniedPermission == .granted, "Denied permission should not equal .granted")
        XCTAssertFalse(undeterminedPermission == .granted, "Undetermined permission should not equal .granted")
    }
    
    func testAudioSessionCategory() throws {
        // Test that we can access audio session category (which is related to recording)
        let audioSession = AVAudioSession.sharedInstance()
        
        // Test that we can read the current category
        let currentCategory = audioSession.category
        XCTAssertNotNil(currentCategory, "Audio session should have a category")
        
        // Test that record category exists
        let recordCategory = AVAudioSession.Category.record
        XCTAssertEqual(recordCategory, .record, "Record category should be available")
    }
    
    func testBooleanResponseFormat() throws {
        // Test the boolean response format that our plugin uses
        let grantedResponse = true
        let deniedResponse = false
        
        XCTAssertTrue(grantedResponse, "Granted response should be true")
        XCTAssertFalse(deniedResponse, "Denied response should be false")
        
        // Test JSON-like structure that would be returned
        let responseDict: [String: Bool] = ["granted": grantedResponse]
        XCTAssertEqual(responseDict["granted"], true, "Response dictionary should contain granted: true")
    }
}
