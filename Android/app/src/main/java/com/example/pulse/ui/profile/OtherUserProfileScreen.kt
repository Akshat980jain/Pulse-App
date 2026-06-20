package com.example.pulse.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.pulse.theme.CardBackground
import com.example.pulse.theme.PulseBlue
import com.example.pulse.theme.TextCharcoal
import com.example.pulse.theme.TextMuted
import com.example.pulse.theme.WarmBackground
import com.example.pulse.theme.warmCardStyle
import com.example.pulse.ui.feed.PostCard

@Composable
fun OtherUserProfileScreen(
    targetUserId: String,
    currentUserId: String?,
    onBack: () -> Unit,
    onCommentClick: (String) -> Unit = {},
    onLikeClick: (String) -> Unit = {},
    viewModel: OtherUserProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(targetUserId) {
        viewModel.loadUser(targetUserId)
    }

    val profile       by viewModel.profile.collectAsState()
    val posts         by viewModel.posts.collectAsState()
    val followerCount by viewModel.followerCount.collectAsState()
    val followingCount by viewModel.followingCount.collectAsState()
    val isFollowedByMe by viewModel.isFollowedByMe.collectAsState()
    val isFollowingMe  by viewModel.isFollowingMe.collectAsState()
    val isFollowRequestPending by viewModel.isFollowRequestPending.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val error         by viewModel.error.collectAsState()

    val isOwnProfile = targetUserId == currentUserId
    val isPrivate = profile?.isPrivate ?: false
    val isLocked = isPrivate && !isFollowedByMe && !isOwnProfile

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {
        if (isLoading && profile == null) {
            CircularProgressIndicator(
                color    = PulseBlue,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Header: Cover + Avatar ──────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        // Cover gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(PulseBlue, PulseBlue.copy(alpha = 0.5f))
                                    )
                                )
                        ) {
                            if (profile?.coverUrl != null) {
                                AsyncImage(
                                    model              = profile!!.coverUrl,
                                    contentDescription = null,
                                    modifier           = Modifier.fillMaxSize(),
                                    contentScale       = ContentScale.Crop
                                )
                            }
                        }

                        // Back button
                        IconButton(
                            onClick  = onBack,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint               = Color.White,
                                modifier           = Modifier.size(20.dp)
                            )
                        }

                        // Avatar
                        val initials = (profile?.displayName ?: profile?.fullName ?: profile?.username ?: "?")
                            .take(2).uppercase()
                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 90.dp)
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(CardBackground)
                                .border(4.dp, CardBackground, CircleShape)
                                .border(2.dp, PulseBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile?.avatarUrl != null) {
                                AsyncImage(
                                    model              = profile!!.avatarUrl,
                                    contentDescription = null,
                                    modifier           = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale       = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text       = initials,
                                    color      = PulseBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 34.sp
                                )
                            }
                        }

                        // Follow / Unfollow button — top right, only if not own profile
                        if (!isOwnProfile) {
                            val buttonText = when {
                                isFollowedByMe -> "Following"
                                isFollowRequestPending -> "Requested"
                                isFollowingMe -> "Follow Back"
                                else -> "Follow"
                            }
                            val isOutlinedStyle = isFollowedByMe || isFollowRequestPending

                            Button(
                                onClick = { viewModel.toggleFollow() },
                                colors  = ButtonDefaults.buttonColors(
                                    containerColor = if (isOutlinedStyle) CardBackground else PulseBlue,
                                    contentColor   = if (isOutlinedStyle) PulseBlue      else Color.White
                                ),
                                shape    = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = 8.dp)
                                    .then(
                                        if (isOutlinedStyle) Modifier.border(1.dp, PulseBlue, RoundedCornerShape(50))
                                        else             Modifier
                                    )
                            ) {
                                Text(
                                    text       = buttonText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 14.sp
                                )
                            }
                        }
                    }
                }

                // ── Identity Info ───────────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text       = profile?.displayName ?: profile?.fullName ?: "Unknown",
                                fontWeight = FontWeight.Black,
                                fontSize   = 22.sp,
                                color      = TextCharcoal
                            )
                            if (profile?.isVerified == true) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector        = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint               = PulseBlue,
                                    modifier           = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text  = "@${profile?.username ?: "user"}",
                            color = TextMuted,
                            fontSize = 15.sp
                        )
                        profile?.bio?.takeIf { it.isNotEmpty() }?.let { bio ->
                            Spacer(Modifier.height(8.dp))
                            Text(text = bio, color = TextCharcoal, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }

                // ── Stats Row ───────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .warmCardStyle(elevation = 1.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatItem(count = posts.size.toString(),    label = "Posts")
                        ProfileStatItem(count = followerCount.toString(),  label = "Followers")
                        ProfileStatItem(count = followingCount.toString(), label = "Following")
                    }
                }

                // ── Posts Header ────────────────────────────────────────────
                item {
                    Text(
                        text       = "Posts",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = TextCharcoal,
                        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                if (isLocked) {
                    item {
                        PrivateAccountLockBlock()
                    }
                } else {
                    if (posts.isEmpty()) {
                        item {
                            Box(
                                modifier            = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment    = Alignment.Center
                            ) {
                                Text(
                                    text      = "No posts yet.",
                                    color     = TextMuted,
                                    textAlign = TextAlign.Center,
                                    fontSize  = 14.sp
                                )
                            }
                        }
                    } else {
                        items(posts, key = { it.id }) { post ->
                            PostCard(
                                post                    = post,
                                isMyPost                = false,
                                onDeleteClick           = {},
                                onLikeClick             = { onLikeClick(post.id) },
                                onCommentClick          = { onCommentClick(post.id) },
                                pinnedPostId            = null,
                                onEditCaption           = { _ -> },
                                onPinToggle             = {},
                                onCommentsDisabledToggle = {},
                                onHidePost              = {},
                                onUnfollowCreator       = {},
                                onMuteCreator           = {},
                                onReportPost            = { _ -> }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivateAccountLockBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .warmCardStyle(elevation = 2.dp)
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Private Account",
            tint = TextMuted,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "This Account is Private",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextCharcoal
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Follow this creator to see their posts and updates.",
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
