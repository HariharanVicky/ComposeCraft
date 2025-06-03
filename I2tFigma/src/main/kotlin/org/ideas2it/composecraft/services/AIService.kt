package org.ideas2it.composecraft.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import org.ideas2it.composecraft.models.AIModel
import org.ideas2it.composecraft.models.AIModelRegistry
import org.ideas2it.composecraft.models.ModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.swing.Swing

@Service(Service.Level.PROJECT)
class AIService(private val project: Project) {
    private var currentModel: AIModel = AIModelRegistry.getDefaultModel()
    private val apiKeyService = APIKeyService.getInstance(project)
    
    fun setModel(model: AIModel) {
        currentModel = model
    }
    
    fun getCurrentModel(): AIModel = currentModel
    
    private fun ensureApiKey(model: AIModel): String {
        val existingKey = apiKeyService.getApiKey(model)
        if (!existingKey.isNullOrBlank()) {
            return existingKey
        }

        // Switch to EDT for showing dialog
        return invokeAndWaitIfNeeded {
            val apiKey = Messages.showInputDialog(
                project,
                "Enter API Key for ${model.displayName}",
                "API Key Required",
                Messages.getQuestionIcon()
            )

            if (apiKey.isNullOrBlank()) {
                throw IllegalStateException("API key is required for ${model.displayName}")
            }

            apiKeyService.saveApiKey(model, apiKey)
            apiKey
        }
    }
    
    suspend fun generateCode(prompt: String, imageData: String? = null): String {
        // Get API key on EDT if needed
        val apiKey = withContext(Dispatchers.Swing) {
            ensureApiKey(currentModel)
        }
        
        // Switch to IO for API calls
        return withContext(Dispatchers.IO) {
            when (currentModel.provider) {
                ModelProvider.GOOGLE -> {
                    if (imageData != null) {
                        GeminiService.generateWithImage(prompt, imageData, currentModel.name, apiKey)
                    } else {
                        GeminiService.generateJetpackComposeCode(prompt, currentModel.name, apiKey)
                    }
                }
                ModelProvider.OPENAI -> {
                    throw NotImplementedError("OpenAI implementation pending")
                }
                ModelProvider.ANTHROPIC -> {
                    throw NotImplementedError("Anthropic implementation pending")
                }
            }
        }
    }
    
    companion object {
        fun getInstance(project: Project): AIService =
            project.getService(AIService::class.java)
    }
}