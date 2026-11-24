package com.ojhdtapp.parabox.protocol.v2

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServiceDescriptor
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls
import io.grpc.kotlin.ServerCalls
import io.grpc.protobuf.lite.ProtoLiteUtils
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Standard gRPC service descriptor & MethodDescriptors for
 * `parabox.extension.v2.ParaboxExtensionService` (100% wire-compatible with
 * `parabox_extension_v2.proto` without requiring the x86_64 protoc-gen-grpc-java binary).
 */
object ParaboxExtensionServiceGrpc {
    const val SERVICE_NAME = "parabox.extension.v2.ParaboxExtensionService"

    private fun <Req : com.google.protobuf.MessageLite, Resp : com.google.protobuf.MessageLite> unaryMethod(
        methodName: String,
        reqDefault: Req,
        respDefault: Resp,
    ): MethodDescriptor<Req, Resp> = MethodDescriptor.newBuilder<Req, Resp>()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, methodName))
        .setSampledToLocalTracing(true)
        .setRequestMarshaller(ProtoLiteUtils.marshaller(reqDefault))
        .setResponseMarshaller(ProtoLiteUtils.marshaller(respDefault))
        .build()

    val getManifestMethod: MethodDescriptor<GetManifestRequest, ExtensionManifest> =
        unaryMethod("GetManifest", GetManifestRequest.getDefaultInstance(), ExtensionManifest.getDefaultInstance())

    val nextInitStepMethod: MethodDescriptor<InitStepRequest, InitStepResponse> =
        unaryMethod("NextInitStep", InitStepRequest.getDefaultInstance(), InitStepResponse.getDefaultInstance())

    val getConfigSchemaMethod: MethodDescriptor<GetConfigSchemaRequest, ConfigSchemaResponse> =
        unaryMethod("GetConfigSchema", GetConfigSchemaRequest.getDefaultInstance(), ConfigSchemaResponse.getDefaultInstance())

    val validateConfigMethod: MethodDescriptor<ValidateConfigRequest, ValidationResult> =
        unaryMethod("ValidateConfig", ValidateConfigRequest.getDefaultInstance(), ValidationResult.getDefaultInstance())

    val executeConfigActionMethod: MethodDescriptor<ExecuteConfigActionRequest, ActionResult> =
        unaryMethod("ExecuteConfigAction", ExecuteConfigActionRequest.getDefaultInstance(), ActionResult.getDefaultInstance())

    val openSessionMethod: MethodDescriptor<HostSessionFrame, ExtensionSessionFrame> =
        MethodDescriptor.newBuilder<HostSessionFrame, ExtensionSessionFrame>()
            .setType(MethodDescriptor.MethodType.BIDI_STREAMING)
            .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "OpenSession"))
            .setSampledToLocalTracing(true)
            .setRequestMarshaller(ProtoLiteUtils.marshaller(HostSessionFrame.getDefaultInstance()))
            .setResponseMarshaller(ProtoLiteUtils.marshaller(ExtensionSessionFrame.getDefaultInstance()))
            .build()

    val sendMessageMethod: MethodDescriptor<SendMessageRequest, ActionResult> =
        unaryMethod("SendMessage", SendMessageRequest.getDefaultInstance(), ActionResult.getDefaultInstance())

    val recallMessageMethod: MethodDescriptor<RecallMessageRequest, ActionResult> =
        unaryMethod("RecallMessage", RecallMessageRequest.getDefaultInstance(), ActionResult.getDefaultInstance())

    val getContactsMethod: MethodDescriptor<SessionScopedRequest, ContactListResponse> =
        unaryMethod("GetContacts", SessionScopedRequest.getDefaultInstance(), ContactListResponse.getDefaultInstance())

    val getChatsMethod: MethodDescriptor<SessionScopedRequest, ChatListResponse> =
        unaryMethod("GetChats", SessionScopedRequest.getDefaultInstance(), ChatListResponse.getDefaultInstance())

    val queryMessageHistoryMethod: MethodDescriptor<QueryHistoryRequest, MessageListResponse> =
        unaryMethod("QueryMessageHistory", QueryHistoryRequest.getDefaultInstance(), MessageListResponse.getDefaultInstance())

    val getGroupBasicInfoMethod: MethodDescriptor<BasicInfoRequest, BasicInfoResponse> =
        unaryMethod("GetGroupBasicInfo", BasicInfoRequest.getDefaultInstance(), BasicInfoResponse.getDefaultInstance())

    val getUserBasicInfoMethod: MethodDescriptor<BasicInfoRequest, BasicInfoResponse> =
        unaryMethod("GetUserBasicInfo", BasicInfoRequest.getDefaultInstance(), BasicInfoResponse.getDefaultInstance())

    val queryForwardNodeMethod: MethodDescriptor<ForwardNodeRequest, ForwardNodeResponse> =
        unaryMethod("QueryForwardNode", ForwardNodeRequest.getDefaultInstance(), ForwardNodeResponse.getDefaultInstance())

    val streamResourceMethod: MethodDescriptor<ResourceStreamRequest, ResourceChunk> =
        MethodDescriptor.newBuilder<ResourceStreamRequest, ResourceChunk>()
            .setType(MethodDescriptor.MethodType.SERVER_STREAMING)
            .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "StreamResource"))
            .setSampledToLocalTracing(true)
            .setRequestMarshaller(ProtoLiteUtils.marshaller(ResourceStreamRequest.getDefaultInstance()))
            .setResponseMarshaller(ProtoLiteUtils.marshaller(ResourceChunk.getDefaultInstance()))
            .build()

    val serviceDescriptor: ServiceDescriptor = ServiceDescriptor.newBuilder(SERVICE_NAME)
        .addMethod(getManifestMethod)
        .addMethod(nextInitStepMethod)
        .addMethod(getConfigSchemaMethod)
        .addMethod(validateConfigMethod)
        .addMethod(executeConfigActionMethod)
        .addMethod(openSessionMethod)
        .addMethod(sendMessageMethod)
        .addMethod(recallMessageMethod)
        .addMethod(getContactsMethod)
        .addMethod(getChatsMethod)
        .addMethod(queryMessageHistoryMethod)
        .addMethod(getGroupBasicInfoMethod)
        .addMethod(getUserBasicInfoMethod)
        .addMethod(queryForwardNodeMethod)
        .addMethod(streamResourceMethod)
        .build()
}

