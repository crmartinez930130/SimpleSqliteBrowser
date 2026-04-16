package com.github.gitofleonardo.simplesqlitebrowser.ui.viewmodel

import com.github.gitofleonardo.simplesqlitebrowser.data.DbTableInstance
import com.github.gitofleonardo.simplesqlitebrowser.model.SqliteModel
import com.github.gitofleonardo.simplesqlitebrowser.mvvm.LiveData
import com.github.gitofleonardo.simplesqlitebrowser.mvvm.ViewModel
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities
import kotlin.math.ceil

private val LOG = Logger.getInstance(TableViewModel::class.java)

private const val DEFAULT_PAGE_COUNT = 50

class TableViewModel(private val dbFile: VirtualFile) : ViewModel {
    private val model = SqliteModel
    var currentPage: Int = 1
    var pageCount: Int = DEFAULT_PAGE_COUNT
    var currentTableName: String? = null
    var totalPages: Int = 1
    var totalCount: Int = 0
    private val loadingTaskCount = AtomicInteger(0)
    private val disposables = CompositeDisposable()
    @Volatile
    private var disposed = false

    val tables = LiveData<List<String>>()
    val tableData = LiveData<DbTableInstance>()
    val isLoading = LiveData<Boolean>()
    /** Non-empty means the last async load failed; cleared on successful load. */
    val loadError = LiveData<String>()

    fun resetTableData() {
        currentTableName?.let { resetTableData(it) }
    }

    fun resetTableData(tableName: String) {
        currentPage = 1
        currentTableName = tableName
        loadTableData(dbFile, tableName, pageCount, currentPage)
    }

    fun loadNextPage() {
        currentTableName?.let {
            if (currentPage < totalPages) {
                ++currentPage
                loadTableData(dbFile, it, pageCount, currentPage)
            }
        }
    }

    fun loadPreviousPage() {
        currentTableName?.let {
            if (currentPage > 1) {
                --currentPage
                loadTableData(dbFile, it, pageCount, currentPage)
            }
        }
    }

    fun loadPage(page: Int) {
        if (page < 1 || page > totalPages) {
            return
        }
        currentTableName?.let {
            currentPage = page
            loadTableData(dbFile, it, pageCount, currentPage)
        }
    }

    fun loadFirstPage() {
        loadPage(1)
    }

    fun loadLastPage() {
        loadPage(totalPages)
    }

    private fun loadTableData(file: VirtualFile, tableName: String, pageCount: Int, page: Int) {
        increaseLoading()
        val d = Observable
            .fromCallable { model.loadTableData(file, tableName, pageCount, page) }
            .subscribeOn(Schedulers.io())
            .doFinally {
                decreaseLoading()
            }
            .subscribe(
                { result ->
                    runOnEdtIfAlive {
                        totalCount = result.totalCount
                        totalPages = ceil(totalCount.toFloat() / pageCount).toInt()
                        tableData.value = result
                        loadError.value = ""
                    }
                },
                { error ->
                    LOG.warn(error)
                    runOnEdtIfAlive {
                        loadError.value = error.message ?: error.toString()
                    }
                }
            )
        disposables.add(d)
    }

    fun loadTables() {
        increaseLoading()
        val d = Observable
            .fromCallable { model.loadTables(dbFile) }
            .subscribeOn(Schedulers.io())
            .doFinally {
                decreaseLoading()
            }
            .subscribe(
                { tbls ->
                    runOnEdtIfAlive {
                        tables.value = tbls
                        loadError.value = ""
                    }
                },
                { error ->
                    LOG.warn(error)
                    runOnEdtIfAlive {
                        loadError.value = error.message ?: error.toString()
                    }
                }
            )
        disposables.add(d)
    }

    private fun runOnEdtIfAlive(block: () -> Unit) {
        SwingUtilities.invokeLater {
            if (!disposed) {
                block()
            }
        }
    }

    private fun increaseLoading() {
        if (disposed) {
            return
        }
        if (loadingTaskCount.incrementAndGet() == 1) {
            SwingUtilities.invokeLater {
                if (!disposed) {
                    isLoading.value = true
                }
            }
        }
    }

    private fun decreaseLoading() {
        if (loadingTaskCount.decrementAndGet() <= 0) {
            loadingTaskCount.set(0)
            SwingUtilities.invokeLater {
                if (!disposed) {
                    isLoading.value = false
                }
            }
        }
    }

    override fun dispose() {
        disposed = true
        disposables.dispose()
    }
}
