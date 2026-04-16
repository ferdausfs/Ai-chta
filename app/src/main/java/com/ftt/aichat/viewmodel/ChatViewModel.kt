package com.ftt.aichat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ftt.aichat.api.ClaudeApiService
import com.ftt.aichat.data.Message
import com.ftt.aichat.data.AVAILABLE_MODELS
import com.ftt.aichat.data.DEFAULT_MODEL
import com.ftt.aichat.data.ModelOption
import com.ftt.aichat.utils.PreferencesManager
import okhttp3.Call

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val apiService = ClaudeApiService()

    // Active OkHttp streaming call (used for cancellation)
    private var activeCall: Call? = null

    // ── Observable State ─────────────────────────────────────────

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _isStreaming = MutableLiveData(false)
    val isStreaming: LiveData<Boolean> = _isStreaming

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _selectedModel = MutableLiveData(getCurrentModel())
    val selectedModel: LiveData<ModelOption> = _selectedModel

    // ── Getters for current settings ──────────────────────────────
    val hasApiKey: Boolean get() = prefs.hasApiKey
    val apiKey: String get() = prefs.apiKey

    // ── Chat Actions ─────────────────────────────────────────────

    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return
        if (_isStreaming.value == true) return
        if (!prefs.hasApiKey) {
            _error.value = "API key not set. Go to Settings."
            return
        }

        // 1. Add user message
        val userMsg = Message(role = Message.ROLE_USER, content = userInput.trim())
        val updatedMessages = _messages.value.orEmpty().toMutableList()
        updatedMessages.add(userMsg)

        // 2. Add empty streaming placeholder for assistant
        val streamingMsg = Message(role = Message.ROLE_ASSISTANT, content = "", isStreaming = true)
        updatedMessages.add(streamingMsg)
        _messages.postValue(updatedMessages.toList())
        _isStreaming.postValue(true)

        // 3. Stream from API
        activeCall = apiService.streamChat(
            apiKey = prefs.apiKey,
            model = prefs.selectedModelId,
            messages = updatedMessages.filter { !it.isStreaming },
            systemPrompt = prefs.systemPrompt.ifBlank { null },
            onToken = { token ->
                // Append token to the streaming message
                val current = _messages.value.orEmpty().toMutableList()
                val idx = current.indexOfLast { it.isStreaming }
                if (idx != -1) {
                    current[idx] = current[idx].copy(content = current[idx].content + token)
                    _messages.postValue(current.toList())
                }
            },
            onComplete = {
                // Finalize the streaming message
                val current = _messages.value.orEmpty().toMutableList()
                val idx = current.indexOfLast { it.isStreaming }
                if (idx != -1) {
                    current[idx] = current[idx].copy(isStreaming = false)
                    _messages.postValue(current.toList())
                }
                _isStreaming.postValue(false)
                activeCall = null
            },
            onError = { errorMsg ->
                // Remove streaming placeholder, show error
                val current = _messages.value.orEmpty().toMutableList()
                current.removeAll { it.isStreaming }
                // Add error message visually
                current.add(
                    Message(
                        role = Message.ROLE_ASSISTANT,
                        content = "⚠️ $errorMsg",
                        isError = true
                    )
                )
                _messages.postValue(current.toList())
                _isStreaming.postValue(false)
                _error.postValue(errorMsg)
                activeCall = null
            }
        )
    }

    fun stopStreaming() {
        activeCall?.cancel()
        // Finalize whatever was streamed so far
        val current = _messages.value.orEmpty().toMutableList()
        val idx = current.indexOfLast { it.isStreaming }
        if (idx != -1) {
            current[idx] = current[idx].copy(isStreaming = false)
            _messages.postValue(current.toList())
        }
        _isStreaming.postValue(false)
        activeCall = null
    }

    fun clearChat() {
        stopStreaming()
        _messages.value = emptyList()
        _error.value = null
    }

    fun clearError() {
        _error.value = null
    }

    // ── Settings ─────────────────────────────────────────────────

    fun refreshModel() {
        _selectedModel.value = getCurrentModel()
    }

    private fun getCurrentModel(): ModelOption {
        return AVAILABLE_MODELS.find { it.id == prefs.selectedModelId } ?: DEFAULT_MODEL
    }

    override fun onCleared() {
        super.onCleared()
        activeCall?.cancel()
    }
}
