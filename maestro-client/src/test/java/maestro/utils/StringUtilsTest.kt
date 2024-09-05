package maestro.utils

import com.google.common.truth.Truth.assertThat
import maestro.utils.StringUtils.evalTruthness
import maestro.utils.StringUtils.toRegexSafe
import org.junit.jupiter.api.Test

internal class StringUtilsTest {

    @Test
    internal fun `toRegexSafe should escape string if regex is invalid`() {
        // Given
        val input = "*Oświadczam, że zapoznałem się z treścią Regulaminu serwisu i akceptuję jego postanowienia."

        // When
        val regex = input.toRegexSafe()

        // Then
        assertThat(regex.matches(input)).isTrue()
    }

    @Test
    fun `eval truthness`() {
        val nullString = null as? String

        listOf(nullString, "", "  ", "0", "NO", "no", "N", "n", "FALSE", "false").forEach {
            assertThat(it.evalTruthness()).isFalse()
        }

        listOf("1", "TRUE", "true", "YES", "yes", "Y", "y").forEach {
            assertThat(it.evalTruthness()).isTrue()
        }

        assertThat("anything".evalTruthness()).isTrue()
    }
}
