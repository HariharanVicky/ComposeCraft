package org.ideas2it.composecraft.services

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import org.ideas2it.composecraft.models.AIModel
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag

@State(
    name = "FigmaToComposeAPIKeys",
    storages = [Storage("figmaToCompose.xml")]
)
@Service(Service.Level.PROJECT)
class APIKeyService(private val project: Project) : PersistentStateComponent<APIKeyService.State> {
    
    class State {
        var apiKeys: MutableMap<String, String> = mutableMapOf()
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    companion object {
        fun getInstance(project: Project): APIKeyService =
            project.getService(APIKeyService::class.java)
    }

    fun getApiKey(model: AIModel): String? {
        return myState.apiKeys[getKeyForModel(model)]
    }

    fun saveApiKey(model: AIModel, apiKey: String) {
        if (apiKey.isBlank()) return
        myState.apiKeys[getKeyForModel(model)] = apiKey
    }

    private fun getKeyForModel(model: AIModel): String {
        return "${model.provider.name}-${model.name}"
    }

    fun clearApiKey(model: AIModel) {
        myState.apiKeys.remove(getKeyForModel(model))
    }

    fun getAllStoredModels(): List<String> {
        return myState.apiKeys.keys.toList()
    }
} 