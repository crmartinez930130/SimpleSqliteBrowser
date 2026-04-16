package com.github.gitofleonardo.simplesqlitebrowser.model

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import java.sql.Connection
import java.sql.DriverManager

object ConnectionManager {
    private val LOG = Logger.getInstance(ConnectionManager::class.java)

    // Ensure driver is loaded
    private val clazz = Class.forName("org.sqlite.JDBC")

    fun createConnection(file: VirtualFile): Connection? {
        return try {
            val connection = DriverManager.getConnection("jdbc:sqlite:${file.canonicalPath}")
            connection
        } catch (e: Exception) {
            LOG.warn("Failed to open SQLite connection for ${file.path}", e)
            null
        }
    }

    fun disposeConnection(connection: Connection?) {
        connection?.close()
    }
}
