//
// Copyright Amazon.com Inc. or its affiliates.
// All Rights Reserved.
//
// SPDX-License-Identifier: Apache-2.0
//
        
import ClientRuntime
import SmithyTestUtil
import XCTest
@testable import AWSClientRuntime

class UserAgentTests: XCTestCase {
    func testUserAgent() {
        let ua = AWSUserAgentMetadata.fromEnv(apiMetadata: APIMetadata(serviceId: "Test Service", version: "1.2.3"))
        XCTAssert(ua.userAgent == "aws-sdk-swift/1.2.3")
    }
    
    func testXAmzUserAgent() {
        let apiMeta = APIMetadata(serviceId: "Test Service", version: "1.2.3")
        let sdkMeta = SDKMetadata(name: "swift", version: apiMeta.version)
        let osMeta = OSMetadata(family: currentOS, version: "11.4")
        let langMeta = LanguageMetadata(version: "9.9")
        let awsUserAgent = AWSUserAgentMetadata(sdkMetadata: sdkMeta,
                                      apiMetadata: apiMeta,
                                      osMetadata: osMeta,
                                      languageMetadata: langMeta)
        var expected: String = ""
        switch currentOS {
        case .linux:
            expected = "aws-sdk-swift/1.2.3 api/test-service/1.2.3 os/linux/11.4 lang/swift/9.9"
        case .macOS:
            expected = "aws-sdk-swift/1.2.3 api/test-service/1.2.3 os/macOS/11.4 lang/swift/9.9"
        case .iOS:
            expected = "aws-sdk-swift/1.2.3 api/test-service/1.2.3 os/iOS/11.4 lang/swift/9.9"
        case .unknown:
            expected = "aws-sdk-swift/1.2.3 api/test-service/1.2.3 os/unknown/11.4 lang/swift/9.9"
        default:
            XCTFail("Unexpected OS")
        }
        XCTAssert(awsUserAgent.xAmzUserAgent == expected, awsUserAgent.xAmzUserAgent)
    }
}
