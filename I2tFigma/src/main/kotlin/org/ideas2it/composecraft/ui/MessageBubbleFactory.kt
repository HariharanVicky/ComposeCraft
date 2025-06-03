package org.ideas2it.composecraft.ui

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.ideas2it.composecraft.utils.FileMetadata
import org.ideas2it.composecraft.utils.DetectedFileStore
import org.ideas2it.composecraft.utils.CodeBlockAnalyzer
import org.ideas2it.composecraft.utils.FilePathResolver
import java.awt.*
import javax.swing.*
import java.awt.datatransfer.StringSelection

/**
 * Factory object for creating message bubble UI components in the ComposeCraft plugin.
 * This factory handles the creation of rich message bubbles that can display both text
 * and code content with appropriate styling and interactive features.
 *
 * Features:
 * - Rich text message rendering with Markdown support
 * - Syntax-highlighted code blocks
 * - Interactive code actions (copy, generate file)
 * - Sender identification and timestamps
 * - Theme-aware styling
 *
 * The factory follows IntelliJ's UI guidelines and automatically adapts to the current theme.
 */
object MessageBubbleFactory {
    /**
     * Creates a message bubble panel containing the message content and sender information.
     *
     * @param message The message content, which can include both text and code blocks (in markdown format)
     * @param sender The sender's identifier (e.g., "User" or "Assistant")
     * @param project The current IntelliJ project instance
     * @return A JPanel containing the formatted message bubble
     */
    fun createMessageBubble(message: String, sender: String, project: Project): JPanel {
        val bubblePanel = JPanel(BorderLayout(5, 5)).apply {
            isOpaque = false
            border = JBUI.Borders.empty(5)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val contentPanel = JPanel(BorderLayout(5, 5)).apply {
            isOpaque = false
            border = BorderFactory.createEmptyBorder(8, 12, 8, 12)
            maximumSize = Dimension(600, Int.MAX_VALUE)
        }

        val messageContent = if (message.contains("```")) {
            createCodeMessageContent(message, project)
        } else {
            createTextMessageContent(message)
        }

        contentPanel.add(messageContent, BorderLayout.CENTER)

        val headerPanel = createHeaderPanel(sender)
        val wrapperPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(headerPanel, BorderLayout.NORTH)
            add(contentPanel, BorderLayout.CENTER)
        }

        bubblePanel.add(wrapperPanel, BorderLayout.CENTER)
        return bubblePanel
    }

    /**
     * Creates a header panel containing sender information and timestamp.
     * The sender name is color-coded based on the sender type (User/Assistant).
     *
     * @param sender The sender's identifier
     * @return A JPanel containing the formatted header
     */
    private fun createHeaderPanel(sender: String): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            isOpaque = false
            
