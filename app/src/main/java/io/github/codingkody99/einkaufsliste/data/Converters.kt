package io.github.codingkody99.einkaufsliste.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromCategory(category: Category): String = category.name

    @TypeConverter
    fun toCategory(name: String?): Category = Category.fromName(name)
}
