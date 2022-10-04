package maestro.orchestra.util

object InputTextRandom {
    private const val CHARSET_TEXT = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
    private const val CHARSET_NUMBER = "0123456789"
    private const val CHARSET_NUMBER_WITHOUT_ZERO = "123456789"
    private val LIST_POPULAR_LAST_NAME = arrayOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez")
    private val LIST_POPULAR_FIST_NAME = arrayOf(
        "Liam",
        "Olivia",
        "Noah",
        "Emma",
        "Oliver",
        "Charlotte",
        "Elijah",
        "Amelia",
        "James",
        "Ava",
        "William",
        "Sophia",
        "Benjamin",
        "Isabella",
        "Lucas",
        "Mia",
        "Henry",
        "Evelyn",
        "Theodore",
        "Harper"
    )
    private val LIST_POPULAR_EMAIL_DOMAIN = arrayOf("gmail.com", "yahoo.com", "hotmail.com", "aol.com", "msn.com", "outlook.com")

    fun randomPersonName() = String.format(
        "%s %s",
        LIST_POPULAR_FIST_NAME.random(), LIST_POPULAR_LAST_NAME.random()
    )

    fun randomEmail() = String.format(
        "%s_%s@%s",
        LIST_POPULAR_FIST_NAME.random().lowercase(), LIST_POPULAR_LAST_NAME.random().lowercase(), LIST_POPULAR_EMAIL_DOMAIN.random(),
    )

    fun getRandomNumber(length: Int): String {
        val randomNum = (1..length)
            .map { CHARSET_NUMBER.random() }
            .joinToString("")
        return if (randomNum.startsWith("0")) {
            CHARSET_NUMBER_WITHOUT_ZERO.random() + randomNum.substring(1)
        } else randomNum
    }

    fun getRandomText(length: Int): String {
        return (1..length)
            .map { CHARSET_TEXT.random() }
            .joinToString("")
    }
}
