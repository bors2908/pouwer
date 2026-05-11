package ru.itmo.enterprise.challenge.api

data class NonceRange(
    val start: Long,
    val end: Long
)

data class ChallengePayload(
    val random: ByteArray,
    val expiresAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChallengePayload
        if (!random.contentEquals(other.random)) return false
        if (expiresAt != other.expiresAt) return false
        return true
    }

    override fun hashCode(): Int {
        var result = random.contentHashCode()
        result = 31 * result + expiresAt.hashCode()
        return result
    }
}
