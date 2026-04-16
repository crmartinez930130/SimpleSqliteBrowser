package com.github.gitofleonardo.simplesqlitebrowser.ui.window

import com.github.gitofleonardo.simplesqlitebrowser.PLUGIN_DISPLAY_NAME
import com.github.gitofleonardo.simplesqlitebrowser.data.SqliteMetadata
import com.github.gitofleonardo.simplesqlitebrowser.tools.DatabaseTreeCellRenderer
import com.github.gitofleonardo.simplesqlitebrowser.tools.DatabaseTreeModel
import com.github.gitofleonardo.simplesqlitebrowser.ui.TabbedChildView
import com.github.gitofleonardo.simplesqlitebrowser.ui.viewmodel.MetadataViewModel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

private const val TITLE = "Database Metadata"

class SqliteMetadataWindow(private val dbFile: VirtualFile) : TabbedChildView() {
    override val title: String = TITLE
    override val icon: Icon? = null

    private val viewModel = MetadataViewModel()
    private val emptyMetadata = SqliteMetadata()
    private var treeModel = DatabaseTreeModel(emptyMetadata)

    private lateinit var rootTree: Tree
    private lateinit var rootContainer: JPanel
    private lateinit var treeScrollContainer: JBScrollPane

    init {
        setupUi()
        bindViewModel()
        viewModel.loadMetaData(dbFile)
    }

    override fun dispose() {
        viewModel.dispose()
        super.dispose()
    }

    private fun bindViewModel() {
        viewModel.metadata.observe { meta ->
            if (!meta.isValidSqliteDatabase) return@observe
            treeModel = DatabaseTreeModel(meta)
            rootTree.model = treeModel
        }
        viewModel.loadError.observe { message ->
            if (message.isEmpty()) return@observe
            Messages.showErrorDialog(this, message, PLUGIN_DISPLAY_NAME)
        }
    }

    private fun setupUi() {
        rootContainer = JPanel(BorderLayout())
        rootTree = Tree().apply {
            cellRenderer = DatabaseTreeCellRenderer()
            model = treeModel
        }
        treeScrollContainer = JBScrollPane(rootTree).apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        }
        rootContainer.add(treeScrollContainer, BorderLayout.CENTER)
        layout = BorderLayout()
        add(rootContainer)
    }
}
