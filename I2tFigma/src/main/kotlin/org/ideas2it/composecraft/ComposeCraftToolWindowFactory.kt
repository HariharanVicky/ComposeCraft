package org.ideas2it.composecraft

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.*
import javax.swing.*
import org.ideas2it.composecraft.ui.*
import org.ideas2it.composecraft.services.AIService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ideas2it.composecraft.listener.ComposeCraftNotifier

/**
 * Factory class for creating the ComposeCraft tool window in IntelliJ IDEA.
 * This class is responsible for initializing and managing the main UI components
 * of the ComposeCraft plugin, including the chat interface and toolbar.
 *
 * The tool window provides functionality for:
 * - AI-powered code generation
 * - Image processing and analysis
 * - Model selection through toolbar
 * - Chat-based interaction with AI
 */
class ComposeCraftToolWindowFactory : ToolWindowFactory {

    /**
     * UI component that contains model selection and other controls
     */
    private lateinit var toolbarPanel: ToolbarPanel

    /**
     * Main chat interface component for user interaction
     */
    private lateinit var chatPanel: ChatPanel

    /**
     * Reference to the current IntelliJ project
     */
    private lateinit var project: Project

    /**
     * Service handling AI-related operations
     */
    private lateinit var aiService: AIService

    /**
     * Creates and initializes the tool window content.
     * This method sets up the main UI components and configures the message bus
     * for handling various events.
     *
     * @param project The current IntelliJ project
     * @param toolWindow The tool window to populate with content
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        this.project = project
        this.aiService = AIService.getInstance(project)

        // Initialize main panel with BorderLayout
        val mainPanel = JPanel(BorderLayout())
        mainPanel.border = null

        // Create toolbar with model selector
        toolbarPanel = ToolbarPanel(project)
        mainPanel.add(toolbarPanel, BorderLayout.NORTH)

        // Setup chat panel content
        chatPanel = ChatPanel(project, aiService)
        mainPanel.add(chatPanel, BorderLayout.CENTER)

        // Add content to tool window
        val content = ContentFactory.getInstance().createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)

        // Subscribe to message bus for event handling
        project.messageBus.connect().subscribe(
            ComposeCraftNotifier.TOPIC,
            object : ComposeCraftNotifier {
                /**
                 * Handles refresh requests from the UI
                 * Currently placeholder for future implementation
                 */
                override fun onRefreshRequested() {
                    // Implement refresh logic
                }

                /**
                 * Clears the chat history when requested
                 */
                override fun onHistoryCleared() {
                    chatPanel.clearHistory()
                }

                /**
                 * Handles settings changes
                 * Currently placeholder for future implementation
                 */
                override fun onSettingsChanged() {
                    // Apply new settings
                }

                /**
                 * Legacy preview toggle handler
                 * Kept for interface compatibility
                 */
                override fun onPreviewToggled(isVisible: Boolean) {
                    // Preview functionality removed
                }

                /**
                 * Processes uploaded images with optional text prompts
                 * 
                 * @param base64Image The image data in base64 format
                 * @param prompt Optional text prompt for image processing
                 */
                override fun onImageUploaded(base64Image: String, prompt: String) {
                    // Show processing status
                    chatPanel.addMessageBubble("Processing image...", "System")
                    chatPanel.showLoadingIndicator()

                    // Process image in background
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Generate code from image
                            val response = aiService.generateCode(prompt, base64Image)
                            chatPanel.hideLoadingIndicator()

                            // Display results
                            if (prompt.isNotEmpty()) {
                                chatPanel.addMessageBubble("Instructions: $prompt", "User")
                            }
                            chatPanel.addMessageBubble("Image processed successfully", "System")
                            chatPanel.addMessageBubble(response, "System")
                        } catch (e: Exception) {
                            // Handle errors
                            chatPanel.hideLoadingIndicator()
                            chatPanel.addMessageBubble("Error: ${e.message}", "System")
                        }
                    }
                }
            }
        )
    }
}
