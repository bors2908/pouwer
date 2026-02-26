package ru.itmo.enterprise.pow.client.monero.stratum

import org.springframework.stereotype.Component
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicReference

@Component
class StratumJobStore {
    private val latest = AtomicReference<RawStratumJob?>(null)

    // optional difficulty pushed separately by server
    @Volatile
    private var currentDifficulty: BigInteger? = null

    fun setDifficulty(difficulty: BigInteger?) {
        currentDifficulty = difficulty
    }

    fun getDifficulty(): BigInteger? = currentDifficulty

    fun set(job: RawStratumJob) = latest.set(job)
    fun getLatest(): RawStratumJob? = latest.get()
}
