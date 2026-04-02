package com.peihua.json

/**
 * The valid types for properties in a JSON schema.
 */
enum class JsonType(
    //The name of the type as it appears in a JSON schema.
    val typeName: String,
) {
    OBJECT("object"),
    LIST("array"),
    STRING("string"),
    NUM("number"),
    INT("integer"),
    BOOLEAN("boolean"),
    NIL("null");
}
