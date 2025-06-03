package org.ideas2it.composecraft.ui

import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.*

/**
 * Factory object for creating loading indicator bubbles in the ComposeCraft plugin.
 * This factory provides a consistent way to display loading states in the chat interface
 * with animated ellipsis (...) to indicate ongoing operations.
 *
 * Features:
 * - Animated loading indicator
 * - Theme-aware styling
 * - Consistent UI layout with message bubbles
 * - Auto-managed animation timer
 *
 * The loading bubble matches the style of regular message bubbles but indicates
 * an in-progress operation through animation.
 */
object LoadingBubbleFactory {
    /**
     * Creates a loading indicator bubble with animated ellipsis.
     * The bubble displays "Generating response..." with animated dots
     * that cycle through 1-3 dots every 500ms.
     *
     * The loading bubble follows the same styling as regular message bubbles
     * for visual consistency in the chat interface.
     *
     * Features:
     * - Transparent background for theme compatibility
     * - Consistent padding and margins
     * - Left-aligned content
     * - Animated ellipsis using a Swing Timer
     *
     * @return A JPanel containing the animated loading indicator
     */
    fun createLoadingBubble(): JPanel {
        val panel = JPanel(BorderLayout(5, 5))
        panel.isOpaque = false
        panel.border = JBUI.Borders.empty(5)
        panel.alignmentX = java.awt.Component.LEFT_ALIGNMENT

        val contentPanel = JPanel(BorderLayout(5, 5))
        contentPanel.isOpaque = false
        contentPanel.border = BorderFactory.createEmptyBorder(8, 12, 8, 12)

        val loadingLabel = JLabel("Generating response...")
        loadingLabel.font = UIUtil.getLabelFont()
        
        // Create a timer to animate the dots
        var dots = 0
        val timer = Timer(500) { 
            dots = (dots + 1) % 4
            loadingLabel.text = "Generating response" + ".".repeat(dots)
        }
        timer.start()

        contentPanel.add(loadingLabel, BorderLayout.CENTER)
        panel.add(contentPanel, BorderLayout.CENTER)

        return panel
    }
} 