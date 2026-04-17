package com.ftt.aichat.repository

import android.content.Context
import android.content.SharedPreferences
import com.ftt.aichat.data.Message
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Saves and loads conversation history using SharedPreferences + JSON.
 * Max 200 messages stored to avoid large file sizes.
 */
class ChatRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "aichat_history"
        private const val KEY_MESSAGES = "messages"
        private const val MAX_STORED_MESSAGES = 200
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /** Load saved messages from disk. Returns empty list if none. */
    fun loadMessages(): List<Message> {
        val json = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Message>>() {}.type
            val messages: List<Message> = gson.fromJson(json, type)
            // Filter out any incomplete streaming messages from a crash
            messages.filter { !it.isStreaming }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Save messages to disk (trims to MAX_STORED_MESSAGES). */
    fun saveMessages(messages: List<Message>) {
        // Only save completed messages, trim if too many
        val toSave = messages
            .filter { !it.isStreaming }
            .takeLast(MAX_STORED_MESSAGES)
        val json = gson.toJson(toSave)
        prefs.edit().putString(KEY_MESSAGES, json).apply()
    }

    /** Clear all saved history. */
    fun clearMessages() {
        prefs.edit().remove(KEY_MESSAGES).apply()
    }
}
