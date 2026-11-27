package ru.haykor.carmanager.core.data.local.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

private const val CREATE_RECORDS_FTS =
    "CREATE VIRTUAL TABLE IF NOT EXISTS `records_fts` USING FTS5(`note`, tokenize=`unicode61`, " +
        "content=`service_records`)"

private const val CREATE_RECORDS_FTS_BEFORE_UPDATE_TRIGGER =
    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_records_fts_BEFORE_UPDATE " +
        "BEFORE UPDATE ON `service_records` BEGIN DELETE FROM `records_fts` " +
        "WHERE `rowid`=OLD.`rowid`; END"

private const val CREATE_RECORDS_FTS_BEFORE_DELETE_TRIGGER =
    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_records_fts_BEFORE_DELETE " +
        "BEFORE DELETE ON `service_records` BEGIN DELETE FROM `records_fts` " +
        "WHERE `rowid`=OLD.`rowid`; END"

private const val CREATE_RECORDS_FTS_AFTER_UPDATE_TRIGGER =
    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_records_fts_AFTER_UPDATE " +
        "AFTER UPDATE ON `service_records` BEGIN INSERT INTO `records_fts`(`rowid`, `note`) " +
        "VALUES (NEW.`rowid`, NEW.`note`); END"

private const val CREATE_RECORDS_FTS_AFTER_INSERT_TRIGGER =
    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_records_fts_AFTER_INSERT " +
        "AFTER INSERT ON `service_records` BEGIN INSERT INTO `records_fts`(`rowid`, `note`) " +
        "VALUES (NEW.`rowid`, NEW.`note`); END"

private const val REBUILD_RECORDS_FTS = "INSERT INTO `records_fts`(`records_fts`) VALUES('rebuild')"

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(CREATE_RECORDS_FTS)
        connection.execSQL(CREATE_RECORDS_FTS_BEFORE_UPDATE_TRIGGER)
        connection.execSQL(CREATE_RECORDS_FTS_BEFORE_DELETE_TRIGGER)
        connection.execSQL(CREATE_RECORDS_FTS_AFTER_UPDATE_TRIGGER)
        connection.execSQL(CREATE_RECORDS_FTS_AFTER_INSERT_TRIGGER)
        connection.execSQL(REBUILD_RECORDS_FTS)
    }
}
