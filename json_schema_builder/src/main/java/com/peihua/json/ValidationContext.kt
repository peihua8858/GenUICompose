package com.peihua.json

import com.peihua.json.schema.Schema
import java.net.URI

/// A context object that holds information about the current validation
/// process.
data class ValidationContext(
    val rootSchema: Schema,
    val strictFormat: Boolean = false,
    val sourceUri: URI? = null,
    val schemaRegistry: SchemaRegistry,
    val vocabularies: Map<String, Boolean> = mapOf(
        "https://json-schema.org/draft/2020-12/vocab/core" to true,
        "https://json-schema.org/draft/2020-12/vocab/applicator" to true,
        "https://json-schema.org/draft/2020-12/vocab/unevaluated" to true,
        "https://json-schema.org/draft/2020-12/vocab/validation" to true,
        "https://json-schema.org/draft/2020-12/vocab/meta-data" to true,
        "https://json-schema.org/draft/2020-12/vocab/format-annotation" to true,
        "https://json-schema.org/draft/2020-12/vocab/content" to true,
    ),
    val loggingContext: LoggingContext? = null
) {


    /// Creates a copy of this context with a new [newSourceUri].
    fun withSourceUri(newSourceUri: URI?): ValidationContext {
        return copy(sourceUri = newSourceUri)
    }

    /// Creates a copy of this context with a new set of [newVocabularies].
    fun withVocabularies(newVocabularies: Map<String, Boolean>): ValidationContext {
        return copy(vocabularies = newVocabularies)
    }
}

/// Validates the given [data] against a [schema].
///
/// This is a helper function for recursively validating subschemas.
fun validateSubSchema(
    schema: Any?, data: Any?,
    currentPath: List<String>,
    context: ValidationContext,
    dynamicScope: List<Schema>,
    initialAnnotations: AnnotationSet? = null
): ValidationResult {
    if (schema is Boolean) {
        if (schema == false) {
            return ValidationResult.failure(
                listOf(
                    ValidationError(
                        ValidationErrorType.custom,
                        path = currentPath,
                        details = "Schema is false",
                    ),
                ), AnnotationSet.empty()
            )
        }
        // If schema is true, it's always valid.
        return ValidationResult.success(AnnotationSet.empty())
    }
    if (schema is Map<*, *>) {
        return Schema.fromMap(schema as Map<String, Any?>).validateSchema(
            data,
            currentPath,
            context,
            dynamicScope,
            initialAnnotations = initialAnnotations,
        )
    }
    // This should not happen for a valid schema file.
    return ValidationResult.success(AnnotationSet.empty())
}

/// An extension on [Schema] that adds validation functionality.
/// Validates the given [data] against this schema.
///
/// Returns a list of [ValidationError] if validation fails,
/// or an empty list if validation succeeds.
fun Schema.validate(
    data: Any?,
    strictFormat: Boolean = false,
    sourceUri: URI?,
    schemaRegistry: SchemaRegistry?,
    loggingContext: LoggingContext?,
): List<ValidationError> {
    val registry = schemaRegistry ?: SchemaRegistry(loggingContext = loggingContext)
    var result: ValidationResult?
    try {
        val baseUri = sourceUri ?: URI.create("local://schema")
        registry.addSchema(baseUri, this)
        val context = ValidationContext(
            this,
            strictFormat = strictFormat,
            sourceUri = baseUri,
            schemaRegistry = registry,
            loggingContext = loggingContext,
        )
        result = validateSchema(data, listOf(), context, listOf(this))
    } finally {
        if (schemaRegistry == null) {
            // If we created our own, we need to dispose it.
            registry.dispose()
        }
    }
    return result.errors
}

suspend fun Schema.validateSchema(
    data: Any?,
    currentPath: List<String>,
    context: ValidationContext,
    dynamicScope: List<Schema>,
    initialAnnotations: AnnotationSet? = null
): ValidationResult {
    var currentContext = context;
    val id = id
    val sourceUri = currentContext.sourceUri
    if (id != null) {
        // This is a heuristic to avoid re-resolving a relative path that has
        // already been applied to the base URI.
        if (!(id.endsWith("/") && sourceUri != null &&
                    sourceUri.path.endsWith("/${id}"))
        ) {
            val newUri = context.sourceUri?.resolve(id);
            currentContext = context.withSourceUri(newUri);
        }
    }
    context.loggingContext?.log("Validating ${currentContext.sourceUri}#${currentPath.joinToString("/")} with schema $value");
    val newDynamicScope = dynamicScope.toMutableList().apply {
        add(this@validateSchema)
    }
    val errors = mutableListOf<ValidationError>();
    val allAnnotations = initialAnnotations ?: AnnotationSet.empty();
    if (schema != null) {
        try {
           val metaSchemaUri = URI.create(schema)
            val metaSchema =  currentContext.schemaRegistry.resolve(metaSchemaUri)
        }catch (e: Exception){

        }
    }
    return ValidationResult()
}
