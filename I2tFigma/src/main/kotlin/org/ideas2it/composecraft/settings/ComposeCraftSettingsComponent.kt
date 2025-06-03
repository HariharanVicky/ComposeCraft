package org.ideas2it.composecraft.settings

import com.intellij.ui.components.JBPasswordField
import org.ideas2it.composecraft.models.AIModelRegistry
import org.ideas2it.composecraft.services.APIKeyService
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel

/**
 * UI component for managing ComposeCraft plugin settings.
 * This class provides a form-based interface for configuring API keys for different AI models
 * used in the plugin. It organizes the settings by AI provider and handles the secure storage
 * of API keys.
 *
 * Features:
 * - Grouped API key management by provider
 * - Secure password field for key entry
 * - Test functionality for API key validation
 * - Clear button for removing stored keys
 * - Automatic state management for modified settings
 *
 * @property project The current IntelliJ project instance
 * @property apiKeyService Service for managing API key storage
 * @property apiKeyFields Map of model names to their corresponding password fields
 * @property mainPanel The main settings panel containing all UI components
 */
class ComposeCraftSettingsComponent(private val project: Project) {
    private val apiKeyService = APIKeyService.getInstance(project)
    private val apiKeyFields = mutableMapOf<String, JBPasswordField>()
    private var mainPanel: DialogPanel

    /**
     * Initializes the settings component and creates the UI panel.
     * Sets up the form layout with grouped sections for each AI provider
     * and their respective models.
     */
    init {
        mainPanel = panel {
            group("API Keys Configuration") {
                row {
                    cell(JBLabel("Configure API keys for AI models used in Figma to Compose conversion."))
                }
                
                // Group models by provider
                val modelsByProvider = AIModelRegistry.availableModels.groupBy { it.provider }
                
                modelsByProvider.forEach { (provider, models) ->
                    group(provider.name) {
                        models.forEach { model ->
                            row("${model.displayName}:") {
                                val field = JBPasswordField().apply {
                                    apiKeyService.getApiKey(model)?.let { key ->
                                        text = key
                                    }
                                    putClientProperty("JPasswordField.cutCopyAllowed", true)
                                }
                                apiKeyFields[model.name] = field
                                cell(field)
                                    .align(AlignX.FILL)
                                
                                button("Test") {
                                    testApiKey(model.name, String(field.password))
                                }.enabled(field.password.isNotEmpty())
                                
                                button("Clear") {
                                    field.text = ""
                                    apiKeyService.clearApiKey(model)
                                }.enabled(field.password.isNotEmpty())
                            }
                        }
                    }
                }

                row {
                    comment("API keys are stored securely in your project settings and are used for converting Figma designs to Compose code.")
                }
            }
        }
    }

    /**
     * Tests the validity of an API key for a specific model.
     * Currently shows a placeholder message for future implementation.
     *
     * @param modelName The name of the AI model to test
     * @param apiKey The API key to validate
     */
    private fun testApiKey(modelName: String, apiKey: String) {
        // TODO: Implement actual API key testing
        Messages.showInfoMessage(
            project,
            "API key validation will be implemented in future updates.",
            "Test API Key"
        )
    }

    /**
     * Returns the main settings panel.
     * This panel contains all the UI components for API key management.
     *
     * @return The configured DialogPanel instance
     */
    fun getPanel(): DialogPanel = mainPanel

    /**
     * Checks if any settings have been modified.
     * Compares the current field values with the stored API keys
     * to determine if changes have been made.
     *
     * @return true if any API key has been modified, false otherwise
     */
    fun isModified(): Boolean {
        return apiKeyFields.any { (modelName, field) ->
            val model = AIModelRegistry.availableModels.find { it.name == modelName }
            model?.let {
                val existingKey = apiKeyService.getApiKey(it) ?: ""
                String(field.password) != existingKey
            } ?: false
        }
    }

    /**
     * Applies the current settings.
     * Saves all modified API keys to the secure storage.
     * Empty fields will result in the corresponding API key being cleared.
     */
    fun apply() {
        apiKeyFields.forEach { (modelName, field) ->
            val model = AIModelRegistry.availableModels.find { it.name == modelName }
            model?.let {
                val apiKey = String(field.password)
                if (apiKey.isNotEmpty()) {
                    apiKeyService.saveApiKey(it, apiKey)
                } else {
                    apiKeyService.clearApiKey(it)
                }
            }
        }
    }

    /**
     * Resets all fields to their last saved values.
     * Retrieves the stored API keys and updates the UI fields accordingly.
     */
    fun reset() {
        apiKeyFields.forEach { (modelName, field) ->
            val model = AIModelRegistry.availableModels.find { it.name == modelName }
            model?.let {
                field.text = apiKeyService.getApiKey(it) ?: ""
            }
        }
    }
} 