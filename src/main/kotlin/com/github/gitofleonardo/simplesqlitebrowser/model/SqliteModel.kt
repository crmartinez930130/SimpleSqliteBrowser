package com.github.gitofleonardo.simplesqlitebrowser.model

import com.github.gitofleonardo.simplesqlitebrowser.data.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import java.sql.ResultSet
import java.sql.Types

/**
 * JDBC-backed SQLite access for the plugin.
 *
 * **Exceptions:** [loadMetaData], [loadTables], and [loadTableData] throw [IllegalStateException] when
 * the database cannot be opened. In this codebase they run only inside [io.reactivex.rxjava3.core.Observable.fromCallable]
 * in the view models; RxJava catches that and forwards it to `subscribe(..., onError)`, which updates UI state — it does
 * **not** become an uncaught exception on the IDE main thread. Any new direct caller must use `try/catch`, Rx, or [runCatching].
 */
object SqliteModel {
    const val NULL = "null"
    const val BLOB = "BLOB"

    private val LOG = Logger.getInstance(SqliteModel::class.java)

    private fun openConnectionOrThrow(file: VirtualFile) =
        ConnectionManager.createConnection(file)
            ?: throw IllegalStateException(
                "Unable to open SQLite database \"${file.name}\". " +
                    "The file may be locked, inaccessible, or not a valid SQLite database."
            )

    /**
     * @throws IllegalStateException if the database file cannot be opened.
     */
    fun loadMetaData(file: VirtualFile): SqliteMetadata {
        LOG.debug("loadMetaData: ${file.path}")
        val connection = openConnectionOrThrow(file)
        val metadata = SqliteMetadata()
        try {
            val md = connection.metaData
            metadata.isValidSqliteDatabase = true
            metadata.version = md.databaseMajorVersion
            metadata.driverVersion = md.driverVersion

            val tables = ArrayList<DbTable>()
            val tableResult = md.getTables(null, null, "%", null)
            while (tableResult.next()) {
                val tb = DbTable()
                tb.tableName = tableResult.getString("TABLE_NAME")
                val tableType = tableResult.getString("TABLE_TYPE")
                if ("TABLE" != tableType) {
                    continue
                }
                val columnResult = md.getColumns(null, null, tb.tableName, null)
                while (columnResult.next()) {
                    val columnName = columnResult.getString("COLUMN_NAME")
                    val type = columnResult.getInt("DATA_TYPE")
                    val typeName = columnResult.getString("TYPE_NAME")
                    val schema = getAllSchema(columnResult)
                    tb.columns.add(DbColumn(columnName, type, typeName, schema))
                }
                tables.add(tb)
            }
            metadata.tables.addAll(tables)
            LOG.debug("loadMetaData done: ${file.path}, tables=${tables.size}")
            return metadata
        } finally {
            ConnectionManager.disposeConnection(connection)
        }
    }

    /**
     * @throws IllegalStateException if the database file cannot be opened.
     */
    fun loadTables(file: VirtualFile): List<String> {
        LOG.debug("loadTables: ${file.path}")
        val connection = openConnectionOrThrow(file)
        return try {
            val result = mutableListOf<String>()
            val resultSet = connection.metaData.getTables(null, null, "%", null)
            while (resultSet.next()) {
                val table = resultSet.getString("TABLE_NAME")
                val type = resultSet.getString("TABLE_TYPE")
                if ("TABLE" == type) {
                    result.add(table)
                }
            }
            LOG.debug("loadTables done: ${file.path}, count=${result.size}")
            result
        } finally {
            ConnectionManager.disposeConnection(connection)
        }
    }

    /**
     * @throws IllegalStateException if the database file cannot be opened.
     */
    fun loadTableData(file: VirtualFile, tableName: String, pageCount: Int, page: Int): DbTableInstance {
        val columns = mutableListOf<DbColumn>()
        val rows = mutableListOf<DbRow>()
        var totalCount = 0
        LOG.debug("loadTableData: ${file.path} table=$tableName page=$page pageSize=$pageCount")
        val connection = openConnectionOrThrow(file)
        try {
            val columnResult = connection.metaData.getColumns(null, null, tableName, null)
            while (columnResult.next()) {
                val columnName = columnResult.getString("COLUMN_NAME")
                val type = columnResult.getInt("DATA_TYPE")
                val typeName = columnResult.getString("TYPE_NAME")
                val schema = getAllSchema(columnResult)
                columns.add(DbColumn(columnName, type, typeName, schema))
            }

            val statement = connection.createStatement()
            val rowResult = statement.executeQuery("SELECT * FROM \"$tableName\" LIMIT $pageCount OFFSET ${pageCount * (page - 1)}")
            val rowMeta = rowResult.metaData
            while (rowResult.next()) {
                val dbRows = mutableListOf<DbRow.RowData>()
                val dbRow = DbRow(dbRows)
                for (columnIndex in columns.indices) {
                    val type = rowMeta.getColumnType(columnIndex + 1)
                    val typeName = rowMeta.getColumnTypeName(columnIndex + 1)
                    val rowData = when (type) {
                        Types.BLOB -> {
                            DbRow.RowData(type, typeName, rowResult.getBytes(columnIndex + 1))
                        }
                        else -> {
                            DbRow.RowData(type, typeName, rowResult.getObject(columnIndex + 1))
                        }
                    }
                    dbRows.add(rowData)
                }
                rows.add(dbRow)
            }

            val countResult = statement.executeQuery("SELECT COUNT(*) FROM \"$tableName\"")
            countResult.next()
            totalCount = countResult.getInt(1)
            LOG.debug(
                "loadTableData done: ${file.path} table=$tableName rows=${rows.size} totalCount=$totalCount"
            )
            return DbTableInstance(columns, rows, rows.size, page, totalCount)
        } finally {
            ConnectionManager.disposeConnection(connection)
        }
    }

    private fun getAllSchema(resultSet: ResultSet): String {
        val nullable = resultSet.getBoolean("NULLABLE")
        val nullableString = if (nullable) "" else "NOT NULL"
        var def = resultSet.getString("COLUMN_DEF")
        def = if (def == null || def.isEmpty()) {
            ""
        } else {
            "DEFAULT $def"
        }
        val autoIncrement = resultSet.getBoolean("IS_AUTOINCREMENT")
        val autoIncString = if (autoIncrement) "AUTO INCREMENT" else ""
        return "$nullableString $def $autoIncString"
    }
}
