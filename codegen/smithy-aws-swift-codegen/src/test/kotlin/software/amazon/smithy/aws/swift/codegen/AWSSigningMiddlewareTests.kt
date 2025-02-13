/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.aws.swift.codegen

import io.kotest.matchers.string.shouldContainOnlyOnce
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import software.amazon.smithy.aws.swift.codegen.middleware.AWSSigningMiddleware
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.AuthTrait
import software.amazon.smithy.model.traits.HttpBasicAuthTrait
import software.amazon.smithy.model.traits.OptionalAuthTrait
import software.amazon.smithy.swift.codegen.SwiftWriter

class AWSSigningMiddlewareTests {
    @Test
    fun `service has SigV4Trait and operation has auth trait`() {
        val sigV4Trait = SigV4Trait.builder().name("ExampleService").build()
        val authList = listOf(HttpBasicAuthTrait().toShapeId(), sigV4Trait.toShapeId())

        val serviceShape = ServiceShape.builder()
            .id("com.test#Example")
            .version("1.0")
            .addTrait(sigV4Trait)
            .build()
        val operationShape = OperationShape.builder()
            .id("com.test#ExampleOperation")
            .addTrait(UnsignedPayloadTrait())
            .addTrait(AuthTrait(authList))
            .build()
        val model = Model.builder()
            .addShape(serviceShape)
            .addShape(operationShape)
            .build()

        val hasAuthScheme = AWSSigningMiddleware.hasSigV4AuthScheme(model, serviceShape, operationShape)
        assertTrue(hasAuthScheme)
    }

    @Test
    fun `service has SigV4trait but operation does not have auth`() {
        val serviceShape = ServiceShape.builder()
            .id("com.test#Example")
            .version("1.0")
            .addTrait(SigV4Trait.builder().name("ExampleService").build())
            .build()
        val operationShape = OperationShape.builder()
            .id("com.test#ExampleOperation")
            .addTrait(OptionalAuthTrait())
            .build()
        val model = Model.builder()
            .addShape(serviceShape)
            .addShape(operationShape)
            .build()

        val hasAuthScheme = AWSSigningMiddleware.hasSigV4AuthScheme(model, serviceShape, operationShape)
        assertFalse(hasAuthScheme)
    }

    @Test
    fun `renderSigningMiddleware unsignedBody true`() {
        val expectedContents =
            """
let sigv4Config = AWSClientRuntime.SigV4Config(unsignedBody: true)
stack.finalizeStep.intercept(position: .before, middleware: AWSClientRuntime.SigV4Middleware(config: sigv4Config))"""
        val writer = SwiftWriter("testName")
        val operationShape = OperationShape.builder()
            .id("com.test#ExampleOperation")
            .addTrait(UnsignedPayloadTrait())
            .build()
        val opStackName = "stack"
        val sut = AWSSigningMiddleware()

        sut.render(writer, operationShape, opStackName)

        val contents = writer.toString()
        contents.shouldContainOnlyOnce(expectedContents)
    }

    @Test
    fun `renderSigningMiddleware unsignedBody false`() {
        val expectedContents =
            """
let sigv4Config = AWSClientRuntime.SigV4Config(unsignedBody: false)
stack.finalizeStep.intercept(position: .before, middleware: AWSClientRuntime.SigV4Middleware(config: sigv4Config))"""
        val writer = SwiftWriter("testName")
        val operationShape = OperationShape.builder()
            .id("com.test#ExampleOperation")
            .build()
        val opStackName = "stack"
        val sut = AWSSigningMiddleware()

        sut.render(writer, operationShape, opStackName)

        val contents = writer.toString()
        contents.shouldContainOnlyOnce(expectedContents)
    }

    @Test
    fun `renderSigningMiddleware unsignedBody true, presigner`() {
        val expectedContents =
            """
let sigv4Config = AWSClientRuntime.SigV4Config(expiration: expiration, unsignedBody: true)
stack.finalizeStep.intercept(position: .before, middleware: AWSClientRuntime.SigV4Middleware(config: sigv4Config))"""
        val writer = SwiftWriter("testName")
        val operationShape = OperationShape.builder()
            .id("com.test#ExampleOperation")
            .addTrait(UnsignedPayloadTrait())
            .build()
        val opStackName = "stack"
        val sut = AWSSigningMiddleware {
            "expiration: expiration, unsignedBody: true"
        }

        sut.render(writer, operationShape, opStackName)

        val contents = writer.toString()
        contents.shouldContainOnlyOnce(expectedContents)
    }

    @Test
    fun `renderSigningMiddleware unsignedBody false, presigner`() {
        val expectedContents =
            """
let sigv4Config = AWSClientRuntime.SigV4Config(expiration: expiration, unsignedBody: false)
stack.finalizeStep.intercept(position: .before, middleware: AWSClientRuntime.SigV4Middleware(config: sigv4Config))"""
        val writer = SwiftWriter("testName")
        val operationShape = OperationShape.builder()
            .id("com.test#ExampleOperation")
            .build()
        val opStackName = "stack"
        val sut = AWSSigningMiddleware {
            "expiration: expiration, unsignedBody: false"
        }

        sut.render(writer, operationShape, opStackName)

        val contents = writer.toString()
        contents.shouldContainOnlyOnce(expectedContents)
    }
}
