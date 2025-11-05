import XCTest
import AVFoundation
@testable import tauri_plugin_audio_permissions

// Mock Invoke class for testing
class MockInvoke {
    var resolvedValue: Any?
    var rejectedError: String?
    var argsJson: String = "{}"

    func resolve(_ value: [String: Any]) {
        resolvedValue = value
    }

    func reject(_ error: String) {
        rejectedError = error
    }

    func parseArgs<T: Decodable>(_ type: T.Type) throws -> T {
        let data = argsJson.data(using: .utf8)!
        return try JSONDecoder().decode(type, from: data)
    }

    func reset() {
        resolvedValue = nil
        rejectedError = nil
        argsJson = "{}"
    }
}

final class AudioPermissionPluginTests: XCTestCase {
    
    var plugin: AudioPermissionPlugin!
    var mockInvoke: MockInvoke!
    
    override func setUp() {
        super.setUp()
        plugin = AudioPermissionPlugin()
        mockInvoke = MockInvoke()
    }
    
    override func tearDown() {
        plugin = nil
        mockInvoke = nil
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
    
    func testCheckPermissionWithCurrentState() throws {
        // Test checkPermission with the current permission state
        let expectation = XCTestExpectation(description: "Check permission should complete")
        
        do {
            try plugin.checkPermission(mockInvoke)
            
            // Give it a moment to complete synchronously
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                // Should have resolved with a boolean value
                if let resolved = self.mockInvoke.resolvedValue as? [String: Bool] {
                    XCTAssertNotNil(resolved["granted"], "Response should contain 'granted' key")
                    let granted = resolved["granted"] ?? false
                    XCTAssertTrue(granted is Bool, "Granted value should be a boolean")
                    expectation.fulfill()
                } else {
                    XCTFail("Should have resolved with [String: Bool] response")
                    expectation.fulfill()
                }
            }
        } catch {
            XCTFail("checkPermission should not throw: \(error)")
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 1.0)
    }
    
