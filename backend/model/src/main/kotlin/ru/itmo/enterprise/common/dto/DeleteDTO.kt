package ru.itmo.enterprise.common.dto

interface BaseDeleteDTO {
    var deleted: Boolean
}

data class DeleteDTO(override var deleted: Boolean) : BaseDeleteDTO

data class StagedDeleteDTO(
    override var deleted: Boolean,
    var stagedChangeId: Long?
) : BaseDeleteDTO
