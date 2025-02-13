// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0.
import ClientRuntime

// TODO: this struct is unfinished. proper endpoint resolving will need to be added futuristically
public struct EndpointResolverMiddleware<OperationStackOutput: HttpResponseBinding,
                                         OperationStackError: HttpResponseBinding>: Middleware {
    
    public let id: String = "EndpointResolver"
    public let endpointResolver: EndpointResolver
    public let serviceId: String
    
    public init(endpointResolver: EndpointResolver, serviceId: String) {
        self.endpointResolver = endpointResolver
        self.serviceId = serviceId
    }
    
    public func handle<H>(context: Context,
                          input: SdkHttpRequestBuilder,
                          next: H) -> Result<OperationOutput<OperationStackOutput>, MError>
    where H: Handler,
    Self.MInput == H.Input,
    Self.MOutput == H.Output,
    Self.Context == H.Context,
    Self.MError == H.MiddlewareError {
        
        guard let region = context.getRegion(), !region.isEmpty else {
            return .failure(.client(ClientError.unknownError(("Region is unable to be resolved"))))
        }
        
        do {
            let awsEndpoint = try endpointResolver.resolve(serviceId: serviceId,
                                                           region: region)
            let host = "\(context.getHostPrefix() ?? "")\(awsEndpoint.endpoint.host)"
            
            if let protocolType = awsEndpoint.endpoint.protocolType {
                input.withProtocol(protocolType)
            }
            
            var updatedContext = context
            
            if let signingRegion = awsEndpoint.signingRegion {
                updatedContext.attributes.set(key: HttpContext.signingRegion, value: signingRegion)
            }
            
            if let signingName = awsEndpoint.signingName {
                updatedContext.attributes.set(key: HttpContext.signingName, value: signingName)
            }
            
            input.withMethod(context.getMethod())
                .withHost(host)
                .withPort(awsEndpoint.endpoint.port)
                .withPath(context.getPath())
            // TODO: investigate if this header should be the same host value as the actual host and where this header should be set
                .withHeader(name: "Host", value: host)
            
            return next.handle(context: updatedContext, input: input)
        } catch {
            return .failure(.client(ClientError.unknownError(("Endpoint is unable to be resolved"))))
        }
    }
    
    public typealias MInput = SdkHttpRequestBuilder
    public typealias MOutput = OperationOutput<OperationStackOutput>
    public typealias Context = HttpContext
    public typealias MError = SdkError<OperationStackError>
}
