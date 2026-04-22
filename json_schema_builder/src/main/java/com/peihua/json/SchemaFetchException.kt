package com.peihua.json

import java.net.URI

class SchemaFetchException(val uri: URI, override val cause: Throwable? = null) : Exception() {
    constructor(uri: URI, message: String) : this(uri, Exception(message))
    override fun toString(): String {
        var message = "Error fetching remote schema from $uri";
        if (cause != null) {
            message = "$message: $cause";
        }
        return message;
    }
}
