package io.github.codingkody99.einkaufsliste.data

/**
 * Aisle-style grouping for the list. The declaration order doubles as the sort
 * order on screen, so it roughly follows the route through a supermarket.
 */
enum class Category(val label: String, val emoji: String) {
    OBST_GEMUESE("Obst & Gemüse", "🥕"),
    BACKWAREN("Backwaren", "🥖"),
    MOLKEREI("Molkerei & Eier", "🧈"),
    FLEISCH_FISCH("Fleisch & Fisch", "🐟"),
    TIEFKUEHL("Tiefkühl", "🧊"),
    VORRAT("Vorrat & Trocken", "🍝"),
    GETRAENKE("Getränke", "🧃"),
    HAUSHALT("Haushalt", "🧽"),
    SONSTIGES("Sonstiges", "🛒"),
    ;

    companion object {
        val DEFAULT = SONSTIGES

        /** Parses a stored name, falling back to [DEFAULT] for unknown values. */
        fun fromName(name: String?): Category =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
