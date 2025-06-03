package org.ideas2it.composecraft.ui

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*

class PreviewPanel : JPanel(BorderLayout()) {
    init {
        border = JBUI.Borders.empty(8)
        setupUI()
    }

    private fun setupUI() {
        // Header
        add(createHeaderPanel(), BorderLayout.NORTH)
        
        // Preview content
        add(createPreviewContent(), BorderLayout.CENTER)
    }

    private fun createHeaderPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            background = ThemeManager.toolbarBackground
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.dividerColor),
                JBUI.Borders.empty(8)
            )
            
            add(JLabel("Preview").apply {
                font = UIUtil.getLabelFont().deriveFont(Font.BOLD, 14f)
                foreground = JBColor.foreground()
            }, BorderLayout.WEST)
        }
    }

    private fun createPreviewContent(): JComponent {
        val previewContent = JPanel(BorderLayout()).apply {
            background = ThemeManager.codeBackground
            border = JBUI.Borders.empty(16)
            
            add(JLabel("Preview will appear here", SwingConstants.CENTER).apply {
                foreground = ThemeManager.codeForeground
                font = UIUtil.getLabelFont()
            }, BorderLayout.CENTER)
        }

        return JBScrollPane(previewContent).apply {
            border = JBUI.Borders.empty()
            verticalScrollBar.unitIncrement = 16
        }
    }
} 