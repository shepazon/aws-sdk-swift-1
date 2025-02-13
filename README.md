# AWS SDK for Swift

The **AWS SDK for Swift** is a pure Swift SDK for AWS services.

**WARNING: Releases prior to 1.0.0 may contain bugs and no guarantee is made about API stability. They are not intended for use in production!**

## License

This library is licensed under the Apache 2.0 License. 

[![License][apache-badge]][apache-url]

[apache-badge]: https://img.shields.io/badge/License-Apache%202.0-blue.svg
[apache-url]: LICENSE


## Development

You can define a `local.properties` config file at the root of the project to modify build behavior. 

An example config with the various properties is below:

```
# comma separated list of paths to `includeBuild()`
# This is useful for local development of smithy-swift in particular 
compositeProjects=../smithy-swift

# comma separated list of services to exclude from generation from sdk-codegen. When not specified all services are generated
# specify service.VERSION matching the filenames in the models directory `aws-models -> service.VERSION.json`
excludeModels=rds-data.2018-08-01, groundstation.2019-05-23 

# comma separated list of services to generate from sdk-codegen. When not specified all services are generated
# specify service.VERSION matching the filenames in the models directory `aws-models -> service.VERSION.json`.
onlyIncludeModels=lambda.2015-03-31

# when generating aws services build as a standalong project or not (rootProject = true)
buildStandaloneSdk=true
```

**Note:** If a service is specified in both `excludeModels` and `onlyIncludeModels`, it will be excluded from generation. 


##### Building a single service
See the local.properties definition above to specify this in a config file.

```
>> ./gradlew -PonlyIncludeModels=lambda.2015-03-31  :sdk-codegen:build
```

##### Testing Locally
Testing generated services requires `ClientRuntime` of `smithy-swift` and `AWSClientRuntime` Swift packages.

## Alpha SDK Testing Instructions
*Steps*

1. We have all of the AWS SDKs available in our alpha release listed under `/release` in a tagged branch.

We will walk you through how you can use `CognitoIdentity`  as dependency for example in the steps below.  To use it, we will create a test project called TestSdk.

```bash
mkdir TestSdk
cd TestSdk
swift package init --type executable
xed .
```

Once Xcode is open, open Package.swift.  Update the file to mirror the following.  Notice the three following changes:

* Platforms is set to `[.macOS(.v10_15), .iOS(.v13)]`,
* dependencies: - has a .package which references the `AWSCognitoIdentity` package
* the first target “TestSDK” has a dependency listed as `AWSCognitoIdentity`

```swift
// swift-tools-version:5.4
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "TestSdk",
    platforms: [.macOS(.v10_15), .iOS(.v13)],
    dependencies: [
        .package(name: "AWSSwiftSDK", url: "https://github.com/awslabs/aws-sdk-swift", from: "0.0.9"),
    ],
    targets: [
        // Targets are the basic building blocks of a package. A target can define a module or a test suite.
        // Targets can depend on other targets in this package, and on products in packages this package depends on.
        .target(
            name: "TestSdk",
            dependencies: [.product(name: "AWSCognitoIdentity", package: "AWSSwiftSDK")]),
        .testTarget(
            name: "TestSdkTests",
            dependencies: ["TestSdk"]),
    ]
)
```

Then you can open up main.swift, and instantiate CognitoIdentity as follows:

If you are running Swift <5.5:
```swift
import CognitoIdentity

let cognitoIdentityClient = try CognitoIdentityClient(region: "us-east-1")
let cognitoInputCall = CreateIdentityPoolInput(developerProviderName: "com.amazonaws.mytestapplication",
                                               identityPoolName: "identityPoolMadeWithSwiftSDK")

cognitoIdentityClient.createIdentityPool(input: cognitoInputCall) { (result) in
        switch(result) {
        case .success(let output):
        print("\(output)")
        case .failure(let error):
        print("\(error)")
    }
}
```
If you are running 5.5+:
```swift
import CognitoIdentity

func createIdentityPool() async throws -> CreateIdentityPoolOutputResponse {
    let cognitoIdentityClient = try CognitoIdentityClient(region: "us-east-1")
    let cognitoInputCall = CreateIdentityPoolInput(developerProviderName: "com.amazonaws.mytestapplication",
                                                    identityPoolName: "identityPoolMadeWithSwiftSDK")
    
    let result = try await cognitoIdentityClient.createIdentityPool(input: cognitoInputCall)
    return result
}
```

As a result, you should be able to:

    1. Log into your AWS console, go to us-east-1 (we default region to us-east-1 but you can pass in a region, no region resolver yet)
    2. Click on cognito
    3. click on cognito identity pools
    4. Verify that you see the newly created identity pool name: identityPoolMadeWithSwiftSDK


If you’ve made it this far... Congrats!🎉

*What’s next?*
Try some other calls?  Help us better understand what you think the most critical features are next.  Run into any bugs? Give us feedback on the call-site interface. etc...

## Logging
The AWS SDK for Swift uses SwiftLog for high performant logging.  Many of our calls are issued to the `debug` level of output, which are disabled in the console by default.  To see debug output to your console, you can add the following code to your application in a place where you know that the code will be called once and only once:
```swift
import ClientRuntime
SDKLoggingSystem.initialize(logLevel: .debug)
```

Alternatively, if you need finer grain control of instances of SwiftLog, you can call `SDKLoggingSystem.add` to control specific instances of the log handler.  For example:
```swift
import ClientRuntime

SDKLoggingSystem.add(logHandlerFactory: S3ClientLogHandlerFactory(logLevel: .debug))
SDKLoggingSystem.add(logHandlerFactory: CRTClientEngineLogHandlerFactory(logLevel: .info))
SDKLoggingSystem.initialize()
```