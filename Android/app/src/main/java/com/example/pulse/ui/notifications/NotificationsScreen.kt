package com.example.pulse.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.pulse.data.local.ProfileEntity
import com.example.pulse.theme.CardBackground
import com.example.pulse.theme.PulseBlue
import com.example.pulse.theme.TextCharcoal
import com.example.pulse.theme.TextMuted
import com.example.pulse.theme.WarmBackground
import com.example.pulse.theme.warmCardStyle

private data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val userInitials: String,
    val body: String,
    val timestamp: String,
    val unread: Boolean = false
)

private enum class NotificationType {
    LIKE, FOLLOW, COMMENT, SYSTEM, MENTION
}

@Composable
fun NotificationsScreen(
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onNavigateToProfile: (String) -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val incomingRequests by viewModel.incomingRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Clean mock notifications list to show activity feed below requests
    val notifications = listOf(
        NotificationItem(
            id = "1",
            type = NotificationType.FOLLOW,
            title = "Sarah",
            userInitials = "SA",
            body = "started following you",
            timestamp = "2m ago",
            unread = true
        ),
        NotificationItem(
            id = "2",
            type = NotificationType.LIKE,
            title = "Liam",
            userInitials = "LI",
            body = "liked your post about Jetpack Compose",
            timestamp = "15m ago",
            unread = true
        ),
        NotificationItem(
            id = "3",
            type = NotificationType.COMMENT,
            title = "Mark",
            userInitials = "MK",
            body = "commented: \"Amazing styling! Looks super smooth.\"",
            timestamp = "1h ago"
        ),
        NotificationItem(
            id = "4",
            type = NotificationType.MENTION,
            title = "Priya",
            userInitials = "PR",
            body = "mentioned you in a post",
            timestamp = "3h ago"
        ),
        NotificationItem(
            id = "5",
            type = NotificationType.SYSTEM,
            title = "Pulse Assistant",
            userInitials = "AI",
            body = "Check out the AI Hub! Custom agents are ready to assist you.",
            timestamp = "1d ago"
        ),
        NotificationItem(
            id = "6",
            type = NotificationType.SYSTEM,
            title = "Pulse System",
            userInitials = "PS",
            body = "Welcome to Pulse! Build your network, post updates, and connect.",
            timestamp = "2d ago"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackground)
            .padding(innerPadding)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Incoming Follow Requests section
            if (incomingRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Follow Requests",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextCharcoal,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(incomingRequests, key = { it.userId }) { requester ->
                    FollowRequestRow(
                        requester = requester,
                        onConfirm = { viewModel.confirmRequest(requester.userId) },
                        onDelete = { viewModel.deleteRequest(requester.userId) },
                        onProfileClick = { onNavigateToProfile(requester.userId) }
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(TextMuted.copy(alpha = 0.15f))
                    )
                }
            }

            // General Activity header
            item {
                Text(
                    text = "Recent Activity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextCharcoal,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(notifications) { notification ->
                NotificationRow(notification = notification)
            }
        }
    }
}

@Composable
private fun FollowRequestRow(
    requester: ProfileEntity,
    onConfirm: () -> Unit,
    onDelete: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initials = (requester.displayName ?: requester.fullName ?: requester.username ?: "?")
        .take(2).uppercase()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .warmCardStyle(elevation = 2.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar click goes to profile
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PulseBlue.copy(alpha = 0.15f))
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            if (requester.avatarUrl != null) {
                AsyncImage(
                    model = requester.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = initials,
                    color = PulseBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Username / Bio click goes to profile
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onProfileClick() }
        ) {
            Text(
                text = requester.displayName ?: requester.fullName ?: requester.username ?: "User",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextCharcoal
            )
            Text(
                text = "@${requester.username ?: "user"}",
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        Spacer(Modifier.width(8.dp))

        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PulseBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E7EB)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Delete", color = TextCharcoal, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: NotificationItem,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (notification.type) {
        NotificationType.LIKE -> Icons.Default.Favorite to Color(0xFFEF4444)
        NotificationType.FOLLOW -> Icons.Default.PersonAdd to PulseBlue
        NotificationType.COMMENT -> Icons.AutoMirrored.Filled.Comment to Color(0xFF10B981)
        NotificationType.MENTION -> Icons.Default.AlternateEmail to Color(0xFF8B5CF6)
        NotificationType.SYSTEM -> Icons.Default.Info to Color(0xFFF59E0B)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .warmCardStyle(elevation = if (notification.unread) 3.dp else 1.dp)
            .clickable { /* Mark read or view */ },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PulseBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = notification.userInitials,
                    color = PulseBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(tint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = TextCharcoal)) {
                        append(notification.title)
                    }
                    append(" ")
                    append(notification.body)
                },
                fontSize = 14.sp,
                color = TextCharcoal
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = notification.timestamp,
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        if (notification.unread) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(PulseBlue)
            )
        }
    }
}
