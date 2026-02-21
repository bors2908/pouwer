package ru.itmo.enterprise.common.exception

class DataConflictException(message: String?) : RuntimeException(message) {
    companion object {
        fun unique(type: Class<*>, fieldName: String, fieldValue: String, id: Long) =
            ru.itmo.enterprise.common.exception.DataConflictException(
                "${type.simpleName} already exists with $fieldName value of $fieldValue. [id=$id]"
            )
    }
}
