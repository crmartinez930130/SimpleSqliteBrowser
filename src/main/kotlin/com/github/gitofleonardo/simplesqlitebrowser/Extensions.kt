package com.github.gitofleonardo.simplesqlitebrowser

import com.intellij.openapi.ui.ComboBox
import java.awt.event.ItemEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JComponent
import javax.swing.text.JTextComponent

inline fun <reified T> ComboBox<T>.addOnItemChangeListener(crossinline listener: (T) -> Unit) {
    addItemListener { e ->
        if (e.stateChange == ItemEvent.SELECTED) {
            val item = e.item
            if (item is T) listener(item)
        }
    }
}

fun JTextComponent.addOnKeyPressedListener(listener: (KeyEvent) -> Unit) {
    addKeyListener(object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            listener(e)
        }
    })
}

fun JComponent.addOnTouchListener(listener: (MouseEvent) -> Unit) {
    addMouseListener(object : MouseListener {
        override fun mouseClicked(e: MouseEvent) {}

        override fun mousePressed(e: MouseEvent) {
            listener(e)
        }

        override fun mouseReleased(e: MouseEvent) {}

        override fun mouseEntered(e: MouseEvent) {}

        override fun mouseExited(e: MouseEvent) {}
    })
}

fun Any?.toStringOr(placeHolder: String = ""): String = this?.toString() ?: placeHolder

private const val BYTES_PER_KIB = 1024
private const val BYTES_PER_MIB = 1024 * 1024

fun ByteArray.toSizeString(): String {
    val n = size
    return when {
        n <= BYTES_PER_KIB -> "$n Bytes"
        n <= BYTES_PER_MIB -> String.format("%.2f KB", n / BYTES_PER_KIB.toDouble())
        else -> String.format("%.2f MB", n / BYTES_PER_MIB.toDouble())
    }
}
