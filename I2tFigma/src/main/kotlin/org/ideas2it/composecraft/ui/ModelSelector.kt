package org.ideas2it.composecraft.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import org.ideas2it.composecraft.models.AIModel
import org.ideas2it.composecraft.models.AIModelRegistry
import java.awt.FlowLayout
import javax.swing.DefaultComboBoxModel
import com.intellij.openapi.options.ShowSettingsUtil
import org.ideas2it.composecraft.settings.ComposeCraftSettingsConfigurable
import javax.swing.JButton
import com.intellij.icons.AllIcons
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import java.awt.Component

/**
 * A UI component for selecting AI models in the ComposeCraft plugin.
 * This component combines a dropdown menu for model selection with a settings button
 * for quick access to API configuration.
 *
 * Features:
 * - Dropdown menu showing available AI models
 * - Custom rendering of model names
 * - Quick access to API settings
 * - IntelliJ-style UI integration
 *
 * The component uses [JBPanel] as its base and follows IntelliJ's UI guidelines
 * for consistent look and feel across the IDE.
 *
 * @property comboBox The dropdown component for selecting AI models
 * @property settingsButton Quick access button to API configuration
 * @property project Reference to the current IntelliJ project (nullable)
 */
class ModelSelector : JBPanel<ModelSelector>() {
    private val comboBox: ComboBox<AIModel>
    private val settingsButton: JButton
    private var project: Project? = null

    /**
     * Initializes the ModelSelector component.
     * Sets up the layout, creates and configures the combo box and settings button.
     * The initialization process includes:
     * - Setting up a left-aligned FlowLayout
     * - Creating the model selection dropdown
     * - Adding a custom renderer for model display names
     * - Creating the settings button with a gear icon
     */
    init {
        layout = FlowLayout(FlowLayout.LEFT, 5, 0)
        border = JBUI.Borders.empty(2)

        // Create and configure the combo box
        comboBox = ComboBox<AIModel>().apply {
            model = DefaultComboBoxModel(AIModelRegistry.availableModels.toTypedArray())
            selectedItem = AIModelRegistry.getDefaultModel()
            
            // Add custom renderer to show only the display name
            renderer = object : DefaultListCellRenderer() {
                /**
                 * Custom renderer for the combo box items.
                 * Displays only the model's display name instead of the full object toString.
                 *
                 * @param list The JList being rendered
                 * @param value The value to render (expected to be an AIModel)
                 * @param index The cell index
                 * @param isSelected True if the cell is selected
                 * @param cellHasFocus True if the cell has focus
                 * @return The component used for rendering the cell
                 */
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    if (value is AIModel) {
                        text = value.displayName
                    }
                    return this
                }
            }
        }

        // Create settings button with gear icon
        settingsButton = JButton(AllIcons.General.Settings).apply {
            toolTipText = "Configure API Keys"
            isBorderPainted = false
            isContentAreaFilled = false
            addActionListener {
                project?.let { proj ->
                    ShowSettingsUtil.getInstance().showSettingsDialog(
                        proj,
                        ComposeCraftSettingsConfigurable::class.java
                    )
                }
            }
        }

        add(comboBox)
        add(settingsButton)
    }

    /**
     * Sets the project instance for this component.
     * The project reference is required for opening the settings dialog.
     *
     * @param project The current IntelliJ project instance
     */
    fun setProject(project: Project) {
        this.project = project
    }

    /**
     * Retrieves the currently selected AI model from the combo box.
     *
     * @return The selected [AIModel] instance
     * @throws ClassCastException if the selected item is not an AIModel
     */
    fun getSelectedAIModel(): AIModel = comboBox.selectedItem as AIModel

    /**
     * Adds an action listener to the model selection combo box.
     * The listener will be notified whenever the user selects a different model.
     *
     * @param listener The callback function to be invoked when the selection changes
     */
    fun addActionListener(listener: () -> Unit) {
        comboBox.addActionListener { listener() }
    }
} 