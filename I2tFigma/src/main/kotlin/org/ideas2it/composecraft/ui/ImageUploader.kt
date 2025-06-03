package org.ideas2it.composecraft.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import java.util.Base64
import kotlin.math.min

/**
 * A dialog for uploading and processing images in the ComposeCraft plugin.
 * This class provides a user interface for selecting images, previewing them,
 * and optionally adding instructions for image processing.
 *
 * Features:
 * - Image file selection with format filtering (jpg, jpeg, png)
 * - Image preview with automatic scaling
 * - Optional text instructions input
 * - Base64 image conversion for API compatibility
 * - Error handling and user feedback
 *
 * The dialog follows IntelliJ's UI guidelines and integrates with the
 * platform's theme and styling.
 *
 * @property project The current IntelliJ project instance
 * @property selectedImage The currently selected image in BufferedImage format
 * @property base64Image The selected image converted to base64 format
 * @property imageLabel Label component for displaying the image preview
 * @property promptField Text field for additional processing instructions
 */
class ImageUploader(private val project: Project) : DialogWrapper(project) {
    private var selectedImage: BufferedImage? = null
    private var base64Image: String? = null
    private val imageLabel = JBLabel()
    private val promptField = JTextField(40)
    
    init {
        title = "Upload Image"
        init()
    }

    /**
     * Creates the main content panel of the dialog.
     * Sets up the UI layout with image preview, file selection,
     * and instruction input areas.
     *
     * The panel consists of:
     * - A preview area showing the selected image
     * - A file selection button
     * - An optional instructions text field
     *
     * @return The configured content panel
     */
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        panel.border = JBUI.Borders.empty(10)

        // Image preview area
        val previewPanel = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(400, 300)
            border = BorderFactory.createTitledBorder("Image Preview")
            
            val defaultLabel = JBLabel("No image selected", SwingConstants.CENTER).apply {
                icon = AllIcons.General.Add
            }
            add(defaultLabel, BorderLayout.CENTER)
        }

        // Image selection button
        val selectButton = JButton("Select Image").apply {
            addActionListener {
                selectImage(previewPanel)
            }
        }

        // Prompt input area
        val promptPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createTitledBorder("Additional Instructions (Optional)")
            add(promptField, BorderLayout.CENTER)
        }

        // Layout components
        panel.add(previewPanel, BorderLayout.CENTER)
        
        val bottomPanel = JPanel(BorderLayout(5, 5)).apply {
            add(selectButton, BorderLayout.NORTH)
            add(promptPanel, BorderLayout.CENTER)
        }
        panel.add(bottomPanel, BorderLayout.SOUTH)

        return panel
    }

    /**
     * Handles the image selection process.
     * Opens a file chooser dialog filtered for image files,
     * loads the selected image, and updates the preview.
     *
     * @param previewPanel The panel where the image preview will be displayed
     */
    private fun selectImage(previewPanel: JPanel) {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Select an Image"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false
            fileFilter = FileNameExtensionFilter("Image files", "jpg", "jpeg", "png")
        }

        if (fileChooser.showOpenDialog(previewPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                val file = fileChooser.selectedFile
                selectedImage = ImageIO.read(file)
                
                if (selectedImage != null) {
                    // Convert to base64
                    base64Image = convertImageToBase64(file)
                    
                    // Update preview
                    updatePreview(previewPanel)
                }
            } catch (e: Exception) {
                showError("Failed to load image: ${e.message}")
            }
        }
    }

    /**
     * Updates the preview panel with the selected image.
     * Scales the image to fit the preview area while maintaining aspect ratio.
     *
     * @param previewPanel The panel to update with the scaled image
     */
    private fun updatePreview(previewPanel: JPanel) {
        selectedImage?.let { image ->
            // Scale image to fit preview area while maintaining aspect ratio
            val maxWidth = 380
            val maxHeight = 280
            val scaledImage = scaleImage(image, maxWidth, maxHeight)
            
            // Update preview
            imageLabel.icon = ImageIcon(scaledImage)
            
            previewPanel.removeAll()
            previewPanel.add(JBScrollPane(imageLabel), BorderLayout.CENTER)
            previewPanel.revalidate()
            previewPanel.repaint()
        }
    }

    /**
     * Scales an image to fit within specified dimensions while maintaining aspect ratio.
     *
     * @param image The image to scale
     * @param maxWidth Maximum width for the scaled image
     * @param maxHeight Maximum height for the scaled image
     * @return The scaled image
     */
    private fun scaleImage(image: BufferedImage, maxWidth: Int, maxHeight: Int): Image {
        val widthRatio = maxWidth.toDouble() / image.width
        val heightRatio = maxHeight.toDouble() / image.height
        val ratio = min(widthRatio, heightRatio)
        
        val newWidth = (image.width * ratio).toInt()
        val newHeight = (image.height * ratio).toInt()
        
        return image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
    }

    /**
     * Converts an image file to base64 string format.
     *
     * @param file The image file to convert
     * @return Base64 encoded string of the image
     */
    private fun convertImageToBase64(file: File): String {
        return Base64.getEncoder().encodeToString(file.readBytes())
    }

    /**
     * Displays an error message dialog to the user.
     *
     * @param message The error message to display
     */
    private fun showError(message: String) {
        JOptionPane.showMessageDialog(
            null,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        )
    }

    /**
     * Gets the base64-encoded string of the selected image.
     *
     * @return The base64 string or null if no image is selected
     */
    fun getBase64Image(): String? = base64Image

    /**
     * Gets the user-provided processing instructions.
     *
     * @return The trimmed text from the prompt field
     */
    fun getPrompt(): String = promptField.text.trim()
} 