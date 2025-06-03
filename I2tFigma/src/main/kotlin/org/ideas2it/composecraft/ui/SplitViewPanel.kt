package org.ideas2it.composecraft.ui

import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.SwingUtilities

class SplitViewPanel : JPanel(BorderLayout()) {
    private val splitter = JBSplitter(false, 0.5f, 0.3f, 0.7f).apply {
        dividerWidth = 3
        isShowDividerControls = true
        isShowDividerIcon = true
    }
    
    private var leftComponent: JComponent? = null
    private var rightComponent: JComponent? = null
    private var lastSplitRatio = 0.5f
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)
    private val SPLIT_VIEW = "SPLIT_VIEW"
    private val CHAT_VIEW = "CHAT_VIEW"
    
    init {
        add(contentPanel, BorderLayout.CENTER)
        contentPanel.add(splitter, SPLIT_VIEW)
        contentPanel.add(JPanel().apply {
            layout = BorderLayout()
        }, CHAT_VIEW)
        border = JBUI.Borders.empty()
        showSplitView(true)
    }
    
    fun setLeftContent(component: JComponent) {
        leftComponent = component
        val scrollPane = JBScrollPane(component).apply {
            border = JBUI.Borders.empty()
            verticalScrollBar.unitIncrement = 16
        }
        splitter.firstComponent = scrollPane
        
        // Also set the component in chat view
        (contentPanel.getComponent(1) as JPanel).apply {
            removeAll()
            add(scrollPane, BorderLayout.CENTER)
        }
    }
    
    fun setRightContent(component: JComponent) {
        rightComponent = component
        val scrollPane = JBScrollPane(component).apply {
            border = JBUI.Borders.empty()
            verticalScrollBar.unitIncrement = 16
        }
        splitter.secondComponent = scrollPane
    }
    
    fun setSplitRatio(ratio: Float) {
        lastSplitRatio = ratio.coerceIn(0.1f, 0.9f)
        splitter.proportion = lastSplitRatio
    }
    
    fun showSplitView(show: Boolean) {
        SwingUtilities.invokeLater {
            cardLayout.show(contentPanel, if (show) SPLIT_VIEW else CHAT_VIEW)
            if (show) {
                splitter.proportion = lastSplitRatio
            }
            revalidate()
            repaint()
        }
    }
    
    fun getLeftComponent(): JComponent? = leftComponent
    fun getRightComponent(): JComponent? = rightComponent
} 