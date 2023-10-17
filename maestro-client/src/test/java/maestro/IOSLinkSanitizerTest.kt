package maestro

import com.google.common.truth.Truth.assertThat
import maestro.utils.StringUtils.toSanitizedIOSLink
import org.junit.jupiter.api.Test

class IOSLinkSanitizerTest {

    @Test
    fun `sanitize the question mark if present in the link`() {
        // given
        val link = "testapp://testpage?testparam={TEST:\"test\"}"

        // when
        val sanitizedLink = link.toSanitizedIOSLink()

        // then
        assertThat(sanitizedLink).isEqualTo(
            "testapp://testpage\\?testparam={TEST:\"test\"}"
        )
    }

    @Test
    fun `sanitize the normal links without side effect`() {
        // given
        val link = "https://google.com"

        // when
        val sanitizedLink = link.toSanitizedIOSLink()

        // then
        assertThat(sanitizedLink).isEqualTo(link)
    }
}