object ParaboxExtensionServiceGrpcKt {
    const val SERVICE_NAME: String = ParaboxExtensionServiceGrpc.SERVICE_NAME

    class ParaboxExtensionServiceCoroutineStub(
        channel: Channel,
        callOptions: CallOptions = CallOptions.DEFAULT,
    ) : AbstractCoroutineStub<ParaboxExtensionServiceCoroutineStub>(channel, callOptions) {

        override fun build(
            channel: Channel,
            callOptions: CallOptions,
        ): ParaboxExtensionServiceCoroutineStub = ParaboxExtensionServiceCoroutineStub(channel, callOptions)

        suspend fun getManifest(
            request: GetManifestRequest,
            headers: Metadata = Metadata(),
        ): ExtensionManifest = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.getManifestMethod, request, callOptions, headers
        )

        suspend fun nextInitStep(
            request: InitStepRequest,
            headers: Metadata = Metadata(),
        ): InitStepResponse = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.nextInitStepMethod, request, callOptions, headers
        )

        suspend fun getConfigSchema(
            request: GetConfigSchemaRequest,
            headers: Metadata = Metadata(),
        ): ConfigSchemaResponse = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.getConfigSchemaMethod, request, callOptions, headers
        )

        suspend fun validateConfig(
            request: ValidateConfigRequest,
            headers: Metadata = Metadata(),
        ): ValidationResult = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.validateConfigMethod, request, callOptions, headers
        )

        suspend fun executeConfigAction(
            request: ExecuteConfigActionRequest,
            headers: Metadata = Metadata(),
        ): ActionResult = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.executeConfigActionMethod, request, callOptions, headers
        )

        fun openSession(
            requests: Flow<HostSessionFrame>,
            headers: Metadata = Metadata(),
        ): Flow<ExtensionSessionFrame> = ClientCalls.bidiStreamingRpc(
            channel, ParaboxExtensionServiceGrpc.openSessionMethod, requests, callOptions, headers
        )

        suspend fun sendMessage(
            request: SendMessageRequest,
            headers: Metadata = Metadata(),
        ): ActionResult = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.sendMessageMethod, request, callOptions, headers
        )

        suspend fun recallMessage(
            request: RecallMessageRequest,
            headers: Metadata = Metadata(),
        ): ActionResult = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.recallMessageMethod, request, callOptions, headers
        )

        suspend fun getContacts(
            request: SessionScopedRequest,
            headers: Metadata = Metadata(),
        ): ContactListResponse = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.getContactsMethod, request, callOptions, headers
        )

        suspend fun getChats(
            request: SessionScopedRequest,
            headers: Metadata = Metadata(),
        ): ChatListResponse = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.getChatsMethod, request, callOptions, headers
        )

        suspend fun queryMessageHistory(
            request: QueryHistoryRequest,
            headers: Metadata = Metadata(),
        ): MessageListResponse = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.queryMessageHistoryMethod, request, callOptions, headers
        )

        suspend fun getGroupBasicInfo(
            request: BasicInfoRequest,
            headers: Metadata = Metadata(),
        ): BasicInfoResponse = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.getGroupBasicInfoMethod, request, callOptions, headers
        )

        suspend fun getUserBasicInfo(
            request: BasicInfoRequest,
            headers: Metadata = Metadata(),
        ): BasicInfoResponse = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.getUserBasicInfoMethod, request, callOptions, headers
        )

        suspend fun queryForwardNode(
            request: ForwardNodeRequest,
            headers: Metadata = Metadata(),
        ): ForwardNodeResponse = ClientCalls.unaryRpc(
            channel, ParaboxExtensionServiceGrpc.queryForwardNodeMethod, request, callOptions, headers
        )

        fun streamResource(
            request: ResourceStreamRequest,
            headers: Metadata = Metadata(),
        ): Flow<ResourceChunk> = ClientCalls.serverStreamingRpc(
            channel, ParaboxExtensionServiceGrpc.streamResourceMethod, request, callOptions, headers
        )
    }

    abstract class ParaboxExtensionServiceCoroutineImplBase(
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
    ) : AbstractCoroutineServerImpl(coroutineContext) {

        open suspend fun getManifest(request: GetManifestRequest): ExtensionManifest =
            throw UnsupportedOperationException("Method GetManifest is unimplemented")

        open suspend fun nextInitStep(request: InitStepRequest): InitStepResponse =
            throw UnsupportedOperationException("Method NextInitStep is unimplemented")

        open suspend fun getConfigSchema(request: GetConfigSchemaRequest): ConfigSchemaResponse =
            throw UnsupportedOperationException("Method GetConfigSchema is unimplemented")

        open suspend fun validateConfig(request: ValidateConfigRequest): ValidationResult =
            throw UnsupportedOperationException("Method ValidateConfig is unimplemented")

        open suspend fun executeConfigAction(request: ExecuteConfigActionRequest): ActionResult =
            throw UnsupportedOperationException("Method ExecuteConfigAction is unimplemented")

        open fun openSession(requests: Flow<HostSessionFrame>): Flow<ExtensionSessionFrame> =
            throw UnsupportedOperationException("Method OpenSession is unimplemented")

        open suspend fun sendMessage(request: SendMessageRequest): ActionResult =
            throw UnsupportedOperationException("Method SendMessage is unimplemented")

        open suspend fun recallMessage(request: RecallMessageRequest): ActionResult =
            throw UnsupportedOperationException("Method RecallMessage is unimplemented")

        open suspend fun getContacts(request: SessionScopedRequest): ContactListResponse =
            throw UnsupportedOperationException("Method GetContacts is unimplemented")

        open suspend fun getChats(request: SessionScopedRequest): ChatListResponse =
            throw UnsupportedOperationException("Method GetChats is unimplemented")

        open suspend fun queryMessageHistory(request: QueryHistoryRequest): MessageListResponse =
            throw UnsupportedOperationException("Method QueryMessageHistory is unimplemented")

        open suspend fun getGroupBasicInfo(request: BasicInfoRequest): BasicInfoResponse =
            throw UnsupportedOperationException("Method GetGroupBasicInfo is unimplemented")

        open suspend fun getUserBasicInfo(request: BasicInfoRequest): BasicInfoResponse =
            throw UnsupportedOperationException("Method GetUserBasicInfo is unimplemented")

        open suspend fun queryForwardNode(request: ForwardNodeRequest): ForwardNodeResponse =
            throw UnsupportedOperationException("Method QueryForwardNode is unimplemented")

        open fun streamResource(request: ResourceStreamRequest): Flow<ResourceChunk> =
            throw UnsupportedOperationException("Method StreamResource is unimplemented")

        final override fun bindService(): ServerServiceDefinition =
            ServerServiceDefinition.builder(ParaboxExtensionServiceGrpc.serviceDescriptor)
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.getManifestMethod,
                        implementation = ::getManifest,
                    )
                )
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.nextInitStepMethod,
                        implementation = ::nextInitStep,
                    )
                )
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.getConfigSchemaMethod,
                        implementation = ::getConfigSchema,
                    )
                )
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.validateConfigMethod,
                        implementation = ::validateConfig,
                    )
                )
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.executeConfigActionMethod,
                        implementation = ::executeConfigAction,
                    )
                )
                .addMethod(
                    ServerCalls.bidiStreamingServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.openSessionMethod,
                        implementation = ::openSession,
                    )
                )
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.sendMessageMethod,
                        implementation = ::sendMessage,
                    )
                )
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.recallMessageMethod,
                        implementation = ::recallMessage,
                    )
                )
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.getContactsMethod,
                        implementation = ::getContacts,
                    )
                )
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.getChatsMethod,
                        implementation = ::getChats,
                    )
                )
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.queryMessageHistoryMethod,
                        implementation = ::queryMessageHistory,
                    )
                )
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.getGroupBasicInfoMethod,
                        implementation = ::getGroupBasicInfo,
                    )
                )
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.getUserBasicInfoMethod,
                        implementation = ::getUserBasicInfo,
                    )
                )
                .addMethod(
                    ServerCalls.unaryServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.queryForwardNodeMethod,
                        implementation = ::queryForwardNode,
                    )
                )
                .addMethod(
                    ServerCalls.serverStreamingServerMethodDefinition(
                        context = this.context,
                        descriptor = ParaboxExtensionServiceGrpc.streamResourceMethod,
                        implementation = ::streamResource,
                    )
                )
                .build()
    }
}
