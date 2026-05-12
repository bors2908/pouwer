package ge.becrin.pouwer.common.exception

class DataNotFoundException(message: String?) : RuntimeException(message) {
    companion object {
        fun changeType(javaClass: Class<*>) =
            DataNotFoundException("ChangeType for class ${javaClass.simpleName} was not found.")
    }
}
