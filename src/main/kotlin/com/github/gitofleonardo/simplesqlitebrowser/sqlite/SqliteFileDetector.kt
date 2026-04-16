package com.github.gitofleonardo.simplesqlitebrowser.sqlite

import com.github.gitofleonardo.simplesqlitebrowser.EXTENSION
import com.github.gitofleonardo.simplesqlitebrowser.EXTENSION_SQLITE
import com.github.gitofleonardo.simplesqlitebrowser.EXTENSION_SQLITE3
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

/**
 * Decides whether a [VirtualFile] should open with the SQLite browser editor.
 *
 * Known extensions ([EXTENSION], [EXTENSION_SQLITE], [EXTENSION_SQLITE3]) use a fast path with no I/O.
 * Otherwise the first 16 bytes are read and compared to the SQLite 3 file header (magic).
 */
object SqliteFileDetector {
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    private val KNOWN_EXTENSIONS = setOf(EXTENSION, EXTENSION_SQLITE, EXTENSION_SQLITE3)

    /** Skip magic probe for very large files to avoid pointless work on non-SQLite binaries. */
    const val MAX_LENGTH_FOR_HEADER_PROBE: Long = 512L * 1024 * 1024

    fun isKnownSqliteExtension(file: VirtualFile): Boolean {
        val ext = file.extension?.lowercase() ?: return false
        return ext in KNOWN_EXTENSIONS
    }

    fun shouldOpenWithSqliteEditor(file: VirtualFile): Boolean {
        if (!file.isValid || file.isDirectory) return false
        if (isKnownSqliteExtension(file)) return true
        val length = file.length
        if (length < SQLITE_HEADER.size) return false
        if (length > MAX_LENGTH_FOR_HEADER_PROBE) return false
        return headerMatches(file)
    }

    private fun headerMatches(file: VirtualFile): Boolean {
        return try {
            file.inputStream.use { stream ->
                val buf = stream.readNBytes(SQLITE_HEADER.size)
                buf.contentEquals(SQLITE_HEADER)
            }
        } catch (_: IOException) {
            false
        }
    }
}
