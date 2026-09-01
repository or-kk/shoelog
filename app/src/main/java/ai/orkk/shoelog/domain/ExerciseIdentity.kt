package ai.orkk.shoelog.domain

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

object ExerciseIdentity {
    fun fallbackKey(
        originPackage: String,
        start: Instant,
        end: Instant,
        type: Int,
    ): String {
        val canonical = listOf(
            originPackage,
            start.toEpochMilli().toString(),
            end.toEpochMilli().toString(),
            type.toString(),
        ).joinToString("|")
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}
