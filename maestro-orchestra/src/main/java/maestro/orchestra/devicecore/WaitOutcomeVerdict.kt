package maestro.orchestra.devicecore

import dev.mobile.devicecore.prototype.api.ActionEvidence
import dev.mobile.devicecore.prototype.api.Outcome
import maestro.MaestroException

/**
 * Reads an assertVisible verdict off a waited [ActionEvidence]'s [Outcome] — device-core's own
 * answer, not Maestro-side geometry. [Outcome.Acted] is a pass (null); [Outcome.Absent] and
 * [Outcome.Blocked] are a false verdict ([MaestroException.AssertionFailure]); [Outcome.Crashed]
 * is an [MaestroException.AppCrash]. Mirrors [DeviceCoreErrorMapper.tapOutcomeToException] but
 * Absent is a not-visible assertion failure here, not ElementNotFound.
 */
object WaitOutcomeVerdict {
    fun toException(evidence: ActionEvidence, selectorDesc: String, timeoutMs: Long): MaestroException? =
        when (val o = evidence.outcome) {
            is Outcome.Acted -> null
            is Outcome.Absent -> MaestroException.AssertionFailure(
                message = "Assertion is false: $selectorDesc is not visible within ${timeoutMs}ms",
                debugMessage = "device-core waitFor Absent (${o.via}, cap=${o.capMs}ms) for $selectorDesc",
            )
            is Outcome.Blocked -> MaestroException.AssertionFailure(
                message = "Assertion is false: $selectorDesc is not visible within ${timeoutMs}ms",
                debugMessage = "device-core waitFor Blocked for $selectorDesc: ${o.detail} " +
                    "(visible=${evidence.actionability.visible.value}, " +
                    "source=${evidence.actionability.visible.source})",
            )
            is Outcome.Crashed -> MaestroException.AppCrash(
                "App ${o.appId} crashed during assertVisible on $selectorDesc"
            )
        }

    /**
     * The INVERTED reading, for `assertNotVisible` over device-core's `Locator.waitUntilGone`. The
     * arms are the same envelope but the pass flips: [Outcome.Acted] is the target GONE (the pass,
     * null), [Outcome.Absent] is the target STILL PRESENT (the not-visible assertion is false), and
     * [Outcome.Blocked] is a present-but-unresolvable read (also not-gone, so a false verdict).
     * device-core spells this out on `Locator.waitUntilGone` — "a successful waitUntilGone returns
     * Acted, and Outcome.Absent names a target that is still present." Do NOT reuse [toException]
     * here: there Absent is the pass-adjacent "gone" arm; the meaning of Absent is opposite.
     */
    fun goneToException(evidence: ActionEvidence, selectorDesc: String, timeoutMs: Long): MaestroException? =
        when (val o = evidence.outcome) {
            is Outcome.Acted -> null
            is Outcome.Absent -> MaestroException.AssertionFailure(
                message = "Assertion is false: $selectorDesc is still visible within ${timeoutMs}ms",
                debugMessage = "device-core waitUntilGone Absent (${o.via}, cap=${o.capMs}ms) — " +
                    "target still present for $selectorDesc",
            )
            is Outcome.Blocked -> MaestroException.AssertionFailure(
                message = "Assertion is false: $selectorDesc is still visible within ${timeoutMs}ms",
                debugMessage = "device-core waitUntilGone Blocked for $selectorDesc: ${o.detail}",
            )
            is Outcome.Crashed -> MaestroException.AppCrash(
                "App ${o.appId} crashed during assertNotVisible on $selectorDesc"
            )
        }
}
