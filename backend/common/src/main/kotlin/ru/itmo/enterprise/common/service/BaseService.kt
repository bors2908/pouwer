package ru.itmo.enterprise.common.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.JpaRepository
import ru.itmo.enterprise.common.dto.BaseCreateDTO
import ru.itmo.enterprise.common.dto.BaseDTO
import ru.itmo.enterprise.common.dto.BaseDeleteDTO
import ru.itmo.enterprise.common.dto.BaseUpdateDTO
import ru.itmo.enterprise.common.entity.BaseEntity
import ru.itmo.enterprise.common.entity.ServiceState
import ru.itmo.enterprise.common.exception.DataNotFoundException
import ru.itmo.enterprise.common.mapper.BaseMapper
import kotlin.reflect.KClass

abstract class BaseService<E : BaseEntity, D : BaseDTO, C : BaseCreateDTO, U : BaseUpdateDTO, L : BaseDeleteDTO> {
    protected abstract val repository: JpaRepository<E, Long>

    protected abstract val mapper: BaseMapper<E, D, C, U>

    protected abstract val entityClass: KClass<E>

    protected abstract val entityManager: EntityManager

    protected open fun createEntity(dto: C): D {
        return postCreate(create(prepareCreate(dto)))
    }

    protected open fun updateEntity(dto: U): D {
        return postUpdate(update(prepareUpdate(dto)))
    }

    protected open fun deleteEntity(id: Long): L {
        return postDelete(delete(prepareDelete(id)))
    }

    private fun create(it: E): E {
        return if (it.serviceState == ServiceState.PREPARED) {
            repository.save(it)
                .also { s -> s.serviceState = ServiceState.CREATED }
        } else {
            it
        }
    }

    private fun update(it: E): E {
        return if (it.serviceState == ServiceState.PREPARED) {
            repository.save(it)
                .also { u -> u.serviceState = ServiceState.UPDATED }
        } else {
            it
        }
    }

    private fun delete(it: E): E {
        if (it.serviceState == ServiceState.PREPARED) {
            repository.delete(it)

            it.serviceState = ServiceState.DELETED
        }

        return it
    }

    fun findAll(): List<D> {
        return mapper.toDtoList(findAllEntities())
    }

    fun findEntityById(id: Long): E {
        return repository.findById(id)
            .orElseThrow { DataNotFoundException.entity(entityClass.java, id) }
    }

    fun findById(id: Long): D {
        return findEntityById(id)
            .let { mapper.toDto(it) }
    }

    protected open fun findAllEntities(): List<E> {
        return repository.findAll()
    }

    //Override for custom checks and logic.
    protected open fun prepareCreate(dto: C): E {
        log.info { "Creating ${entityClass.simpleName} [dto=$dto]" }

        return mapper.toEntity(dto)
            .also { it.serviceState = ServiceState.PREPARED }
    }

    //Override for custom checks and logic.
    protected open fun prepareUpdate(dto: U): E {
        log.info { "Updating ${entityClass.simpleName} [dto=$dto]" }

        return findEntityById(dto.id)
            //Avoid leaking unflushed changes.
            .also { entityManager.detach(it) }
            .also { it.serviceState = ServiceState.PREPARED }
    }

    //Override for custom checks and logic.
    protected open fun prepareDelete(id: Long): E {
        log.info { "Deleting ${entityClass.simpleName} [id=$id]" }

        return findEntityById(id)
            //Avoid leaking unflushed changes.
            .also { entityManager.detach(it) }
            .also { it.serviceState = ServiceState.PREPARED }
    }

    //Override for custom checks and logic.
    protected open fun postCreate(entity: E): D {
        val dto = mapper.toDto(entity)

        if (dto.serviceState == ServiceState.CREATED) {
            log.info { "Created ${entityClass.simpleName} [dto=$dto]" }
        } else {
            log.info { "Could not create ${entityClass.simpleName} [dto=$dto]" }
        }

        return dto
    }

    //Override for custom checks and logic.
    protected open fun postUpdate(entity: E): D {
        val dto = mapper.toDto(entity)

        if (dto.serviceState == ServiceState.UPDATED) {
            log.info { "Updated ${entityClass.simpleName} [dto=$dto]" }
        } else {
            log.info { "Could not update ${entityClass.simpleName} [dto=$dto]" }
        }

        return dto
    }

    protected abstract fun getDeleteDTO(): L

    //Override for custom checks and logic.
    protected open fun postDelete(entity: E): L {
        val dto = getDeleteDTO()

        if (entity.serviceState == ServiceState.DELETED) {
            log.info { "Deleted ${entityClass.simpleName} [id=${entity.id}]" }

            dto.deleted = true
        } else {
            log.info { "Could not delete ${entityClass.simpleName} [id=${entity.id}]" }

            dto.deleted = false
        }

        return dto
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
