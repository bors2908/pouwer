package ge.becrin.pouwer.common.exception

class DataConflictException(message: String?) : RuntimeException(message) {
    companion object {
        fun unique(type: Class<*>, fieldName: String, fieldValue: String, id: Long) =
            ge.becrin.pouwer.common.exception.DataConflictException(
                "${type.simpleName} already exists with $fieldName value of $fieldValue. [id=$id]"
            )
    }
}
