package ru.haykor.carmanager.core.data.repository

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.vinceglb.filekit.*
import io.github.vinceglb.filekit.exceptions.FileKitException
import ru.haykor.carmanager.core.data.local.di.FilesDir
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.repository.PhotoRepository
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.core.result.EmptyResult
import java.io.File
import java.io.IOException
import kotlin.uuid.Uuid

@ContributesBinding(AppScope::class)
@Inject
class DefaultPhotoRepository(@FilesDir private val filesDir: PlatformFile) : PhotoRepository {

    private val photoDir = filesDir / PHOTO_DIR

    override suspend fun save(source: String): DataResult<String, DataError> = try {
        val image = PlatformFile(source)

        val hash = Uuid.random()
        val imageName = "$hash.${image.extension}"
        val key = "$PHOTO_DIR/$imageName"
        val destination = filesDir.resolve(key)

        // If 'photo/' doesn't exist
        photoDir.createDirectories()

        destination write image
        DataResult.Success(key)
    } catch (e: IOException) {
        DataResult.Failure(DataError.Local.Unknown)
    } catch (e: FileKitException) {
        DataResult.Failure(DataError.Local.Unknown)
    }

    override suspend fun delete(keys: List<String>): EmptyResult<DataError> {
        // Keys are app-private paths, so a plain file delete is enough. FileKit's delete checks for
        // symlinks via android.system.Os, which host unit tests cannot call.
        keys.forEach { key ->
            File(filesDir.path, key).delete()
        }
        return DataResult.Success(Unit)
    }

    private companion object {
        const val PHOTO_DIR = "photo"
    }
}
