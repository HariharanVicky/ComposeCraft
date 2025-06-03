package org.ideas2it.composecraft.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.ide.ui.LafManager
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.Color
import org.ideas2it.composecraft.listener.ComposeCraftNotifier
import com.intellij.openapi.actionSystem.ActionToolbar

/**
 * A modern, customizable toolbar component for the ComposeCraft plugin.
 * This toolbar provides a set of actions for managing the plugin's functionality,
 * including image upload, conversation history management, and theme toggling.
 *
 * The toolbar is designed to match IntelliJ's UI guidelines and automatically
 * adapts to both light and dark themes.
 *
 * Features:
 * - Image upload and processing
 * - Conversation history management
 * - Theme switching (light/dark)
 * - Preview panel toggle
 *
 * @property project The current IntelliJ project instance
 * @property messageHistory Stores the conversation history as pairs of sender and message
 * @property isPreviewVisible Controls the visibility state of the preview panel
 * @property actionToolbar The IntelliJ action toolbar component
 */
class ModernToolbar(private val project: Project) : JPanel(BorderLayout()) {
    // Store reference to message history
    private var messageHistory: MutableList<Pair<String, String>> = mutableListOf()
    private var isPreviewVisible = true
    private lateinit var actionToolbar: ActionToolbar
    
    init {
        isOpaque = true
        background = JBColor.PanelBackground
        border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor(Color(0xDDDDDD), Color(0x515151)), 0, 0, 1, 0),
            JBUI.Borders.empty(4)
        )

        setupToolbar()
    }

    /**
     * Sets up the toolbar with action buttons.
     * Initializes and configures the action toolbar with the following actions:
     * - Upload Image
     * - Clear History
     * - Toggle Theme
     *
     * The toolbar is positioned on the west (left) side of the panel.
     */
    private fun setupToolbar() {
        val actionGroup = DefaultActionGroup().apply {
            add(UploadImageAction(::uploadImage))
            add(ClearHistoryAction(::clearHistory))
            add(ToggleThemeAction(::toggleTheme))
        }

        actionToolbar = ActionManager.getInstance().let { actionManager ->
            actionManager.createActionToolbar(
                ActionPlaces.TOOLBAR,  // Use standard toolbar place
                actionGroup,
                true
            ).apply {
                setTargetComponent(this@ModernToolbar)
                setMinimumButtonSize(ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
            }
        }
        
        add(actionToolbar.component, BorderLayout.WEST)
    }

    /**
     * Updates the conversation history with new messages.
     *
     * @param messages List of message pairs, where each pair contains:
     *                 - First: The sender identifier
     *                 - Second: The message content
     */
    fun updateMessageHistory(messages: List<Pair<String, String>>) {
        messageHistory = messages.toMutableList()
    }

    /**
     * Handles the clearing of conversation history.
     * Shows a confirmation dialog before clearing and notifies
     * the main window through the message bus if confirmed.
     */
    private fun clearHistory() {
        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to clear the conversation history?",
            "Clear History",
            "Clear",
            "Cancel",
            Messages.getQuestionIcon()
        )

        if (result == Messages.YES) {
            messageHistory.clear()
            // Notify the main window to clear the history
            val publisher = project.messageBus.syncPublisher(ComposeCraftNotifier.TOPIC)
            publisher.onHistoryCleared()
        }
    }

    /**
     * Toggles between light and dark themes.
     * Uses IntelliJ's LafManager to switch between Darcula (dark) and
     * default (light) themes.
     */
    private fun toggleTheme() {
        val lafManager = LafManager.getInstance()
        val currentLaf = lafManager.currentLookAndFeel
        val isDark = currentLaf.name.contains("Darcula")
        
        lafManager.installedLookAndFeels
            .find { laf -> 
                if (isDark) !laf.name.contains("Darcula")
                else laf.name.contains("Darcula")
            }?.let { newLaf ->
                lafManager.currentLookAndFeel = newLaf
            }
    }

    /**
     * Toggles the visibility of the preview panel.
     * Notifies subscribers through the message bus about the visibility change.
     */
    private fun togglePreview() {
        isPreviewVisible = !isPreviewVisible
        val publisher = project.messageBus.syncPublisher(ComposeCraftNotifier.TOPIC)
        publisher.onPreviewToggled(isPreviewVisible)
    }

    /**
     * Handles image upload functionality.
     * Shows the image upload dialog and processes the selected image.
     * If an image is successfully selected and processed:
     * - Converts it to base64 format
     * - Captures any associated prompt text
     * - Notifies subscribers through the message bus
     */
    private fun uploadImage() {
        val uploader = ImageUploader(project)
        if (uploader.showAndGet()) {
            val base64Image = uploader.getBase64Image()
            val prompt = uploader.getPrompt()
            
            if (base64Image != null) {
                val publisher = project.messageBus.syncPublisher(ComposeCraftNotifier.TOPIC)
                publisher.onImageUploaded(base64Image, prompt)
            }
        }
    }
}

/**
 * Action for clearing the conversation history.
 * Displays a garbage collection icon and triggers the clear history functionality.
 *
 * @property handler The function to execute when the action is performed
 */
class ClearHistoryAction(private val handler: () -> Unit) : AnAction(
    "Clear History",
    "Clear conversation history",
    AllIcons.Actions.GC
) {
    override fun actionPerformed(e: AnActionEvent) {
        handler()
    }
}

/**
 * Action for toggling between light and dark themes.
 * Displays a lightning icon and triggers the theme toggle functionality.
 *
 * @property handler The function to execute when the action is performed
 */
class ToggleThemeAction(private val handler: () -> Unit) : AnAction(
    "Toggle Theme",
    "Switch between light and dark theme",
    AllIcons.Actions.Lightning
) {
    override fun actionPerformed(e: AnActionEvent) {
        handler()
    }
}

/**
 * Action for toggling the preview panel visibility.
 * Displays a preview icon and triggers the preview toggle functionality.
 *
 * @property handler The function to execute when the action is performed
 */
class TogglePreviewAction(private val handler: () -> Unit) : AnAction(
    "Toggle Preview",
    "Show/Hide preview panel",
    AllIcons.Actions.PreviewDetails
) {
    override fun actionPerformed(e: AnActionEvent) {
        handler()
    }
}

/**
 * Action for uploading and processing images.
 * Displays an upload icon and triggers the image upload functionality.
 *
 * @property handler The function to execute when the action is performed
 */
class UploadImageAction(private val handler: () -> Unit) : AnAction(
    "Upload Image",
    "Upload and process an image",
    AllIcons.Actions.Upload
) {
    override fun actionPerformed(e: AnActionEvent) {
        handler()
    }
} 