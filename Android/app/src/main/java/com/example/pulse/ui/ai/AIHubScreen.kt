package com.example.pulse.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulse.theme.CardBackground
import com.example.pulse.theme.PulseBlue
import com.example.pulse.theme.TextCharcoal
import com.example.pulse.theme.TextMuted
import com.example.pulse.theme.WarmBackground
import com.example.pulse.theme.warmCardStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIHubScreen(
    viewModel: AIHubViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedAgent by viewModel.selectedAgent.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val input by viewModel.input.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackground),
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedAgent?.name ?: "AI Hub",
                        fontWeight = FontWeight.Black,
                        color = PulseBlue,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedAgent != null) {
                            viewModel.selectAgent(null)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PulseBlue
                        )
                    }
                },
                actions = {
                    if (selectedAgent != null) {
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh, // acts as clear symbol
                                contentDescription = "Clear Chat",
                                tint = TextMuted
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WarmBackground
                )
            )
        },
        containerColor = WarmBackground,
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 640.dp)
            .fillMaxWidth()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val agent = selectedAgent
            if (agent == null) {
                // Dashboard Grid selection view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Intelligence Dashboard",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextCharcoal
                    )
                    Text(
                        text = "Select one of our specialized assistants to get started:",
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(viewModel.agents) { item ->
                            AgentSelectionCard(
                                agent = item,
                                onClick = { viewModel.selectAgent(item) }
                            )
                        }
                    }
                }
            } else {
                // Interactive Chat conversation View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Chat messages list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        items(messages, key = { it.id }) { msg ->
                            ChatBubbleItem(
                                bubble = msg,
                                agentEmoji = agent.emoji
                            )
                        }
                    }

                    // Bottom message input controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .warmCardStyle(elevation = 3.dp)
                    ) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { viewModel.setInput(it) },
                            placeholder = { Text("Message ${agent.name}...", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextCharcoal,
                                unfocusedTextColor = TextCharcoal,
                                focusedBorderColor = PulseBlue,
                                unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                focusedLabelColor = PulseBlue
                            ),
                            maxLines = 4,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            enabled = input.trim().isNotEmpty() && !isStreaming,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (input.trim().isNotEmpty() && !isStreaming) PulseBlue else PulseBlue.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun AgentSelectionCard(
    agent: AIAgent,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .warmCardStyle(elevation = 2.dp)
            .clickable(onClick = onClick)
            .height(200.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = agent.emoji,
                    fontSize = 28.sp
                )
                Icon(
                    imageVector = Icons.Default.Star, // acts as status light
                    contentDescription = "Active",
                    tint = PulseBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = agent.name,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = TextCharcoal
            )
            Text(
                text = agent.tagline,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = PulseBlue,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Text(
                text = agent.description,
                fontSize = 10.sp,
                color = TextMuted,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun ChatBubbleItem(
    bubble: ChatBubble,
    agentEmoji: String
) {
    val isUser = bubble.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PulseBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = agentEmoji, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) PulseBlue else CardBackground)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = bubble.content,
                color = if (isUser) Color.White else TextCharcoal,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
