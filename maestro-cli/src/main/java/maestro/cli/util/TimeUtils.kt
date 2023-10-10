package maestro.cli.util

import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object TimeUtils {

    fun durationInSeconds(startTime: Long?, endTime: Long?): Duration {
        if (startTime == null || endTime == null) return Duration.ZERO
        return ((endTime - startTime) / 1000f).roundToLong().seconds
    }

    fun durationInSeconds(durationInMillis: Long): Duration {
        return ((durationInMillis) / 1000f).roundToLong().seconds
    }
}