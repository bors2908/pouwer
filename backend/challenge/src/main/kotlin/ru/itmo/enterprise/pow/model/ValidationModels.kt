package ru.itmo.enterprise.pow.model

data class ValidationResult(
    val status: ValidationStatus,
    val reason: String? = null
)

enum class ValidationStatus {
    ACCEPTED,
    REJECTED,
    CONFLICT
}
