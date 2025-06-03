package org.ideas2it.composecraft.listener

import com.intellij.util.messages.Topic

interface ComposeCraftNotifier {
    companion object {
        val TOPIC = Topic.create("ComposeCraft Notifications", ComposeCraftNotifier::class.java)
    }

    fun onRefreshRequested()
    fun onHistoryCleared()
    fun onSettingsChanged()
    fun onPreviewToggled(isVisible: Boolean)
    fun onImageUploaded(base64Image: String, prompt: String)
}