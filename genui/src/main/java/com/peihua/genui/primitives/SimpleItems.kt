package com.peihua.genui.primitives

import kotlin.uuid.Uuid

/// A map of key-value pairs representing a JSON object.
typealias JsonMap = Map<String, Any?>;

/// Key used in schema definition to specify the component ID.
const val surfaceIdKey = "surfaceId";

/// Generates a unique ID (UUID v4).
fun generateId(): String =  Uuid().v4();