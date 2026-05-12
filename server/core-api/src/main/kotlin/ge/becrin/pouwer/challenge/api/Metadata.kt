package ge.becrin.pouwer.challenge.api

import java.util.Objects

data class NonceRange(
    val start: Long,
    val end: Long
)

data class ChallengePayload(
    val random: ByteArray,
    val expiresAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ChallengePayload) {
            return false
        }

        return Objects.equals(expiresAt, other.expiresAt)
                && random.contentEquals(other.random)
    }

    override fun hashCode(): Int {
        return expiresAt.hashCode() + random.contentHashCode()
    }
}
