package com.github.gitofleonardo.simplesqlitebrowser.ui.viewmodel

import com.github.gitofleonardo.simplesqlitebrowser.data.SqliteMetadata
import com.github.gitofleonardo.simplesqlitebrowser.model.SqliteModel
import com.github.gitofleonardo.simplesqlitebrowser.mvvm.LiveData
import com.github.gitofleonardo.simplesqlitebrowser.mvvm.ViewModel
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.swing.SwingUtilities

private val LOG = Logger.getInstance(MetadataViewModel::class.java)

class MetadataViewModel : ViewModel {
    private val model = SqliteModel
    private val disposables = CompositeDisposable()
    @Volatile
    private var disposed = false

    val metadata: LiveData<SqliteMetadata> = LiveData()
    val loadError = LiveData<String>()

    fun loadMetaData(file: VirtualFile) {
        val d = Observable
            .fromCallable { model.loadMetaData(file) }
            .subscribeOn(Schedulers.io())
            .subscribe(
                { result ->
                    SwingUtilities.invokeLater {
                        if (!disposed) {
                            metadata.value = result
                            loadError.value = ""
                        }
                    }
                },
                { error ->
                    LOG.warn(error)
                    SwingUtilities.invokeLater {
                        if (!disposed) {
                            loadError.value = error.message ?: error.toString()
                        }
                    }
                }
            )
        disposables.add(d)
    }

    override fun dispose() {
        disposed = true
        disposables.dispose()
    }
}
