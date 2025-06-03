package org.ideas2it.composecraft.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.icons.AllIcons
import javax.swing.*
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import org.ideas2it.composecraft.services.AIService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A custom panel that implements a chat interface for the ComposeCraft plugin.
 * This panel provides a complete chat UI with message history, input area, and controls.
 * 
 * Features:
 * - Message history with scrolling
 * - Text input area with support for multi-line input
 * - Image upload capability
 * - Loading indicators for async operations
 * - Theme-aware UI components
 *
 * @property project The current IntelliJ project instance
 * @property aiService Service for handling AI-related operations
 */
class ChatPanel(
    private val project: Project,
    private val aiService: AIService
) : JPanel(BorderLayout()) {

    /**
     * Panel containing the chat history.
     * Uses BoxLayout for vertical message stacking.
     */
    private val historyPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = JBColor.background()
        alignmentX = Component.LEFT_ALIGNMENT
    }

    /**
     * Scrollable container for the history panel.
     * Configured for vertical scrolling only.
     */
    private val historyScrollPane = JBScrollPane(historyPanel).apply {
        border = JBUI.Borders.empty()
        verticalScrollBar.unitIncrement = 16
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
    }

    /**
     * Reference to the current loading indicator bubble.
     * Null when no loading operation is in progress.
     */
    private var loadingBubble: JPanel? = null

    init {
        border = JBUI.Borders.empty(8)
        setupUI()
    }

    /**
     * Sets up the main UI components of the chat panel.
     * Organizes the layout into history and input sections.
     */
    private fun setupUI() {
        // Setup history section with scrolling
        val historyContainer = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty()
            add(historyScrollPane, BorderLayout.CENTER)
        }
        add(historyContainer, BorderLayout.CENTER)

        // Setup input section at bottom
        add(createInputPanel(), BorderLayout.SOUTH)
    }

    /**
     * Creates the input panel with text area and control buttons.
     * 
     * @return A panel containing the input area and control buttons
     */
    private fun createInputPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 0, 0, 0)
            
            // Create and configure the input text area
            val inputArea = JTextArea(3, 50).apply {
                font = UIUtil.getLabelFont()
                lineWrap = true
                wrapStyleWord = true
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(JBColor.border()),
                    JBUI.Borders.empty(8)
                )
                maximumSize = Dimension(600, Int.MAX_VALUE)

                // Handle Ctrl+Enter for sending messages
                addKeyListener(object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        if (e.keyCode == KeyEvent.VK_ENTER && e.isControlDown) {
                            val text = text.trim()
                            if (text.isNotEmpty()) {
                                processUserInput(text)
                                setText("")
                            }
                            e.consume()
                        }
                    }
                })
            }
            
            // Create button panel with upload and send buttons
            val buttonPanel = createButtonPanel(inputArea)

            // Add components to the input panel
            add(JBScrollPane(inputArea).apply {
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            }, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.SOUTH)
        }
    }

    /**
     * Creates the panel containing upload and send buttons.
     * 
     * @param inputArea Reference to the input text area for accessing its content
     * @return Panel containing the control buttons
     */
    private fun createButtonPanel(inputArea: JTextArea): JPanel {
        return JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
            // Create image upload button
            val uploadButton = JButton("Upload Image", AllIcons.Actions.Upload).apply {
                addActionListener {
                    val uploader = ImageUploader(project)
                    if (uploader.showAndGet()) {
                        val base64Image = uploader.getBase64Image()
                        val prompt = uploader.getPrompt()
                        if (base64Image != null) {
                            processUserInput(prompt ?: "Generate a compose code for this image", base64Image)
                        }
                    }
                }
            }
            
            // Create send button
            val sendButton = JButton("Send", AllIcons.Actions.Execute).apply {
                addActionListener {
                    val text = inputArea.text.trim()
                    if (text.isNotEmpty()) {
                        processUserInput(text)
                        inputArea.text = ""
                    }
                }
            }

            add(uploadButton)
            add(sendButton)
        }
    }

    /**
     * Adds a new message bubble to the chat history.
     * 
     * @param message The message content to display
     * @param sender The sender of the message ("User" or "System")
     */
    fun addMessageBubble(message: String, sender: String) {
        val bubblePanel = MessageBubbleFactory.createMessageBubble(message, sender, project)
        historyPanel.add(Box.createVerticalStrut(8))
        historyPanel.add(bubblePanel)
        historyPanel.add(Box.createVerticalStrut(8))
        historyPanel.revalidate()
        historyPanel.repaint()

        // Auto-scroll to the latest message
        SwingUtilities.invokeLater {
            val vertical = historyScrollPane.verticalScrollBar
            vertical.value = vertical.maximum
        }
    }

    /**
     * Shows a loading indicator in the chat history.
     * Used during async operations like AI processing.
     */
    fun showLoadingIndicator() {
        SwingUtilities.invokeLater {
            loadingBubble = LoadingBubbleFactory.createLoadingBubble()
            historyPanel.add(Box.createVerticalStrut(8))
            historyPanel.add(loadingBubble)
            historyPanel.add(Box.createVerticalStrut(8))
            historyPanel.revalidate()
            historyPanel.repaint()
            
            // Auto-scroll to show the loading indicator
            val vertical = historyScrollPane.verticalScrollBar
            vertical.value = vertical.maximum
        }
    }

    /**
     * Removes the loading indicator from the chat history.
     * Called when async operations complete.
     */
    fun hideLoadingIndicator() {
        SwingUtilities.invokeLater {
            loadingBubble?.let { bubble ->
                try {
                    // Remove the loading bubble and its spacing
                    val components = historyPanel.components
                    val index = components.indexOf(bubble)
                    
                    if (index >= 0) {
                        if (index > 0) historyPanel.remove(index - 1)
                        historyPanel.remove(bubble)
                        if (index < components.size - 1) {
                            historyPanel.remove(index - (if (index > 0) 1 else 0))
                        }
                        
                        historyPanel.revalidate()
                        historyPanel.repaint()
                    }
                } catch (e: Exception) {
                    // Fallback cleanup if precise removal fails
                    historyPanel.remove(bubble)
                    historyPanel.revalidate()
                    historyPanel.repaint()
                }
            }
            loadingBubble = null
        }
    }

    /**
     * Processes user input and generates AI response.
     * 
     * @param text The user's input text
     * @param imageData Optional base64-encoded image data
     */
    private fun processUserInput(text: String, imageData: String? = null) {
        // Display user message
        addMessageBubble(text, "User")
        showLoadingIndicator()
        
        // Process input in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = aiService.generateCode(text, imageData)
                hideLoadingIndicator()
                addMessageBubble(response, "System")
            } catch (e: Exception) {
                hideLoadingIndicator()
                addMessageBubble("Error: ${e.message}", "System")
            }
        }
    }

    /**
     * Clears all messages from the chat history.
     */
    fun clearHistory() {
        historyPanel.removeAll()
        historyPanel.revalidate()
        historyPanel.repaint()
    }
} 