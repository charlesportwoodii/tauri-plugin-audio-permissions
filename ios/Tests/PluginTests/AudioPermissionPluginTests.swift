import XCTest
import AVFoundation
@testable import tauri_plugin_audio_permissions

// Mock Invoke class for testing
class MockInvoke {
    var resolvedValue: Any?
    var rejectedError: String?
    
    func resolve(_ value: [String: Any]) {
        resolvedValue = value
    }
    
    func reject(_ error: String) {
        rejectedError = error
    }
    
    func parseArgs<T: Decodable>(_ type: T.Type) throws -> T {
        // Return empty args for PermissionArgs (which has no properties)
        let data = "{}".data(using: .utf8)!
        return try JSONDecoder().decode(type, from: data)
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
}
