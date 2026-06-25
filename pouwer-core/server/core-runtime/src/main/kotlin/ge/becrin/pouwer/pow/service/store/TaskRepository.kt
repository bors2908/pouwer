package ge.becrin.pouwer.pow.service.store

import ge.becrin.pouwer.challenge.api.Task
import java.util.UUID

interface TaskRepository {
    fun save(task: Task)

    fun find(jobId: UUID): Task?

    fun getAndRemove(jobId: UUID): Task?
}
