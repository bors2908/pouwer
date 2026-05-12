package ge.becrin.pouwer.common.dto

import java.time.Instant
import ge.becrin.pouwer.common.entity.ServiceState

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
