# AI Chat — Native Android Claude Client

A clean, fast Claude AI chat app for Android with real-time streaming.

## Features
- ✅ Real-time SSE streaming (token-by-token like Claude.ai)
- ✅ All Anthropic models (Opus, Sonnet, Haiku)
- ✅ API key encrypted storage
- ✅ Markdown rendering (code blocks, tables, bold, etc.)
- ✅ Conversation history persists across app restarts
- ✅ Stop streaming button
- ✅ Long press to copy any message
- ✅ System prompt support
- ✅ Dark / Light / System theme

## Setup

1. Get API key from [console.anthropic.com](https://console.anthropic.com)
2. Install APK → open app → Settings ⚙️ → enter API key → Save
3. Select model → Start chatting

## Build

Push to GitHub → Actions runs automatically → Download APK from Artifacts.

## Package
`com.ftt.aichat` | minSdk 26 (Android 8.0+)
