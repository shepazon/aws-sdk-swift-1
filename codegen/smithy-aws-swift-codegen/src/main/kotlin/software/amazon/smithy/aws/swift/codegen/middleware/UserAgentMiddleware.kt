package software.amazon.smithy.aws.swift.codegen.middleware

import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.swift.codegen.SwiftWriter
import software.amazon.smithy.swift.codegen.integration.MiddlewarePosition
import software.amazon.smithy.swift.codegen.integration.MiddlewareStep
import software.amazon.smithy.swift.codegen.integration.OperationMiddlewareRenderable
import software.amazon.smithy.swift.codegen.integration.ProtocolGenerator

class UserAgentMiddleware : OperationMiddlewareRenderable {

    override val name = "UserAgentMiddleware"

    override val middlewareStep = MiddlewareStep.BUILDSTEP

    override val position = MiddlewarePosition.BEFORE

    override fun render(
        ctx: ProtocolGenerator.GenerationContext,
        writer: SwiftWriter,
        serviceShape: ServiceShape,
        op: OperationShape,
        operationStackName: String
    ) {
        writer.write("$operationStackName.${middlewareStep.stringValue()}.intercept(position: ${position.stringValue()}, middleware: UserAgentMiddleware(${middlewareParamsString(ctx, serviceShape, op)}))")
    }

    override fun middlewareParamsString(
        ctx: ProtocolGenerator.GenerationContext,
        serviceShape: ServiceShape,
        op: OperationShape
    ): String {
        return "metadata: AWSUserAgentMetadata.fromEnv(apiMetadata: APIMetadata(serviceId: serviceName, version: \"${ctx.settings.moduleVersion}\"))"
    }
}
