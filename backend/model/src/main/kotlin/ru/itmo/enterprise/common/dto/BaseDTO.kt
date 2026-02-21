package ru.itmo.enterprise.common.dto

import java.time.Instant
import ru.itmo.enterprise.common.entity.ServiceState

interface BaseDTO : SerializableDTO {
    val id: Long?

    val createdAt: Instant?

    val serviceState: ServiceState
}

interface BaseStagedDTO : BaseDTO {
    val stagedChangeId: Long?
}

interface BaseCreateDTO : SerializableDTO

interface BaseUpdateDTO : SerializableDTO {
    val id: Long
}
