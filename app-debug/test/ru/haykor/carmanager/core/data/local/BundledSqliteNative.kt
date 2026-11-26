package ru.haykor.carmanager.core.data.local

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * In host unit tests `androidx.sqlite:sqlite-bundled` resolves to its Android variant, which expects
 * `libsqliteJni` to be shipped in the APK and loads it via `System.loadLibrary`. There is no APK in a
 * JVM test, so we extract the same native library from `sqlite-bundled-jvm` and point the Android
 * loader at it via the `androidx.sqlite.driver.bundled.*` system properties.
 */
internal object BundledSqliteNative {

    private const val PATH_PROPERTY = "androidx.sqlite.driver.bundled.path"
    private const val NAME_PROPERTY = "androidx.sqlite.driver.bundled.name"

    fun install() {
        if (System.getProperty(PATH_PROPERTY) != null) return

        val os = System.getProperty("os.name").lowercase()
        val platform = when {
            os.contains("linux") -> "linux"
            os.contains("mac") || os.contains("osx") -> "osx"
            os.contains("windows") -> "windows"
            else -> error("Unsupported host OS: $os")
        }
        val arch = System.getProperty("os.arch").lowercase().let { arch ->
            when {
                arch == "aarch64" || (arch.contains("arm") && arch.contains("64")) -> "arm64"
                arch.contains("arm") -> "arm32"
                arch.contains("64") -> "x64"
                else -> "x86"
            }
        }
        val extension = when (platform) {
            "linux" -> "so"
            "osx" -> "dylib"
            else -> "dll"
        }
        val name = if (platform == "windows") "sqliteJni.$extension" else "libsqliteJni.$extension"
        val resource = "/natives/${platform}_$arch/$name"

        val stream = checkNotNull(BundledSqliteNative::class.java.getResourceAsStream(resource)) {
            "No bundled SQLite native library at $resource"
        }
        val dir = Files.createTempDirectory("sqlite-jni")
        val file = dir.resolve(name)
        stream.use { Files.copy(it, file, StandardCopyOption.REPLACE_EXISTING) }

        System.setProperty(PATH_PROPERTY, dir.toString())
        System.setProperty(NAME_PROPERTY, name)
    }
}
