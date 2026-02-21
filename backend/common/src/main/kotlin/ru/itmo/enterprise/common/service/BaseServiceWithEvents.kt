package ru.itmo.enterprise.common.service

import ru.itmo.enterprise.common.dto.BaseCreateDTO
import ru.itmo.enterprise.common.dto.BaseDTO
import ru.itmo.enterprise.common.dto.BaseDeleteDTO
import ru.itmo.enterprise.common.dto.BaseUpdateDTO
import ru.itmo.enterprise.common.entity.BaseEntity
import ru.itmo.enterprise.event.sender.EventSender
import ru.itmo.enterprise.stagedchange.dto.ChangeAction

abstract class BaseServiceWithEvents<E : BaseEntity, D : BaseDTO, C : BaseCreateDTO, U : BaseUpdateDTO, L : BaseDeleteDTO> :
    BaseService<E, D, C, U, L>() {

    protected abstract val eventSender: EventSender

    override fun postCreate(entity: E): D {
        val dto = super.postCreate(entity)

        eventSender.sendEvent(
            subject = entity,
            crudAction = ChangeAction.CREATE,
            serviceState = entity.serviceState
        )

        return dto
    }

    override fun postUpdate(entity: E): D {
        val dto = super.postUpdate(entity)

        eventSender.sendEvent(
            subject = entity,
            crudAction = ChangeAction.UPDATE,
            serviceState = entity.serviceState
        )

        return dto
    }

    override fun postDelete(entity: E): L {
        val dto = super.postDelete(entity)

        eventSender.sendEvent(
            subject = entity,
            crudAction = ChangeAction.DELETE,
            serviceState = entity.serviceState
        )

        return dto
    }
}
