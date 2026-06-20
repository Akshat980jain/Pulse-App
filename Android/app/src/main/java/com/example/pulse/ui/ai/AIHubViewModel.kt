package com.example.pulse.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.remote.ChatMessage
import com.example.pulse.data.remote.OpenRouterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AIAgent(
    val id: String,
    val name: String,
    val emoji: String,
    val tagline: String,
    val description: String,
    val systemPrompt: String,
    val model: String,
    val starters: List<String>
)

data class ChatBubble(
    val id: String,
    val role: String, // "user", "assistant"
    val content: String
)

@HiltViewModel
class AIHubViewModel @Inject constructor(
    private val openRouterService: OpenRouterService
) : ViewModel() {

    val agents = listOf(
        AIAgent(
            id = "techWizard",
            name = "Tech Wizard",
            emoji = "🧙‍♂️",
            tagline = "ENGINEER • DEBUGGER • ARCHITECT",
            description = "Write code, debug errors, explain complex concepts, and architect solutions across any tech stack.",
            systemPrompt = "You are Tech Wizard, an elite software engineer. Give concise, working code with markdown code blocks.",
            model = "google/gemini-2.0-flash-001",
            starters = listOf("Debug my React hook", "Explain async/await", "Design a REST API", "Write a SQL query")
        ),
        AIAgent(
            id = "creativeWriter",
            name = "Creative Writer",
            emoji = "✍️",
            tagline = "COPYWRITER • POET • STORYTELLER",
            description = "Craft viral captions, compelling stories, poetry, song lyrics, and unforgettable marketing copy.",
            systemPrompt = "You are Creative Writer, a master of social media copy and storytelling. Be vivid and engaging. Give only the content.",
            model = "anthropic/claude-3-haiku",
            starters = listOf("Viral Instagram caption", "Poem about city lights", "Story opening line", "Product tagline")
        ),
        AIAgent(
            id = "pulseHelper",
            name = "Pulse Helper",
            emoji = "💡",
            tagline = "GROWTH • STRATEGY • ANALYTICS",
            description = "Grow your audience, decode analytics, build content calendars, and master social media marketing.",
            systemPrompt = "You are Pulse Helper, a social media growth expert. Give practical, actionable advice.",
            model = "google/gemini-2.0-flash-001",
            starters = listOf("Grow my followers", "Best posting time", "Week content plan", "Hashtag strategy")
        ),
        AIAgent(
            id = "aiAssistant",
            name = "General AI",
            emoji = "🤖",
            tagline = "KNOWLEDGE • RESEARCH • CONVERSATION",
            description = "Your all-purpose AI — trivia, analysis, translation, summarization, advice, or great conversation.",
            systemPrompt = "You are a brilliant, knowledgeable, and warm AI assistant. Answer accurately and conversationally.",
            model = "google/gemini-2.0-flash-001",
            starters = listOf("Explain quantum physics", "Translate to Japanese", "Summarize a concept", "Recipe suggestions")
        )
    )

    private val _selectedAgent = MutableStateFlow<AIAgent?>(null)
    val selectedAgent: StateFlow<AIAgent?> = _selectedAgent.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatBubble>>(emptyList())
    val messages: StateFlow<List<ChatBubble>> = _messages.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    fun selectAgent(agent: AIAgent?) {
        _selectedAgent.value = agent
        _messages.value = if (agent != null) {
            listOf(
                ChatBubble(
                    id = "welcome",
                    role = "assistant",
                    content = "Hey! I'm **${agent.name}** — ${agent.description}\n\nHow can I help you today?"
                )
            )
        } else {
            emptyList()
        }
        _input.value = ""
    }

    fun setInput(value: String) {
        _input.value = value
    }

    fun sendMessage(text: String = _input.value.trim()) {
        val agent = _selectedAgent.value ?: return
        if (text.isEmpty() || _isStreaming.value) return
        
        _input.value = ""
        val userBubbleId = UUID.randomUUID().toString()
        val assistantBubbleId = UUID.randomUUID().toString()

        val newUserBubble = ChatBubble(id = userBubbleId, role = "user", content = text)
        val newAssistantBubble = ChatBubble(id = assistantBubbleId, role = "assistant", content = "")

        _messages.value = _messages.value + newUserBubble + newAssistantBubble
        _isStreaming.value = true

        viewModelScope.launch {
            // Map chat bubbles to Ktor client structure
            val history = _messages.value
                .filter { it.id != "welcome" && it.id != assistantBubbleId }
                .map { ChatMessage(role = it.role, content = it.content) }

            openRouterService.streamChat(
                messages = history,
                agentModel = agent.model,
                systemPrompt = agent.systemPrompt
            )
            .onStart {
                // Starting streaming
            }
            .onCompletion {
                _isStreaming.value = false
            }
            .collect { chunk ->
                // Update Assistant Bubble with new tokens
                _messages.value = _messages.value.map { bubble ->
                    if (bubble.id == assistantBubbleId) {
                        bubble.copy(content = bubble.content + chunk)
                    } else {
                        bubble
                    }
                }
            }
        }
    }

    fun clearChat() {
        val agent = _selectedAgent.value ?: return
        _messages.value = listOf(
            ChatBubble(
                id = "welcome",
                role = "assistant",
                content = "Chat cleared! How can I help you today?"
            )
        )
    }
}
