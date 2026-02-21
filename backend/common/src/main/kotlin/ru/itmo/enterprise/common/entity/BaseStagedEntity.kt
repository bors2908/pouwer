package ru.itmo.enterprise.common.entity

import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class BaseStagedEntity : BaseEntity() {
    @Transient
    var stagedChangeId: Long? = null
}
