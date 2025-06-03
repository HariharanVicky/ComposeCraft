package org.ideas2it.composecraft.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

/**
 * Configuration class for ComposeCraft plugin settings.
 * This class implements the IntelliJ [Configurable] interface to provide a settings UI
 * for managing API keys and other plugin configurations.
 *
 * Features:
 * - API key management for different AI models
 * - Settings persistence across IDE restarts
 * - Project-specific configuration
 * - Integration with IntelliJ's settings dialog
 *
 * @property project The current IntelliJ project instance
 * @property settingsComponent The UI component containing the settings form
 */
class ComposeCraftSettingsConfigurable(private val project: Project) : Configurable {
    private var settingsComponent: ComposeCraftSettingsComponent? = null

    /**
     * Returns the display name for this settings page.
     * This name appears in the Settings dialog sidebar.
     *
     * @return The name "ComposeCraft" as displayed in settings
     */
    override fun getDisplayName(): String = "ComposeCraft"

    /**
     * Creates and returns the settings component.
     * This method is called on-demand when the user opens the settings dialog.
     *
     * @return A JComponent containing the settings UI
     */
    override fun createComponent(): JComponent {
        settingsComponent = ComposeCraftSettingsComponent(project)
        return settingsComponent!!.getPanel()
    }

    /**
     * Checks if the settings have been modified.
     * This determines whether the Apply button should be enabled.
     *
     * @return true if settings were modified, false otherwise
     */
    override fun isModified(): Boolean {
        return settingsComponent?.isModified() ?: false
    }

    /**
     * Applies the settings changes.
     * Called when the user clicks Apply or OK in the settings dialog.
     * Persists the changes to the API key storage.
     */
    override fun apply() {
        settingsComponent?.apply()
    }

    /**
     * Resets the settings to their last saved state.
     * Called when the user clicks the Reset button or cancels the dialog.
     */
    override fun reset() {
        settingsComponent?.reset()
    }

    /**
     * Releases resources when the settings dialog is closed.
     * Cleans up the settings component to prevent memory leaks.
     */
    override fun disposeUIResources() {
        settingsComponent = null
    }
} 