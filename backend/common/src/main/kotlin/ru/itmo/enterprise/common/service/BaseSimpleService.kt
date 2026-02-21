package ru.itmo.enterprise.common.service

import ru.itmo.enterprise.common.dto.BaseCreateDTO
import ru.itmo.enterprise.common.dto.BaseDTO
import ru.itmo.enterprise.common.dto.BaseUpdateDTO
import ru.itmo.enterprise.common.dto.DeleteDTO
import ru.itmo.enterprise.common.entity.BaseEntity

abstract class BaseSimpleService<E : BaseEntity, D : BaseDTO, C : BaseCreateDTO, U : BaseUpdateDTO> :
    BaseServiceWithEvents<E, D, C, U, DeleteDTO>() {
    override fun getDeleteDTO(): DeleteDTO {
        return DeleteDTO(false)
    }
}
