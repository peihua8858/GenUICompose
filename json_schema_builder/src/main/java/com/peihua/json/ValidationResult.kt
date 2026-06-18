package com.peihua.json

data class AnnotationSet(
    final val evaluatedKeys: Set<String>,
    final val evaluatedItems: Set<Int>,
) {
    companion object {
        fun empty(): AnnotationSet {
            return AnnotationSet(setOf(), setOf())
        }
    }
}

data class ValidationResult(
    final val isValid: Boolean = false,
    final val errors: List<ValidationError>,
    final val annotations: AnnotationSet
) {
    companion object {
        fun failure(errors: List<ValidationError>, annotations: AnnotationSet): ValidationResult {
            return ValidationResult(errors = errors, annotations = annotations)
        }

        fun success(annotations: AnnotationSet): ValidationResult {
            return ValidationResult(annotations = annotations, errors = listOf())
        }
    }
}

