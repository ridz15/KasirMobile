package com.moncake94.pos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        ProductVariationEntity::class,
        ProductVariationOptionEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        ClosingReportEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PosDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "moncake94_pos.db")
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_1_3)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN payment_method TEXT NOT NULL DEFAULT 'TUNAI'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN status TEXT NOT NULL DEFAULT 'SUCCESS'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN void_reason TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN voided_at INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN refunded_at INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN refund_amount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN refund_reason TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS closing_reports (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "report_date INTEGER NOT NULL, " +
                        "printed_at INTEGER NOT NULL, " +
                        "gross_sales INTEGER NOT NULL, " +
                        "void_total INTEGER NOT NULL, " +
                        "refund_total INTEGER NOT NULL, " +
                        "net_sales INTEGER NOT NULL, " +
                        "success_count INTEGER NOT NULL, " +
                        "void_count INTEGER NOT NULL, " +
                        "refund_count INTEGER NOT NULL, " +
                        "tunai_total INTEGER NOT NULL, " +
                        "qris_total INTEGER NOT NULL, " +
                        "item_sold_count INTEGER NOT NULL, " +
                        "best_sellers_text TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_closing_reports_report_date ON closing_reports(report_date)")
            }
        }

        private val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }
    }
}
