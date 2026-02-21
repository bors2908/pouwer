package ru.itmo.enterprise.common.exception

import ru.itmo.enterprise.common.entity.BaseEntity

class DataNotFoundException(message: String?) : RuntimeException(message) {
    companion object {
        fun entity(entityClass: Class<out BaseEntity>, id: Long) =
            DataNotFoundException("Entity ${entityClass.simpleName} was not found. [id=$id]")

        fun changeType(javaClass: Class<*>) =
            DataNotFoundException("ChangeType for class ${javaClass.simpleName} was not found.")
    }
}
