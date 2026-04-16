package com.github.gitofleonardo.simplesqlitebrowser.mvvm

/**
 * ViewModels tied to a file editor must release resources when the editor is closed.
 */
interface ViewModel {
    fun dispose()
}