package ru.itmo.enterprise.common.entity

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import java.time.Instant
import kotlin.jvm.Transient

@MappedSuperclass
abstract class BaseEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = Instant.now()
    }

    @Transient
    var serviceState: ServiceState = ServiceState.UNKNOWN
}
