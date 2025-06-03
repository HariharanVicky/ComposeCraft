package org.ideas2it.composecraft.models

enum class ModelProvider {
    OPENAI,
    GOOGLE,
    ANTHROPIC
}

data class AIModel(
    val name: String,
    val displayName: String,
    val provider: ModelProvider,
    val isAvailable: Boolean = true
)

object AIModelRegistry {
    val availableModels = listOf(
        // OpenAI Models
        AIModel("gpt-4-turbo-preview", "GPT-4 Turbo", ModelProvider.OPENAI),
        AIModel("gpt-4", "GPT-4", ModelProvider.OPENAI),
        AIModel("gpt-3.5-turbo", "GPT-3.5 Turbo", ModelProvider.OPENAI),
        
        // Google Models
        AIModel("gemini-2.0-flash", "Gemini 2.5", ModelProvider.GOOGLE),
        AIModel("gemini-pro", "Gemini Pro", ModelProvider.GOOGLE),
        AIModel("gemini-pro-vision", "Gemini Pro Vision", ModelProvider.GOOGLE),
        AIModel("palm2", "PaLM 2", ModelProvider.GOOGLE),
        
        // Anthropic Models
        AIModel("claude-3-opus", "Claude 3 Opus", ModelProvider.ANTHROPIC),
        AIModel("claude-3-sonnet", "Claude 3 Sonnet", ModelProvider.ANTHROPIC),
        AIModel("claude-2.1", "Claude 2.1", ModelProvider.ANTHROPIC)
    )

    fun getDefaultModel(): AIModel = availableModels.first { it.name == "gemini-2.0-flash" }
} 