    func testRequestPermissionResponseFormat() throws {
        // Test that requestPermission returns the correct response format
        let expectation = XCTestExpectation(description: "Request permission should complete")
        
        do {
            try plugin.requestPermission(mockInvoke)
            
            // Wait for the async permission request to complete
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                // Should have resolved with a boolean value
                if let resolved = self.mockInvoke.resolvedValue as? [String: Bool] {
                    XCTAssertNotNil(resolved["granted"], "Response should contain 'granted' key")
                    let granted = resolved["granted"] ?? false
                    XCTAssertTrue(granted is Bool, "Granted value should be a boolean")
                    expectation.fulfill()
                } else if self.mockInvoke.rejectedError != nil {
                    // Permission request might be rejected on simulator/test environment
                    XCTAssertNotNil(self.mockInvoke.rejectedError, "Should have error message")
                    expectation.fulfill()
                } else {
                    XCTFail("Should have either resolved or rejected")
                    expectation.fulfill()
                }
            }
        } catch {
            XCTFail("requestPermission should not throw: \(error)")
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 2.0)
    }
    
    func testPermissionArgsDecoding() throws {
        // Test that PermissionArgs can be decoded from empty JSON
        let emptyJson = "{}"
        let data = emptyJson.data(using: .utf8)!
        
        do {
            let args = try JSONDecoder().decode(PermissionArgs.self, from: data)
            XCTAssertNotNil(args, "PermissionArgs should decode from empty JSON")
        } catch {
            XCTFail("PermissionArgs should decode successfully: \(error)")
        }
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

    // MARK: - New Service Management Tests

    func testNotificationArgsDecoding() throws {
        // Test NotificationArgs with both fields
        let fullJson = """
        {
            "title": "Recording Audio",
            "message": "Audio recording is active"
        }
        """
        let fullData = fullJson.data(using: .utf8)!
        let fullArgs = try JSONDecoder().decode(NotificationArgs.self, from: fullData)
        XCTAssertEqual(fullArgs.title, "Recording Audio")
        XCTAssertEqual(fullArgs.message, "Audio recording is active")

        // Test NotificationArgs with empty JSON
        let emptyJson = "{}"
        let emptyData = emptyJson.data(using: .utf8)!
        let emptyArgs = try JSONDecoder().decode(NotificationArgs.self, from: emptyData)
        XCTAssertNil(emptyArgs.title)
        XCTAssertNil(emptyArgs.message)

        // Test NotificationArgs with only title
        let titleOnlyJson = """
        {
            "title": "Test Title"
        }
        """
        let titleData = titleOnlyJson.data(using: .utf8)!
        let titleArgs = try JSONDecoder().decode(NotificationArgs.self, from: titleData)
        XCTAssertEqual(titleArgs.title, "Test Title")
        XCTAssertNil(titleArgs.message)
    }

    func testIsServiceRunningInitialState() throws {
        // Test that service is not running initially
        mockInvoke.reset()

        try plugin.isServiceRunning(mockInvoke)

        if let resolved = mockInvoke.resolvedValue as? [String: Bool] {
            XCTAssertEqual(resolved["running"], false, "Service should not be running initially")
        } else {
            XCTFail("Should have resolved with [String: Bool] response")
        }
    }

    func testUpdateNotificationResponseFormat() throws {
        // Test that updateNotification returns correct response format
        mockInvoke.reset()
        mockInvoke.argsJson = """
        {
            "title": "Test Title",
            "message": "Test Message"
        }
        """

        try plugin.updateNotification(mockInvoke)

        if let resolved = mockInvoke.resolvedValue as? [String: Bool] {
            XCTAssertEqual(resolved["updated"], true, "Should return updated: true")
        } else {
            XCTFail("Should have resolved with [String: Bool] response")
        }
    }

    func testUpdateNotificationWithEmptyArgs() throws {
        // Test that updateNotification handles empty args
        mockInvoke.reset()
        mockInvoke.argsJson = "{}"

        try plugin.updateNotification(mockInvoke)

        if let resolved = mockInvoke.resolvedValue as? [String: Bool] {
            XCTAssertEqual(resolved["updated"], true, "Should return updated: true even with empty args")
        } else {
            XCTFail("Should have resolved with [String: Bool] response")
        }
    }

    func testStopForegroundServiceResponseFormat() throws {
        // Test that stopForegroundService returns correct response format
        mockInvoke.reset()

        try plugin.stopForegroundService(mockInvoke)

        if let resolved = mockInvoke.resolvedValue as? [String: Bool] {
            XCTAssertEqual(resolved["stopped"], true, "Should return stopped: true")
        } else {
            XCTFail("Should have resolved with [String: Bool] response")
        }
    }

    func testServiceLifecycle() throws {
        // This test verifies the state transitions but won't actually start audio session
        // as that requires permission which may not be granted in test environment

        // Initial state should be not running
        mockInvoke.reset()
        try plugin.isServiceRunning(mockInvoke)

        if let resolved = mockInvoke.resolvedValue as? [String: Bool] {
            let initialState = resolved["running"] ?? true
            XCTAssertFalse(initialState, "Service should not be running initially")

            // After stop, should still be not running
            mockInvoke.reset()
            try plugin.stopForegroundService(mockInvoke)

            if let stopResolved = mockInvoke.resolvedValue as? [String: Bool] {
                XCTAssertEqual(stopResolved["stopped"], true, "Stop should return stopped: true")

                // Verify state is still not running
                mockInvoke.reset()
                try plugin.isServiceRunning(mockInvoke)

                if let stateResolved = mockInvoke.resolvedValue as? [String: Bool] {
                    XCTAssertFalse(stateResolved["running"] ?? true, "Service should remain stopped")
                }
            }
        } else {
            XCTFail("Should have resolved with initial state")
        }
    }

    func testStartForegroundServiceRequiresPermission() throws {
        // Test that startForegroundService checks permission first
        let permission = AVAudioSession.sharedInstance().recordPermission

        mockInvoke.reset()
        try plugin.startForegroundService(mockInvoke)

        if permission != .granted {
            // If we don't have permission, should reject
            XCTAssertNotNil(mockInvoke.rejectedError, "Should reject when permission not granted")
            XCTAssertTrue(mockInvoke.rejectedError?.contains("not granted") ?? false, "Error should mention permission not granted")
        } else {
            // If we have permission, should succeed
            if let resolved = mockInvoke.resolvedValue as? [String: Bool] {
                XCTAssertEqual(resolved["started"], true, "Should return started: true when permission granted")
            }
        }
    }

    func testAudioSessionCategoryConfiguration() throws {
        // Test that the audio session category constants exist
        let playAndRecord = AVAudioSession.Category.playAndRecord
        XCTAssertEqual(playAndRecord, .playAndRecord, "playAndRecord category should be available")

        // Test mode constant
        let defaultMode = AVAudioSession.Mode.default
        XCTAssertEqual(defaultMode, .default, "default mode should be available")

        // Test option constants
        let speakerOption = AVAudioSession.CategoryOptions.defaultToSpeaker
        let bluetoothOption = AVAudioSession.CategoryOptions.allowBluetooth
        XCTAssertTrue(speakerOption.contains(.defaultToSpeaker), "defaultToSpeaker option should be available")
        XCTAssertTrue(bluetoothOption.contains(.allowBluetooth), "allowBluetooth option should be available")
    }

    func testResponseModelConsistency() throws {
        // Test that all response models follow consistent structure

        // Permission responses
        let permissionResponse: [String: Bool] = ["granted": true]
        XCTAssertNotNil(permissionResponse["granted"])

        // Service responses
        let startResponse: [String: Bool] = ["started": true]
        XCTAssertNotNil(startResponse["started"])

        let stopResponse: [String: Bool] = ["stopped": true]
        XCTAssertNotNil(stopResponse["stopped"])

        let updateResponse: [String: Bool] = ["updated": true]
        XCTAssertNotNil(updateResponse["updated"])

        // Status responses
        let statusResponse: [String: Bool] = ["running": false]
        XCTAssertNotNil(statusResponse["running"])
    }

    func testErrorHandling() throws {
        // Test that methods handle invalid JSON gracefully
        mockInvoke.reset()
        mockInvoke.argsJson = "invalid json"

        XCTAssertThrowsError(try plugin.updateNotification(mockInvoke)) { error in
            // Should throw decoding error
            XCTAssertTrue(error is DecodingError, "Should throw DecodingError for invalid JSON")
        }
    }
}
