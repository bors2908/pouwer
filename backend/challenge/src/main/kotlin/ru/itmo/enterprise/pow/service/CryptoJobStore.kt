package ru.itmo.enterprise.pow.service

import org.springframework.stereotype.Component
import ru.itmo.enterprise.pow.model.CryptoJob

@Component
class CryptoJobStore {
    @Volatile
    var currentJob: CryptoJob? = null
}
