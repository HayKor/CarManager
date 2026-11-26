package ru.haykor.carmanager.core.data.repository

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.haykor.carmanager.core.result.getOrNull
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultPhotoRepositoryTest {

    private lateinit var baseDir: File
    private lateinit var repository: DefaultPhotoRepository

    @Before
    fun setUp() {
        baseDir = Files.createTempDirectory("photo-repository").toFile()
        repository = DefaultPhotoRepository(PlatformFile(baseDir))
    }

    @After
    fun tearDown() {
        baseDir.deleteRecursively()
    }

    @Test
    fun `save copies the source bytes into the photo directory`() = runBlocking {
        val source = file("cat.PNG", byteArrayOf(1, 2, 3))

        val key = assertNotNull(repository.save(source.absolutePath).getOrNull())

        val saved = File(baseDir, key)
        assertTrue(saved.isFile)
        assertEquals(source.readBytes().toList(), saved.readBytes().toList())
    }

    @Test
    fun `save returns a photo key that keeps the source extension`() = runBlocking {
        val source = file("cat.PNG", byteArrayOf(1))

        val key = assertNotNull(repository.save(source.absolutePath).getOrNull())

        assertTrue(key.matches(Regex("photo/[0-9a-fA-F-]{36}\\.PNG")))
    }

    @Test
    fun `save creates the photo directory when it is missing`() = runBlocking {
        val source = file("cat.jpg", byteArrayOf(1))

        val key = assertNotNull(repository.save(source.absolutePath).getOrNull())

        assertEquals("photo", File(baseDir, key).parentFile?.name)
        assertTrue(File(baseDir, "photo").isDirectory)
    }

    @Test
    fun `save of a missing source fails without leaving a file behind`() = runBlocking {
        val missing = File(baseDir, "missing.jpg").absolutePath

        assertNull(repository.save(missing).getOrNull())

        val photoDir = File(baseDir, "photo")
        assertTrue(photoDir.listFiles().isNullOrEmpty())
    }

    @Test
    fun `save of a non-file source fails instead of throwing`() = runBlocking {
        val dir = File(baseDir, "broken.jpg").apply { mkdirs() }

        assertNull(repository.save(dir.absolutePath).getOrNull())
    }

    @Test
    fun `delete removes the listed files`() = runBlocking {
        val first = file("photo/first.jpg", byteArrayOf(1))
        val second = file("photo/second.jpg", byteArrayOf(2))

        assertNotNull(repository.delete(listOf("photo/first.jpg", "photo/second.jpg")).getOrNull())

        assertFalse(first.exists())
        assertFalse(second.exists())
    }

    @Test
    fun `delete of missing keys succeeds`() = runBlocking {
        assertNotNull(repository.delete(listOf("photo/missing.jpg")).getOrNull())
    }

    @Test
    fun `delete leaves files outside the list untouched`() = runBlocking {
        val sentinel = file("photo/sentinel.jpg", byteArrayOf(1))
        val removed = file("photo/removed.jpg", byteArrayOf(2))

        repository.delete(listOf("photo/removed.jpg"))

        assertTrue(sentinel.exists())
        assertFalse(removed.exists())
    }

    @Test
    fun `delete resolves the photo prefix exactly once`() = runBlocking {
        // Regression: keys already contain the 'photo/' prefix, so it must not be added twice
        val saved = file("photo/1.jpg", byteArrayOf(1))

        repository.delete(listOf("photo/1.jpg"))

        assertFalse(saved.exists())
        assertFalse(File(baseDir, "photo/photo").exists())
    }

    @Test
    fun `delete removes a file written by save`() = runBlocking {
        val source = file("cat.jpg", byteArrayOf(1, 2, 3))
        val key = assertNotNull(repository.save(source.absolutePath).getOrNull())

        repository.delete(listOf(key))

        assertFalse(File(baseDir, key).exists())
    }

    private fun file(path: String, bytes: ByteArray): File = File(baseDir, path).apply {
        parentFile?.mkdirs()
        writeBytes(bytes)
    }
}
