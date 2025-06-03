package org.ideas2it.composecraft.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import javax.swing.JPanel
import javax.swing.JLabel
import javax.swing.BorderFactory
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Component
import org.ideas2it.composecraft.services.AIService

/**
 * Custom toolbar panel for the ComposeCraft plugin.
 * This panel provides a user interface for model selection and other toolbar actions.
 * It consists of two main components:
 * 1. A model selector dropdown on the left side
 * 2. A modern toolbar with additional actions in the center
 *
 * The panel uses BorderLayout for component arrangement and follows IntelliJ's theming guidelines.
 *
 * @property project The current IntelliJ project instance
 */
class ToolbarPanel(private val project: Project) : JPanel(BorderLayout()) {

    /**
     * Dropdown component for selecting AI models
     */
    private val modelSelector: ModelSelector = ModelSelector()

    /**
     * Custom toolbar component containing additional action buttons
     */
    private val toolbar: ModernToolbar = ModernToolbar(project)

    /**
     * Initializes the toolbar panel with required components and styling.
     * Sets up:
     * - Background color based on current theme
     * - Bottom border for visual separation
     * - Model selector configuration
     * - Component layout
     */
    init {
        // Apply theme-aware styling
        background = ThemeManager.toolbarBackground
        border = BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.dividerColor)
        
        // Initialize components
        setupModelSelector()
        add(createModelSelectorPanel(), BorderLayout.WEST)
        add(toolbar, BorderLayout.CENTER)
    }

    /**
     * Configures the model selector with project reference and event handling.
     * Sets up the action listener to update the AI service when a new model is selected.
     */
    private fun setupModelSelector() {
        // Associate with current project
        modelSelector.setProject(project)
        
        // Configure model change listener
        modelSelector.addActionListener {
            AIService.getInstance(project).setModel(modelSelector.getSelectedAIModel())
        }
    }

    /**
     * Creates a panel containing the model selector with appropriate styling.
     * The panel includes:
     * - A label indicating "Model:"
     * - The model selector dropdown
     * Both components are styled according to the current theme.
     *
     * @return JPanel containing the styled model selector components
     */
    private fun createModelSelectorPanel(): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            // Apply theme-aware background
            background = ThemeManager.toolbarBackground
            
            // Add and style the "Model:" label
            add(JLabel("Model:").apply {
                foreground = JBColor.foreground()
            })
            
            // Add the model selector component
            add(modelSelector as Component)
        }
    }

    /**
     * Provides access to the model selector component.
     * This method is used when external components need to interact with
     * or query the model selector.
     *
     * @return The ModelSelector instance used in this toolbar
     */
    fun getModelSelector(): ModelSelector = modelSelector
}