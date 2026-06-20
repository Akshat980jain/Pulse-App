package com.example.pulse.ui.search

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.pulse.data.local.PostEntity
import com.example.pulse.data.local.ProfileEntity
import com.example.pulse.theme.CardBackground
import com.example.pulse.theme.PulseBlue
import com.example.pulse.theme.TextCharcoal
import com.example.pulse.theme.TextMuted
import com.example.pulse.theme.WarmBackground
import com.example.pulse.theme.warmCardStyle
import com.example.pulse.ui.feed.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    innerPadding: PaddingValues,
    onNavigateToProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    currentUserId: String? = null,
    pinnedPostId: String? = null,
    onDeletePost: (String) -> Unit = {},
    onLikeClick: (String) -> Unit = {},
    onCommentClick: (String) -> Unit = {},
    onEditCaption: (String, String) -> Unit = { _, _ -> },
    onPinToggle: (String?) -> Unit = {},
    onCommentsDisabledToggle: (PostEntity) -> Unit = {},
    onHidePost: (String) -> Unit = {},
    onUnfollowCreator: (String) -> Unit = {},
    onMuteCreator: (String) -> Unit = {},
    onReportPost: (String, String) -> Unit = { _, _ -> }
) {
    val query by viewModel.query.collectAsState()
    val userResults by viewModel.userResults.collectAsState()
    val postResults by viewModel.postResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackground)
            .padding(innerPadding)
    ) {
        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChanged,
                placeholder = { Text("Search creators or posts...", color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextMuted
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedBorderColor = PulseBlue,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextCharcoal,
                    unfocusedTextColor = TextCharcoal
                ),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.performSearch()
                        focusManager.clearFocus()
                    }
                )
            )
        }

        if (query.trim().isEmpty()) {
            // Trending & Suggestions
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = "Trending Topics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextCharcoal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                val trends = listOf(
                    "JetpackCompose" to "1,248 posts",
                    "Supabase" to "984 posts",
                    "AndroidKotlin" to "742 posts",
                    "OpenRouterAI" to "561 posts",
                    "UXDesign" to "329 posts"
                )

                items(trends) { (tag, postsCount) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .warmCardStyle(elevation = 1.dp)
                            .clickable {
                                viewModel.onQueryChanged(tag)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PulseBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = PulseBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "#$tag",
                                fontWeight = FontWeight.Bold,
                                color = TextCharcoal,
                                fontSize = 15.sp
                            )
                            Text(
                                text = postsCount,
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Tab row
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = WarmBackground,
                contentColor = PulseBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Creators", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Posts", fontWeight = FontWeight.Bold) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PulseBlue)
                }
            } else {
                error?.let { err ->
                    Text(
                        text = err,
                        color = Color(0xFFDC2626),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEE2E2))
                            .padding(16.dp)
                    )
                }

                if (selectedTab == 0) {
                    // Creators List
                    if (userResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No creators found", color = TextMuted)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(userResults) { profile ->
                                CreatorResultRow(profile = profile, onClick = {
                                    onNavigateToProfile(profile.userId)
                                })
                            }
                        }
                    }
                } else {
                    // Posts List
                    if (postResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No posts found", color = TextMuted)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(postResults, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    isMyPost = post.userId == currentUserId,
                                    onDeleteClick = { onDeletePost(post.id) },
                                    onLikeClick = { onLikeClick(post.id) },
                                    onCommentClick = { onCommentClick(post.id) },
                                    pinnedPostId = pinnedPostId,
                                    onEditCaption = { newContent -> onEditCaption(post.id, newContent) },
                                    onPinToggle = {
                                        val isPinned = post.id == pinnedPostId
                                        onPinToggle(if (isPinned) null else post.id)
                                    },
                                    onCommentsDisabledToggle = { onCommentsDisabledToggle(post) },
                                    onHidePost = { onHidePost(post.id) },
                                    onUnfollowCreator = { onUnfollowCreator(post.userId) },
                                    onMuteCreator = { onMuteCreator(post.userId) },
                                    onReportPost = { reason -> onReportPost(post.id, reason) },
                                    onAuthorClick = onNavigateToProfile
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorResultRow(
    profile: ProfileEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initials = (profile.displayName ?: profile.fullName ?: profile.username ?: "?")
        .take(2).uppercase()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .warmCardStyle(elevation = 2.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(PulseBlue.copy(alpha = 0.15f)),
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
                    fontSize = 16.sp
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.displayName ?: profile.fullName ?: "Anonymous",
                    fontWeight = FontWeight.Bold,
                    color = TextCharcoal,
                    fontSize = 15.sp
                )
                if (profile.isVerified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = PulseBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = "@${profile.username ?: "user"}",
                color = TextMuted,
                fontSize = 13.sp
            )
            profile.bio?.let { bio ->
                if (bio.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = bio,
                        color = TextCharcoal,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
