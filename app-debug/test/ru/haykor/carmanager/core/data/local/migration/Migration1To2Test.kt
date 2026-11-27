package ru.haykor.carmanager.core.data.local.migration

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.haykor.carmanager.core.data.local.BundledSqliteNative
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Migration1To2Test {

    @Test
    fun indexesServiceRecordNotesAndKeepsTheIndexInSync() = runBlocking {
        BundledSqliteNative.install()
        val databaseFile = Files.createTempFile("app-migration-1-2", ".db")
        try {
            BundledSQLiteDriver().open(databaseFile.toString()).use { connection ->
                connection.execute(
                    """
                    CREATE TABLE IF NOT EXISTS `service_categories` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                connection.execute(
                    """
                    CREATE TABLE IF NOT EXISTS `service_records` (
                        `id` TEXT NOT NULL,
                        `carId` TEXT NOT NULL,
                        `atMileage` INTEGER NOT NULL,
                        `price` INTEGER,
                        `categoryId` TEXT NOT NULL,
                        `note` TEXT,
                        `photoUri` TEXT,
                        `date` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`carId`) REFERENCES `cars`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`categoryId`) REFERENCES `service_categories`(`id`)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                connection.execute(
                    """
                    INSERT INTO `service_records`
                        (`id`, `carId`, `atMileage`, `price`, `categoryId`, `note`, `photoUri`,
                        `date`, `createdAt`, `updatedAt`)
                    VALUES
                        ('record-1', 'car-1', 1000, NULL, 'category-1', 'Engine oil change', NULL, 1, 1, 1)
                    """.trimIndent()
                )

                MIGRATION_1_2.migrate(connection)

                assertEquals(listOf(1L), connection.matches("oil*"))

                connection.execute("UPDATE `service_records` SET `note` = 'Brake pads' WHERE `id` = 'record-1'")

                assertTrue(connection.matches("oil*").isEmpty())
                assertEquals(listOf(1L), connection.matches("brak*"))

                connection.execute(
                    """
                    INSERT INTO `service_records`
                        (`id`, `carId`, `atMileage`, `price`, `categoryId`, `note`, `photoUri`,
                        `date`, `createdAt`, `updatedAt`)
                    VALUES
                        ('record-2', 'car-1', 2000, NULL, 'category-1', 'Tire rotation', NULL, 2, 2, 2)
                    """.trimIndent()
                )

                assertEquals(listOf(2L), connection.matches("tire*"))

                connection.execute("DELETE FROM `service_records` WHERE `id` = 'record-1'")

                assertTrue(connection.matches("brak*").isEmpty())
                assertEquals(listOf(2L), connection.matches("tire*"))

                connection.execute("DELETE FROM `service_records` WHERE `id` = 'record-2'")

                assertTrue(connection.matches("tire*").isEmpty())
            }
        } finally {
            databaseFile.deleteIfExists()
        }
    }
}

private fun SQLiteConnection.execute(sql: String) {
    prepare(sql).use { statement -> statement.step() }
}

private fun SQLiteConnection.matches(token: String): List<Long> =
    prepare("SELECT rowid FROM records_fts WHERE records_fts MATCH ?").use { statement ->
        statement.bindText(1, token)
        buildList { while (statement.step()) add(statement.getLong(0)) }
    }
