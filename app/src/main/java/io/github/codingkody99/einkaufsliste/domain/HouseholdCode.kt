package io.github.codingkody99.einkaufsliste.domain

import kotlin.random.Random

/**
 * The code that joins two phones to the same lists.
 *
 * It doubles as the identifier of the shared data, so it has to be both
 * unguessable and something a person can read off one screen and type into
 * another. Twelve characters out of an alphabet without look-alikes give about
 * 59 bits — far beyond guessing — while staying short enough to dictate.
 *
 * Note what this is not: knowing the code grants access, so it is a shared
 * secret, not a password. Good enough for a household's shopping list, and the
 * reason the code is never shown to anyone who is not already in the household.
 */
object HouseholdCode {

    /** No I, L, O, 0 or 1: those are what people mistype. */
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private const val LENGTH = 12
    private const val GROUP = 4

    fun generate(random: Random = Random.Default): String =
        (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

    /** "abcd efgh-jkmn" and "ABCDEFGHJKMN" are the same code. */
    fun normalize(input: String): String? {
        val stripped = input.uppercase().filter { !it.isWhitespace() && it != '-' }
        if (stripped.length != LENGTH) return null
        if (stripped.any { it !in ALPHABET }) return null
        return stripped
    }

    fun isValid(input: String): Boolean = normalize(input) != null

    /** Groups the code for display: ABCD-EFGH-JKMN. */
    fun format(code: String): String =
        normalize(code)?.chunked(GROUP)?.joinToString("-") ?: code
}
