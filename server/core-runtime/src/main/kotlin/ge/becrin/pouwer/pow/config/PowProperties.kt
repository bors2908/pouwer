package ge.becrin.pouwer.pow.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("pow")
data class PowProperties(
    val difficultyBits: Int = 20,
    val ttlSeconds: Long = 60,
    val hmacSecret: String
)
