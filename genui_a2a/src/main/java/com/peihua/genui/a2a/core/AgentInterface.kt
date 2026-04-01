package com.peihua.genui.a2a.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supported A2A transport protocols. */
@Serializable
enum class TransportProtocol {
    /** JSON-RPC 2.0 over HTTP.
     **/
    @SerialName("JSONRPC")
    JSONRPC,

    /** gRPC over HTTP/2.
     **/
    @SerialName("GRPC")
    GRPC,

    /** REST-style HTTP with JSON.
     * */
    @SerialName("HTTP+JSON")
    HTTP_JSON;
}

@Serializable
class AgentInterface(val url: String, val transport: TransportProtocol)