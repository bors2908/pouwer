package ge.becrin.pouwer.pow.service

import ge.becrin.pouwer.challenge.api.Task
import java.util.UUID

interface TaskStore {
    fun save(task: Task)
    fun find(jobId: UUID): Task?
    fun remove(jobId: UUID): Task?
}
