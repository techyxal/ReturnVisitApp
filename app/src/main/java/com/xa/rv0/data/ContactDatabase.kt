package com.xa.rv0.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xa.rv0.viewmodel.Contact

@Database(entities = [Contact::class], version = 2, exportSchema = false) // Increment version to 2
abstract class ContactDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: ContactDatabase? = null

        fun getDatabase(context: Context): ContactDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ContactDatabase::class.java,
                    "contact_database"
                )
                    .addMigrations(MIGRATION_1_2) // Add the migration strategy
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // NEW: Migration strategy from version 1 to 2
        // This is a DESTRUCTIVE MIGRATION for simplicity.
        // In a real application, you would add an ALTER TABLE statement
        // to add the new column without losing existing data.
        // Example non-destructive:
        // val MIGRATION_1_2 = object : Migration(1, 2) {
        //     override fun migrate(database: SupportSQLiteDatabase) {
        //         database.execSQL("ALTER TABLE contacts ADD COLUMN creationTimestamp INTEGER NOT NULL DEFAULT 0")
        //     }
        // }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // This is a destructive migration for demonstration purposes.
                // It drops the old table and recreates it with the new schema.
                // Existing data will be lost.
                database.execSQL("DROP TABLE IF EXISTS contacts")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS contacts (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "phoneNumber TEXT NOT NULL, " +
                            "address TEXT NOT NULL DEFAULT '', " +
                            "latitude REAL NOT NULL DEFAULT 0.0, " +
                            "longitude REAL NOT NULL DEFAULT 0.0, " +
                            "subject TEXT NOT NULL DEFAULT '', " +
                            "callbackDays TEXT NOT NULL DEFAULT '', " +
                            "callbackTime TEXT NOT NULL DEFAULT '', " +
                            "imageUri TEXT, " + // This column already exists from previous steps
                            "creationTimestamp INTEGER NOT NULL DEFAULT 0)" // NEW COLUMN
                )
            }
        }
    }
}
