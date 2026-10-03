package io.github.codingkody99.einkaufsliste.domain

import io.github.codingkody99.einkaufsliste.data.Category

/** One line of pasted text turned into something the list can hold. */
data class ParsedEntry(
    val name: String,
    val quantity: String,
    val category: Category,
    /** False when no vocabulary matched, so the section is only a guess. */
    val recognized: Boolean,
    /** True for dish or day captions like "Abendessen Freitag:". */
    val isHeading: Boolean,
    /** The line it came from, shown in the preview so nothing looks invented. */
    val sourceLine: String,
)
