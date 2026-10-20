package ru.haykor.carmanager.core.data

import androidx.sqlite.SQLiteException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.result.DataResult

class SafeCallTest {

    @Test
    fun `returns Success with block result`() = runTest {
        val result = safeCall { 42 }

        assertEquals(DataResult.Success(42), result)
    }

    @Test
    fun `SQLite error becomes Failure`() = runTest {
        val result = safeCall { throw SQLiteException("disk I/O error") }

        assertEquals(DataResult.Failure(DataError.Local.Unknown), result)
    }

    @Test
    fun `cancellation is rethrown, not turned into Failure`() = runTest {
        val cancellation = CancellationException("scope cancelled")

        val thrown = assertFailsWith<CancellationException> {
            safeCall { throw cancellation }
        }
        assertSame(cancellation, thrown)
    }

    @Test
    fun `unexpected exception is rethrown`() = runTest {
        val bug = IllegalStateException("mapper bug")

        val thrown = assertFailsWith<IllegalStateException> {
            safeCall { throw bug }
        }
        assertSame(bug, thrown)
    }
}
