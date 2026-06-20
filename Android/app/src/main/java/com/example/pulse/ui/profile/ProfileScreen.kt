package com.example.pulse.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.pulse.data.local.ProfileEntity
import com.example.pulse.theme.CardBackground
import com.example.pulse.ui.feed.FeedViewModel
import com.example.pulse.ui.feed.PostCard
import com.example.pulse.theme.PulseBlue
import com.example.pulse.theme.TextCharcoal
import com.example.pulse.theme.TextMuted
import com.example.pulse.theme.WarmBackground
import com.example.pulse.theme.warmCardStyle
import com.example.pulse.ui.feed.PostCard

@Composable
fun ProfileScreen(
    innerPadding: PaddingValues,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
    currentProfile: ProfileEntity? = null, // fallback/inject from Shell if needed
    onCommentClick: (String) -> Unit = {}
) {
    val profileFromVm by viewModel.profileState.collectAsState()
    val posts by viewModel.userPosts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val editError by viewModel.editError.collectAsState()
    val followerCount by viewModel.followerCount.collectAsState()
    val followingCount by viewModel.followingCount.collectAsState()

    val profile = profileFromVm ?: currentProfile
    val feedViewModel: FeedViewModel = hiltViewModel()

    val sortedPosts = remember(posts, profile?.pinnedPostId) {
        val pinnedId = profile?.pinnedPostId
        if (pinnedId == null) {
            posts
        } else {
            val pinnedPost = posts.find { it.id == pinnedId }
            if (pinnedPost != null) {
                listOf(pinnedPost) + posts.filter { it.id != pinnedId }
            } else {
                posts
            }
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackground)
            .padding(innerPadding)
    ) {
        if (profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PulseBlue)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                // Header section: Cover + Avatar
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        // cover
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            PulseBlue,
                                            PulseBlue.copy(alpha = 0.6f)
                                        )
                                    )
                                )
                        )

                        // avatar overlapping cover
                        val initials = (profile.displayName ?: profile.fullName ?: profile.username ?: "?")
                            .take(2).uppercase()

                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 80.dp)
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(CardBackground)
                                .border(4.dp, CardBackground, CircleShape)
                                .border(2.dp, PulseBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile.avatarUrl != null) {
                                AsyncImage(
                                    model = profile.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = initials,
                                    color = PulseBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                )
                            }
                        }

                        // Edit Button on right
                        Button(
                            onClick = { showEditDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CardBackground,
                                contentColor = PulseBlue
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 4.dp)
                                .border(1.dp, PulseBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Profile Identity Info
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.displayName ?: profile.fullName ?: "No Name",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = TextCharcoal
                            )
                            if (profile.isVerified) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = PulseBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "@${profile.username ?: "user"}",
                            color = TextMuted,
                            fontSize = 15.sp
                        )

                        profile.bio?.let { bio ->
                            if (bio.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = bio,
                                    color = TextCharcoal,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                // Stats Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .warmCardStyle(elevation = 1.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatItem(count = posts.size.toString(), label = "Posts")
                        ProfileStatItem(count = followerCount.toString(), label = "Followers")
                        ProfileStatItem(count = followingCount.toString(), label = "Following")
                    }
                }

                // User's own Feed posts header
                item {
                    Text(
                        text = "My Posts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextCharcoal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                if (posts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "You haven't posted anything yet.\nShare your first update!",
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(sortedPosts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            isMyPost = true,
                            onDeleteClick = {
                                feedViewModel.deletePost(post.id)
                                viewModel.refreshProfileData()
                            },
                            onLikeClick = { feedViewModel.toggleLike(post.id) },
                            onCommentClick = { onCommentClick(post.id) },
                            pinnedPostId = profile.pinnedPostId,
                            onEditCaption = { newContent ->
                                feedViewModel.editPostCaption(post.id, newContent)
                            },
                            onPinToggle = {
                                val isPinned = post.id == profile.pinnedPostId
                                val newPinId = if (isPinned) null else post.id
                                feedViewModel.pinPostToProfile(newPinId) {
                                    viewModel.refreshProfileData()
                                }
                            },
                            onCommentsDisabledToggle = {
                                feedViewModel.toggleCommentsDisabled(post)
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditDialog && profile != null) {
        EditProfileDialog(
            profile = profile,
            error = editError,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, bio, avatar ->
                viewModel.editProfile(name, bio, avatar) {
                    showEditDialog = false
                }
            }
        )
    }
}

@Composable
internal fun ProfileStatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = TextCharcoal
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun EditProfileDialog(
    profile: ProfileEntity,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var displayName by remember { mutableStateOf(profile.displayName ?: "") }
    var bio by remember { mutableStateOf(profile.bio ?: "") }
    var avatarUrl by remember { mutableStateOf(profile.avatarUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Profile Details",
                fontWeight = FontWeight.Bold,
                color = TextCharcoal
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (error != null) {
                    Text(error, color = Color.Red, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextCharcoal,
                        unfocusedTextColor = TextCharcoal,
                        focusedBorderColor = PulseBlue,
                        focusedLabelColor = PulseBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextCharcoal,
                        unfocusedTextColor = TextCharcoal,
                        focusedBorderColor = PulseBlue,
                        focusedLabelColor = PulseBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    label = { Text("Avatar Image URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextCharcoal,
                        unfocusedTextColor = TextCharcoal,
                        focusedBorderColor = PulseBlue,
                        focusedLabelColor = PulseBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(displayName, bio, avatarUrl) }
            ) {
                Text("Save", color = PulseBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = CardBackground
    )
}
