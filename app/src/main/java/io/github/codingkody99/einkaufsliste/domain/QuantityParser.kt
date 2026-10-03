package io.github.codingkody99.einkaufsliste.domain

/**
 * Pulls an amount off the front or back of a fragment: "500g Hackfleisch",
 * "2 x Joghurt", "Milch 1 l", "1/2 Gurke".
 *
 * A bare number keeps its place as an amount ("2 Joghurt" → 2 × Joghurt), but a
 * word following the number is only swallowed when it is a known unit — so
 * "2 Joghurt" keeps "Joghurt" as the name.
 */
object QuantityParser {

    /** The amount in display form (empty when there is none) and the rest. */
    fun split(fragment: String): Pair<String, String> {
        leading(fragment)?.let { return it }
        trailing(fragment)?.let { return it }
        return "" to fragment
    }

    /** True for text that is nothing but an amount, like "1 l" or "2 Packungen". */
    fun isPureQuantity(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val match = PURE.matchEntire(trimmed) ?: return false
        val unit = match.groupValues[3]
        return unit.isEmpty() || canonicalUnit(unit) != null
    }

    private fun leading(fragment: String): Pair<String, String>? {
        val match = LEADING.find(fragment) ?: return null
        val amount = match.groupValues[1]
        val times = match.groupValues[2]
        var rest = fragment.substring(match.range.last + 1).trim()

        var unit: String? = null
        FIRST_WORD.find(rest)?.let { word ->
            canonicalUnit(word.groupValues[1])?.let { canonical ->
                unit = canonical
                rest = rest.substring(word.range.last + 1).trim()
            }
        }

        // Nothing left that could be a name: not an amount, just a stray number.
        if (rest.none { it.isLetter() }) return null
        return display(amount, unit, times) to rest
    }

    private fun trailing(fragment: String): Pair<String, String>? {
        val match = TRAILING.find(fragment) ?: return null
        val amount = match.groupValues[1]
        val suffix = match.groupValues[2]
        val unit = if (suffix.lowercase() in TIMES) null else canonicalUnit(suffix) ?: return null
        val times = if (unit == null) "x" else ""
        val rest = fragment.substring(0, match.range.first).trim().trimEnd(',', ';', '-')
        if (rest.none { it.isLetter() }) return null
        return display(amount, unit, times) to rest
    }

    private fun display(amount: String, unit: String?, times: String): String {
        val number = amount.replace('.', ',')
        return when {
            unit != null -> "$number $unit"
            times.isNotEmpty() -> "$number x"
            else -> number
        }
    }

    private fun canonicalUnit(raw: String): String? =
        if (raw.isEmpty()) null else UNITS[raw.lowercase().trimEnd('.')]

    private val TIMES = setOf("x", "×")

    private val UNITS: Map<String, String> = buildMap {
        fun unit(display: String, vararg spellings: String) =
            spellings.forEach { put(it, display) }

        unit("g", "g", "gr", "gramm")
        unit("kg", "kg", "kilo", "kilogramm")
        unit("mg", "mg")
        unit("ml", "ml")
        unit("cl", "cl")
        unit("l", "l", "ltr", "liter")
        unit("Stück", "st", "stk", "stck", "stueck", "stück")
        unit("Packung", "pck", "pkg", "pack", "packung", "packungen")
        unit("Dose", "dose", "dosen")
        unit("Glas", "glas", "gläser", "glaeser")
        unit("Becher", "becher")
        unit("Flasche", "flasche", "flaschen")
        unit("Beutel", "tüte", "tüten", "tueten", "beutel")
        unit("Bund", "bund", "bd")
        unit("Kopf", "kopf", "köpfe")
        unit("Scheiben", "scheibe", "scheiben")
        unit("Zehen", "zehe", "zehen")
        unit("EL", "el")
        unit("TL", "tl")
        unit("Prise", "prise", "prisen")
        unit("Tafel", "tafel", "tafeln")
        unit("Rolle", "rolle", "rollen")
        unit("Kiste", "karton", "kiste", "kasten")
        unit("Portion", "portion", "portionen")
    }

    // A fraction has to be tried before the plain integer, or "1/2" matches "1".
    private const val NUMBER = "(\\d+/\\d+|\\d+(?:[.,]\\d+)?|½|¼|¾)"
    private const val WORD = "([A-Za-zÄÖÜäöüß]+\\.?)"

    private val LEADING = Regex("^$NUMBER\\s*([xX×])?\\s*")
    private val TRAILING = Regex("$NUMBER\\s*$WORD\\s*$")
    private val PURE = Regex("^$NUMBER\\s*([xX×])?\\s*$WORD?\\s*$")
    private val FIRST_WORD = Regex("^$WORD")
}
