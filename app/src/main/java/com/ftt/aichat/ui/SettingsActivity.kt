package com.ftt.aichat.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.ftt.aichat.R
import com.ftt.aichat.data.AVAILABLE_MODELS
import com.ftt.aichat.databinding.ActivitySettingsBinding
import com.ftt.aichat.utils.PreferencesManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        prefs = PreferencesManager(this)

        setupModelSpinner()
        setupThemeSelector()
        loadCurrentSettings()
        setupSaveButton()
    }

    private fun setupModelSpinner() {
        val modelNames = AVAILABLE_MODELS.map { "${it.displayName} — ${it.description}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerModel.adapter = adapter

        // Select current model
        val currentIndex = AVAILABLE_MODELS.indexOfFirst { it.id == prefs.selectedModelId }
        if (currentIndex != -1) binding.spinnerModel.setSelection(currentIndex)
    }

    private fun setupThemeSelector() {
        val themes = listOf(
            PreferencesManager.THEME_SYSTEM to "System Default",
            PreferencesManager.THEME_LIGHT to "Light",
            PreferencesManager.THEME_DARK to "Dark"
        )
        val themeNames = themes.map { it.second }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerTheme.adapter = adapter

        val currentThemeIndex = themes.indexOfFirst { it.first == prefs.theme }
        if (currentThemeIndex != -1) binding.spinnerTheme.setSelection(currentThemeIndex)
    }

    private fun loadCurrentSettings() {
        // Mask API key — show only last 6 chars for security
        val savedKey = prefs.apiKey
        if (savedKey.isNotBlank()) {
            val masked = "sk-ant-..." + savedKey.takeLast(6)
            binding.etApiKey.hint = masked
        } else {
            binding.etApiKey.hint = "sk-ant-api..."
        }

        // System prompt
        binding.etSystemPrompt.setText(prefs.systemPrompt)
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        var changed = false

        // Save API key if user entered one
        val newApiKey = binding.etApiKey.text?.toString()?.trim() ?: ""
        if (newApiKey.isNotBlank()) {
            if (!newApiKey.startsWith("sk-ant-")) {
                Toast.makeText(this, "API key should start with 'sk-ant-'", Toast.LENGTH_SHORT).show()
                return
            }
            prefs.apiKey = newApiKey
            changed = true
        }

        // Save model
        val selectedModelIndex = binding.spinnerModel.selectedItemPosition
        if (selectedModelIndex != -1) {
            prefs.selectedModelId = AVAILABLE_MODELS[selectedModelIndex].id
            changed = true
        }

        // Save system prompt
        val systemPrompt = binding.etSystemPrompt.text?.toString() ?: ""
        prefs.systemPrompt = systemPrompt
        changed = true

        // Save theme and apply
        val themeKeys = listOf(
            PreferencesManager.THEME_SYSTEM,
            PreferencesManager.THEME_LIGHT,
            PreferencesManager.THEME_DARK
        )
        val selectedThemeIndex = binding.spinnerTheme.selectedItemPosition
        if (selectedThemeIndex != -1) {
            val newTheme = themeKeys[selectedThemeIndex]
            if (prefs.theme != newTheme) {
                prefs.theme = newTheme
                applyTheme(newTheme)
            }
        }

        if (changed) {
            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
            // Clear API key field after save
            binding.etApiKey.text?.clear()
            loadCurrentSettings()
        }
    }

    private fun applyTheme(theme: String) {
        val mode = when (theme) {
            PreferencesManager.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            PreferencesManager.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            true
        } else super.onOptionsItemSelected(item)
    }
}