            add(JLabel(sender).apply {
                font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)
                foreground = if (sender == "User") {
                    JBColor(Color(0, 120, 212), Color(104, 180, 255))
                } else {
                    JBColor(Color(16, 137, 62), Color(35, 134, 54))
                }
            })
            
            add(JLabel("• ${getCurrentTime()}").apply {
                font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)
                foreground = JBColor.gray
            })
        }
    }

    /**
     * Creates a text content component with Markdown rendering support.
     * The text is rendered as HTML with proper styling and word wrapping.
     *
     * @param textPrompt The text content in Markdown format
     * @return A JComponent containing the formatted text
     */
    private fun createTextMessageContent(textPrompt: String): JComponent {
        val parser = Parser.builder().build()
        val document = parser.parse(textPrompt)
        val renderer = HtmlRenderer.builder().build()
        val html = renderer.render(document)

        return JEditorPane().apply {
            contentType = "text/html"
            isEditable = false
            isOpaque = false
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            text = """
                <html>
                    <body style='font-family: "Segoe UI"; font-size: 12px; margin: 5px;'>
                        <div style='word-wrap: break-word; white-space: pre-wrap;'>
                            $html
                        </div>
                    </body>
                </html>
            """.trimIndent()
            
            minimumSize = Dimension(100, 10)
            preferredSize = null
        }
    }

    /**
     * Creates a component that handles messages containing code blocks.
     * Splits the message into text and code segments and renders each appropriately.
     *
     * @param message The full message content containing code blocks
     * @param project The current IntelliJ project instance
     * @return A JComponent containing the formatted message with code blocks
     */
    private fun createCodeMessageContent(message: String, project: Project): JComponent {
        val mainPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
        }

        val parts = message.split("```")
        
        for (i in parts.indices) {
            if (i % 2 == 0) {
                if (parts[i].isNotBlank()) {
                    val textPanel = JPanel(BorderLayout()).apply {
                        isOpaque = false
                        border = JBUI.Borders.empty(5)
                    }
                    textPanel.add(createTextMessageContent(parts[i]), BorderLayout.CENTER)
                    mainPanel.add(textPanel)
                }
            } else {
                mainPanel.add(createCodeBlock(parts[i], project, if (i > 0) parts[i-1] else ""))
            }
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = false
            add(mainPanel, BorderLayout.CENTER)
        }
    }

    /**
     * Creates a code block panel with syntax highlighting and action buttons.
     *
     * @param codeContent The raw code content
     * @param project The current IntelliJ project instance
     * @param previousText The text content preceding this code block
     * @return A JPanel containing the formatted code block with actions
     */
    private fun createCodeBlock(codeContent: String, project: Project, previousText: String): JPanel {
        val codePanel = JPanel(BorderLayout()).apply {
            background = JBColor(Color(40, 44, 52), Color(43, 43, 43))
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

        val (language, code, suggestedFileName) = parseCodeContent(codeContent, previousText)
        val codeArea = createCodeArea(code)
        val scrollPane = JBScrollPane(codeArea).apply {
            border = BorderFactory.createEmptyBorder()
            minimumSize = Dimension(200, 50)
        }

        val buttonsPanel = createButtonsPanel(code, project, language, suggestedFileName)
        
        codePanel.add(buttonsPanel, BorderLayout.NORTH)
        codePanel.add(scrollPane, BorderLayout.CENTER)

        return codePanel
    }

    /**
     * Parses code content to extract language, actual code, and suggested filename.
     *
     * @param content The raw code block content
     * @param previousText The text content preceding the code block
     * @return Triple containing (language, code, suggestedFileName)
     */
    private fun parseCodeContent(content: String, previousText: String): Triple<String, String, String?> {
        val lines = content.trim().lines()
        val language = if (lines.isNotEmpty()) lines[0].trim() else ""
        val code = if (language.isNotEmpty()) {
            content.substring(language.length).trim()
        } else content.trim()

        var suggestedFileName: String? = null
        val headerRegex = Regex("""\*\*([\w_]+\.[a-zA-Z]+).*?\*\*""")
        val headerMatch = headerRegex.find(previousText)
        if (headerMatch != null) {
            suggestedFileName = headerMatch.groupValues[1]
        }

        return Triple(language, code, suggestedFileName)
    }

    /**
     * Creates a styled text area for displaying code content.
     *
     * @param code The code content to display
     * @return A JTextArea configured for code display
     */
    private fun createCodeArea(code: String): JTextArea {
        return JTextArea(code).apply {
            font = Font("JetBrains Mono", Font.PLAIN, 12)
            foreground = JBColor(Color(171, 178, 191), Color(169, 183, 198))
            background = JBColor(Color(40, 44, 52), Color(43, 43, 43))
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
    }

    /**
     * Creates a panel containing action buttons for code blocks.
     *
     * @param code The code content
     * @param project The current IntelliJ project instance
     * @param language The programming language of the code
     * @param suggestedFileName Optional suggested filename for code generation
     * @return A JPanel containing the action buttons
     */
    private fun createButtonsPanel(
        code: String,
        project: Project,
        language: String,
        suggestedFileName: String?
    ): JPanel {
        return JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0)).apply {
            isOpaque = false

            add(JButton("Copy", AllIcons.Actions.Copy).apply {
                addActionListener {
                    copyToClipboard(code, project)
                }
            })

            if (CodeBlockAnalyzer.canGenerateFile(code, language)) {
                add(JButton("Generate File", AllIcons.Actions.AddFile).apply {
                    addActionListener {
                        generateFile(code, language, suggestedFileName, project)
                    }
                })
            }
        }
    }

    /**
     * Copies code content to the system clipboard and shows a notification.
     *
     * @param content The content to copy
     * @param project The current IntelliJ project instance
     */
    private fun copyToClipboard(content: String, project: Project) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(content)
        clipboard.setContents(selection, selection)
        
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ComposeCraft Notifications")
            .createNotification(
                "Code copied to clipboard",
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    /**
     * Generates a new file from code content with user confirmation.
     * Shows a file name dialog and handles file creation through DetectedFileStore.
     *
     * @param code The code content for the file
     * @param language The programming language of the code
     * @param suggestedFileName Optional suggested filename
     * @param project The current IntelliJ project instance
     */
    private fun generateFile(code: String, language: String, suggestedFileName: String?, project: Project) {
        val (fileName, targetPath) = FilePathResolver.resolveFilePath(code, language, suggestedFileName, project)
        
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ComposeCraft Notifications")
            .createNotification(
                "Attempting to generate file: $fileName in $targetPath",
                NotificationType.INFORMATION
            )
            .notify(project)

        val metadata = FileMetadata(
            fileName = fileName,
            relativePath = targetPath,
            content = code,
            suggestedName = fileName,
            fileType = language,
            description = "Generated from code block"
        )

        val dialog = FileNameDialog(
            suggestedName = metadata.suggestedName,
            fileType = metadata.fileType,
            description = metadata.description
        )

        if (dialog.showAndGet()) {
            val confirmedFileName = dialog.getFileName()
            val updatedMetadata = metadata.copy(fileName = confirmedFileName)
            DetectedFileStore.save(project, updatedMetadata)
        }
    }

    /**
     * Gets the current time formatted as HH:mm.
     *
     * @return The current time as a formatted string
     */
    private fun getCurrentTime(): String {
        val now = java.time.LocalTime.now()
        return now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
} 