package io.github.codingkody99.einkaufsliste.data

/**
 * Aisle grouping for the list. The declaration order is the route through the
 * supermarket and therefore the order sections appear on screen, so new
 * categories must be inserted at the right position rather than appended.
 *
 * Values are persisted by [name], not by ordinal, so reordering is safe.
 */
enum class Category(val label: String, val emoji: String) {
    OBST_GEMUESE("Obst & Gemüse", "🥕"),
    BACKWAREN("Backwaren", "🥖"),
    MOLKEREI("Molkerei & Kühlregal", "🧈"),
    FLEISCH_FISCH("Fleisch, Wurst & Fisch", "🥩"),
    TIEFKUEHL("Tiefkühl", "🧊"),
    VORRAT("Vorrat & Konserven", "🍝"),
    SUESS_SNACKS("Süßes & Snacks", "🍫"),
    GETRAENKE("Getränke", "🧃"),
    HAUSHALT("Haushalt & Drogerie", "🧽"),
    SONSTIGES("Sonstiges", "🛒"),
    ;

    companion object {
        val DEFAULT = SONSTIGES

        /** Parses a stored name, falling back to [DEFAULT] for unknown values. */
        fun fromName(name: String?): Category =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
