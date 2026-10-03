package io.github.codingkody99.einkaufsliste.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ShoppingItem::class, CategoryOverride::class, ShoppingList::class, Recipe::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shoppingDao(): ShoppingDao

    abstract fun categoryOverrideDao(): CategoryOverrideDao

    abstract fun shoppingListDao(): ShoppingListDao

    abstract fun recipeDao(): RecipeDao

    companion object {
        private const val NAME = "einkaufsliste.db"

        private const val CREATE_LISTS_TABLE =
            "CREATE TABLE IF NOT EXISTS `shopping_lists` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL)"

        /** Adds the table that remembers hand-picked categories. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `category_overrides` (" +
                        "`name_key` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "PRIMARY KEY(`name_key`))",
                )
            }
        }

        /**
         * Introduces several lists. Everything that exists stays together in the
         * list created here, which keeps its place as the main list.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_LISTS_TABLE)
                db.execSQL(
                    "INSERT INTO `shopping_lists` (`id`, `name`, `position`, `created_at`) " +
                        "VALUES (${ShoppingList.DEFAULT_ID}, '${ShoppingList.DEFAULT_NAME}', 0, " +
                        "${System.currentTimeMillis()})",
                )
                db.execSQL(
                    "ALTER TABLE `shopping_items` ADD COLUMN `list_id` INTEGER NOT NULL " +
                        "DEFAULT ${ShoppingList.DEFAULT_ID}",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_shopping_items_list_id` " +
                        "ON `shopping_items` (`list_id`)",
                )
            }
        }

        /** Adds saved recipes. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recipes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`source_url` TEXT, " +
                        "`ingredients_text` TEXT NOT NULL, " +
                        "`created_at` INTEGER NOT NULL)",
                )
            }
        }

        /** A fresh install has no migration to seed the first list, so do it here. */
        private val seedFirstList = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT INTO `shopping_lists` (`id`, `name`, `position`, `created_at`) " +
                        "VALUES (${ShoppingList.DEFAULT_ID}, '${ShoppingList.DEFAULT_NAME}', 0, " +
                        "${System.currentTimeMillis()})",
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NAME,
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(seedFirstList)
                    .build()
                    .also { instance = it }
            }
    }
}
