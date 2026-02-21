package ru.itmo.enterprise.common.mapper

import ru.itmo.enterprise.common.dto.BaseCreateDTO
import ru.itmo.enterprise.common.dto.BaseDTO
import ru.itmo.enterprise.common.dto.BaseUpdateDTO
import ru.itmo.enterprise.common.entity.BaseEntity

interface BaseMapper<E : BaseEntity, D : BaseDTO, C : BaseCreateDTO, U : BaseUpdateDTO> {
    fun toDto(entity: E): D

    fun toDtoList(entities: Collection<E>): List<D>

    fun toEntity(entity: C): E

    fun toDto(createDTO: C): D

    fun toDto(updateDTO: U): D
}
