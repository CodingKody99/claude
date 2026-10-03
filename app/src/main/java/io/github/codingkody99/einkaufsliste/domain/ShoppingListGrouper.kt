package io.github.codingkody99.einkaufsliste.domain

import io.github.codingkody99.einkaufsliste.data.ShoppingItem

/**
 * Turns a flat list of items into the rows shown on screen: open items grouped
 * by category (in [io.github.codingkody99.einkaufsliste.data.Category]
 * declaration order), then one "erledigt" section with everything checked off.
 *
 * Kept free of Android and Room so the ordering rules can be unit tested.
 */
object ShoppingListGrouper {

    fun group(items: List<ShoppingItem>): List<ShoppingListRow> {
        val (checked, open) = items.partition { it.isChecked }
        val rows = mutableListOf<ShoppingListRow>()

        open.groupBy { it.category }
            .toSortedMap(compareBy { it.ordinal })
            .forEach { (category, entries) ->
                rows += ShoppingListRow.CategoryHeader(category, entries.size)
                entries.sortedWith(compareBy({ it.createdAt }, { it.id }))
                    .forEach { rows += ShoppingListRow.Entry(it) }
            }

        if (checked.isNotEmpty()) {
            rows += ShoppingListRow.DoneHeader(checked.size)
            checked.sortedWith(compareBy({ it.createdAt }, { it.id }))
                .forEach { rows += ShoppingListRow.Entry(it) }
        }

        return rows
    }
}
