package com.example.pulse.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pulse.data.local.PostEntity
import com.example.pulse.data.local.ProfileEntity
import com.example.pulse.theme.CardBackground
import com.example.pulse.theme.PulseBlue
import com.example.pulse.theme.TextCharcoal
import com.example.pulse.theme.TextMuted
import com.example.pulse.theme.WarmBackground
import com.example.pulse.theme.BorderWarm
import com.example.pulse.theme.warmCardStyle
import com.example.pulse.ui.search.SearchScreen
import com.example.pulse.ui.notifications.NotificationsScreen
import com.example.pulse.ui.profile.ProfileScreen
import com.example.pulse.ui.profile.OtherUserProfileScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.CommentsDisabled
import androidx.compose.material.icons.filled.Warning

import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Close
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.painterResource
import com.example.pulse.data.remote.StoryUiModel
import com.example.pulse.data.remote.CommentUiModel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.media.MediaMetadataRetriever
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout

// ---------------------------------------------------------------------------
// Bottom nav item model
// ---------------------------------------------------------------------------
private data class NavItem(val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    NavItem("Home",          Icons.Default.Home),
    NavItem("Search",        Icons.Default.Search),
    NavItem("New Post",      Icons.Default.Add),
    NavItem("Notifications", Icons.Default.Notifications),
    NavItem("Profile",       Icons.Default.Person),
)

// ---------------------------------------------------------------------------
// FeedScreen root / Tab Container
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onNavigateToAIHub: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val posts             by viewModel.posts.collectAsState()
    val isRefreshing      by viewModel.isRefreshing.collectAsState()
    val newPostContent    by viewModel.newPostContent.collectAsState()
    val error             by viewModel.error.collectAsState()
    val currentProfile    by viewModel.currentProfile.collectAsState()
    val suggestedProfiles by viewModel.suggestedProfiles.collectAsState()
    val activeStories     by viewModel.activeStories.collectAsState()
    val comments          by viewModel.comments.collectAsState()
    val currentUserId      = viewModel.currentUserId
    val feedTab           by viewModel.feedTab.collectAsState()

    // Compute display initials from the real profile, fall back to userId prefix
    val myInitials = currentProfile?.let {
        (it.displayName ?: it.fullName ?: it.username)?.take(2)?.uppercase()
    } ?: currentUserId?.take(2)?.uppercase() ?: "Me"

    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var showNewPostSheet  by remember { mutableStateOf(false) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    var selectedStoriesToView by remember { mutableStateOf<List<StoryUiModel>?>(null) }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    // userId of another user whose profile should be shown as an overlay (null = no overlay)
    var viewedUserId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackground),
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            containerColor = WarmBackground,
        topBar = {
            val title = when (selectedNavIndex) {
                0 -> "Pulse"
                1 -> "Search"
                3 -> "Notifications"
                4 -> "Profile"
                else -> "Pulse"
            }
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedNavIndex == 0) {
                            Image(
                                painter = painterResource(id = com.example.pulse.R.drawable.logo_pulse),
                                contentDescription = "Pulse Logo",
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Icon(
                                imageVector = when (selectedNavIndex) {
                                    1 -> Icons.Default.Search
                                    3 -> Icons.Default.Notifications
                                    4 -> Icons.Default.Person
                                    else -> Icons.Default.Home
                                },
                                contentDescription = null,
                                tint = PulseBlue,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(if (selectedNavIndex == 0) 6.dp else 8.dp))
                        Text(
                            text = title,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = PulseBlue
                        )
                    }
                },
                actions = {
                    if (selectedNavIndex == 0) {
                        IconButton(onClick = { selectedNavIndex = 1 }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextCharcoal)
                        }
                        IconButton(onClick = { selectedNavIndex = 3 }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = TextCharcoal)
                        }
                    }
                    if (selectedNavIndex == 4) {
                        // Sign out icon on profile
                        IconButton(onClick = onSignOut) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = TextCharcoal
                            )
                        }
                    }
                    // Avatar circle — shows real photo or initials, taps to AI Hub
                    if (selectedNavIndex != 4) {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PulseBlue.copy(alpha = 0.15f))
                                .border(2.dp, PulseBlue, CircleShape)
                                .clickable(onClick = onNavigateToAIHub),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentProfile?.avatarUrl != null) {
                                AsyncImage(
                                    model              = currentProfile!!.avatarUrl,
                                    contentDescription = "My avatar",
                                    modifier           = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale       = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text       = myInitials,
                                    color      = PulseBlue,
                                    fontWeight = FontWeight.Black,
                                    fontSize   = 12.sp
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmBackground)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CardBackground,
                tonalElevation = 4.dp
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected  = selectedNavIndex == index,
                        onClick   = {
                            if (index == 2) showNewPostSheet = true
                            else selectedNavIndex = index
                        },
                        icon      = {
                            if (index == 2) {
                                // "+" centre pill
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PulseBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(item.icon, contentDescription = item.label, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            } else {
                                Icon(item.icon, contentDescription = item.label)
                            }
                        },
                        label     = { if (index != 2) Text(item.label, fontSize = 10.sp) },
                        colors    = NavigationBarItemDefaults.colors(
                            selectedIconColor   = PulseBlue,
                            selectedTextColor   = PulseBlue,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor      = PulseBlue.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 640.dp)
            .fillMaxWidth()
    ) { innerPadding ->

        when (selectedNavIndex) {
            0 -> {
                HomeScreenContent(
                    posts = posts,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refreshFeed,
                    suggestedProfiles = suggestedProfiles,
                    activeStories = activeStories,
                    myInitials = myInitials,
                    currentUserAvatar = currentProfile?.avatarUrl,
                    currentUserId = currentUserId,
                    onDeletePost = viewModel::deletePost,
                    error = error,
                    innerPadding = innerPadding,
                    onCreateStoryClick = { showCreateStoryDialog = true },
                    onStoryClick = { stories -> selectedStoriesToView = stories },
                    onLikeClick = { postId -> viewModel.toggleLike(postId) },
                    onCommentClick = { postId ->
                        activeCommentPostId = postId
                        viewModel.fetchComments(postId)
                    },
                    pinnedPostId = currentProfile?.pinnedPostId,
                    onEditCaption = { postId, newContent ->
                        viewModel.editPostCaption(postId, newContent)
                    },
                    onPinToggle = { pinnedId ->
                        viewModel.pinPostToProfile(pinnedId)
                    },
                    onCommentsDisabledToggle = { post ->
                        viewModel.toggleCommentsDisabled(post)
                    },
                    onHidePost = { postId ->
                        viewModel.hidePost(postId)
                    },
                    onUnfollowCreator = { creatorId ->
                        viewModel.unfollowCreator(creatorId)
                    },
                    onMuteCreator = { creatorId ->
                        viewModel.muteCreator(creatorId)
                    },
                    onReportPost = { postId, reason ->
                        viewModel.reportPost(postId, reason)
                    },
                    feedTab = feedTab,
                    onFeedTabChange = { tab -> viewModel.setFeedTab(tab) },
                    onAuthorClick = { userId ->
                        if (userId == currentUserId) {
                            selectedNavIndex = 4
                        } else {
                            viewedUserId = userId
                        }
                    }
                )
            }
            1 -> {
                SearchScreen(
                    innerPadding = innerPadding,
                    onNavigateToProfile = { userId ->
                        // If tapping own profile, go to the Profile tab
                        // Otherwise show the other user's profile as an overlay
                        if (userId == currentUserId) {
                            selectedNavIndex = 4
                        } else {
                            viewedUserId = userId
                        }
                    },
                    currentUserId = currentUserId,
                    pinnedPostId = currentProfile?.pinnedPostId,
                    onDeletePost = viewModel::deletePost,
                    onLikeClick = { postId -> viewModel.toggleLike(postId) },
                    onCommentClick = { postId ->
                        activeCommentPostId = postId
                        viewModel.fetchComments(postId)
                    },
                    onEditCaption = { postId, newContent ->
                        viewModel.editPostCaption(postId, newContent)
                    },
                    onPinToggle = { pinnedId ->
                        viewModel.pinPostToProfile(pinnedId)
                    },
                    onCommentsDisabledToggle = { post ->
                        viewModel.toggleCommentsDisabled(post)
                    },
                    onHidePost = { postId ->
                        viewModel.hidePost(postId)
                    },
                    onUnfollowCreator = { creatorId ->
                        viewModel.unfollowCreator(creatorId)
                    },
                    onMuteCreator = { creatorId ->
                        viewModel.muteCreator(creatorId)
                    },
                    onReportPost = { postId, reason ->
                        viewModel.reportPost(postId, reason)
                    }
                )
            }
            3 -> {
                NotificationsScreen(
                    innerPadding = innerPadding,
                    onNavigateToProfile = { userId ->
                        if (userId == currentUserId) {
                            selectedNavIndex = 4
                        } else {
                            viewedUserId = userId
                        }
                    }
                )
            }
            4 -> {
                ProfileScreen(
                    innerPadding = innerPadding,
                    onSignOut = onSignOut,
                    currentProfile = currentProfile,
                    onCommentClick = { postId ->
                        activeCommentPostId = postId
                        viewModel.fetchComments(postId)
                    }
                )
            }
        }

        // ── Floating new-post sheet ──────────────────────────────────────
        if (showNewPostSheet) {
            NewPostBottomSheet(
                content       = newPostContent,
                onContentChange = viewModel::setNewPostContent,
                onPost        = {
                    viewModel.submitPost()
                    showNewPostSheet = false
                },
                onDismiss     = { showNewPostSheet = false }
            )
        }

        // ── Ephemeral Story Dialogs ──────────────────────────────────────
        if (showCreateStoryDialog) {
            CreateStoryDialog(
                onDismiss = { showCreateStoryDialog = false },
                onPublish = { content, bytes, mimeType ->
                    viewModel.publishStory(content, bytes, mimeType)
                }
            )
        }

        selectedStoriesToView?.let { stories ->
            StoryViewerDialog(
                stories = stories,
                onClose = { selectedStoriesToView = null }
            )
        }

        activeCommentPostId?.let { postId ->
            CommentsBottomSheet(
                postId = postId,
                comments = comments,
                onSubmitComment = { content ->
                    viewModel.submitComment(postId, content)
                },
                onDismiss = {
                    activeCommentPostId = null
                }
            )
        }
        }

        // ── Other User Profile Overlay (Instagram-style) ─────────────────
        // Shown when user taps a creator in Search or a post author anywhere
        viewedUserId?.let { uid ->
            OtherUserProfileScreen(
                targetUserId  = uid,
                currentUserId = currentUserId,
                onBack        = { viewedUserId = null },
                onLikeClick   = { postId -> viewModel.toggleLike(postId) },
                onCommentClick = { postId ->
                    activeCommentPostId = postId
                    viewModel.fetchComments(postId)
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Home Screen Feed Content
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    posts: List<PostEntity>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    suggestedProfiles: List<ProfileEntity>,
    activeStories: List<StoryUiModel>,
    myInitials: String,
    currentUserAvatar: String?,
    currentUserId: String?,
    onDeletePost: (String) -> Unit,
    error: String?,
    innerPadding: PaddingValues,
    onCreateStoryClick: () -> Unit,
    onStoryClick: (List<StoryUiModel>) -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    pinnedPostId: String? = null,
    onEditCaption: (String, String) -> Unit = { _, _ -> },
    onPinToggle: (String?) -> Unit = {},
    onCommentsDisabledToggle: (PostEntity) -> Unit = {},
    onHidePost: (String) -> Unit = {},
    onUnfollowCreator: (String) -> Unit = {},
    onMuteCreator: (String) -> Unit = {},
    onReportPost: (String, String) -> Unit = { _, _ -> },
    feedTab: String = "following",
    onFeedTabChange: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {}
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh    = onRefresh,
        modifier     = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        LazyColumn(
            contentPadding      = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier            = Modifier.fillMaxSize()
        ) {
            error?.let { err ->
                item {
                    Text(
                        text     = err,
                        color    = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEE2E2))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // ── Following / Discover tab pills ──────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("following" to "Following", "discover" to "Discover").forEach { (key, label) ->
                        val selected = feedTab == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) PulseBlue else PulseBlue.copy(alpha = 0.08f)
                                )
                                .clickable { onFeedTabChange(key) }
                                .padding(horizontal = 18.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = label,
                                color      = if (selected) Color.White else PulseBlue,
                                fontSize   = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            item {
                StoriesStrip(
                    currentUserInitials = myInitials,
                    currentUserAvatar = currentUserAvatar,
                    activeStories = activeStories,
                    currentUserId = currentUserId,
                    onCreateStoryClick = onCreateStoryClick,
                    onStoryClick = onStoryClick
                )
            }

            if (posts.isEmpty() && !isRefreshing) {
                item { EmptyFeedHint() }
            }

            items(posts, key = { it.id }) { post ->
                PostCard(
                    post          = post,
                    isMyPost      = post.userId == currentUserId,
                    onDeleteClick = { onDeletePost(post.id) },
                    onLikeClick   = { onLikeClick(post.id) },
                    onCommentClick = { onCommentClick(post.id) },
                    pinnedPostId  = pinnedPostId,
                    onEditCaption = { newContent -> onEditCaption(post.id, newContent) },
                    onPinToggle   = {
                        val isPinned = post.id == pinnedPostId
                        onPinToggle(if (isPinned) null else post.id)
                    },
                    onCommentsDisabledToggle = { onCommentsDisabledToggle(post) },
                    onHidePost    = { onHidePost(post.id) },
                    onUnfollowCreator = { onUnfollowCreator(post.userId) },
                    onMuteCreator = { onMuteCreator(post.userId) },
                    onReportPost  = { reason -> onReportPost(post.id, reason) },
                    onAuthorClick = onAuthorClick
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Stories strip
// ---------------------------------------------------------------------------
@Composable
private fun StoriesStrip(
    currentUserInitials: String,
    currentUserAvatar: String?,
    activeStories: List<StoryUiModel>,
    currentUserId: String?,
    onCreateStoryClick: () -> Unit,
    onStoryClick: (List<StoryUiModel>) -> Unit
) {
    val groupedStories = remember(activeStories) { activeStories.groupBy { it.userId } }

    LazyRow(
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // "You+" — add story
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onCreateStoryClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUserAvatar != null) {
                        AsyncImage(
                            model = currentUserAvatar,
                            contentDescription = "My avatar",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(currentUserInitials, fontWeight = FontWeight.Bold, color = TextCharcoal, fontSize = 16.sp)
                    }
                    // + badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(PulseBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Your story", fontSize = 11.sp, color = TextMuted)
            }
        }

        items(groupedStories.entries.toList(), key = { it.key }) { (userId, userStories) ->
            val firstStory = userStories.first()
            val name = if (userId == currentUserId) "You" else firstStory.displayName ?: firstStory.username ?: "User"
            val initials = name.take(2).uppercase()
            val gradientColors = listOf(
                PulseBlue,
                PulseBlue.copy(alpha = 0.6f),
                Color(0xFFFCB045)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onStoryClick(userStories) }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(gradientColors)
                        )
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFE5E7EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (firstStory.avatarUrl != null) {
                            AsyncImage(
                                model = firstStory.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(initials, fontWeight = FontWeight.Bold, color = TextCharcoal, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = name,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(64.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Edit Caption Dialog
// ---------------------------------------------------------------------------
@Composable
fun EditCaptionDialog(
    initialContent: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialContent) }
    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Card(
            shape = RoundedCornerShape(24.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = androidx.compose.ui.Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Edit Caption",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextCharcoal
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextCharcoal,
                        fontSize = 14.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextCharcoal,
                        unfocusedTextColor = TextCharcoal,
                        cursorColor = PulseBlue,
                        focusedBorderColor = PulseBlue,
                        unfocusedBorderColor = BorderWarm
                    ),
                    placeholder = { Text("Write your caption...", color = TextMuted) }
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(text) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PulseBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Post card
// ---------------------------------------------------------------------------
@Composable
fun PostCard(
    post: PostEntity,
    isMyPost: Boolean,
    onDeleteClick: () -> Unit,
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    pinnedPostId: String? = null,
    onEditCaption: (String) -> Unit = {},
    onPinToggle: () -> Unit = {},
    onCommentsDisabledToggle: () -> Unit = {},
    onHidePost: () -> Unit = {},
    onUnfollowCreator: () -> Unit = {},
    onMuteCreator: () -> Unit = {},
    onReportPost: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {}
) {
    var isSaved by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .warmCardStyle(elevation = 2.dp)
    ) {
        if (post.id == pinnedPostId) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = PulseBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Pinned",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PulseBlue
                )
            }
        }

        // ── Header ────────────────────────────────────────────────────────
        Row(
            verticalAlignment   = Alignment.CenterVertically,
            modifier            = Modifier.fillMaxWidth()
        ) {
            // Clickable avatar + name/username group (Instagram-style)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAuthorClick(post.userId) }
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PulseBlue.copy(alpha = 0.12f))
                        .border(2.dp, PulseBlue.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.avatarUrl != null) {
                        AsyncImage(
                            model             = post.avatarUrl,
                            contentDescription = "Avatar",
                            modifier          = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale      = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text          = (post.displayName ?: post.userId).take(2).uppercase(),
                            color         = PulseBlue,
                            fontWeight    = FontWeight.Black,
                            fontSize      = 15.sp
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = post.displayName ?: "User_${post.userId.take(6)}",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 14.sp,
                            color      = TextCharcoal
                        )
                        if (post.isVerified) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector  = Icons.Default.Person, // verified badge placeholder
                                contentDescription = "Verified",
                                tint         = PulseBlue,
                                modifier     = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text     = "@${post.username ?: "user_${post.userId.take(6)}"} · ${formatTime(post.createdAt)}",
                        fontSize = 11.sp,
                        color    = TextMuted
                    )
                }
            }

            var showMenu by remember { mutableStateOf(false) }

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More Options",
                        tint = TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    @Suppress("DEPRECATION")
                    val clipboardManager = LocalClipboardManager.current

                    if (isMyPost) {
                        // Edit Caption
                        DropdownMenuItem(
                            text = { Text("Edit Caption", color = TextCharcoal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Caption",
                                    tint = TextMuted
                                )
                            },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            }
                        )
                        // Copy Post Text
                        DropdownMenuItem(
                            text = { Text("Copy Post Text", color = TextCharcoal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Post Text",
                                    tint = TextMuted
                                )
                            },
                            onClick = {
                                showMenu = false
                                clipboardManager.setText(AnnotatedString(post.content))
                                Toast.makeText(context, "Copied post text to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                        // Pin to Profile
                        val isPinned = post.id == pinnedPostId
                        DropdownMenuItem(
                            text = { Text(if (isPinned) "Unpin from Profile" else "Pin to Profile", color = TextCharcoal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pin to Profile",
                                    tint = if (isPinned) PulseBlue else TextMuted
                                )
                            },
                            onClick = {
                                showMenu = false
                                onPinToggle()
                            }
                        )
                        // Turn off Comments
                        DropdownMenuItem(
                            text = { Text(if (post.commentsDisabled) "Turn on Comments" else "Turn off Comments", color = TextCharcoal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CommentsDisabled,
                                    contentDescription = "Comments toggle",
                                    tint = TextMuted
                                )
                            },
                            onClick = {
                                showMenu = false
                                onCommentsDisabledToggle()
                            }
                        )
                        // Delete Post
                        DropdownMenuItem(
                            text = { Text("Delete Post", color = Color(0xFFEF4444)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFEF4444)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    } else {
                        // Copy Post Text
                        DropdownMenuItem(
                            text = { Text("Copy Post Text", color = TextCharcoal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Post Text",
                                    tint = TextMuted
                                )
                            },
                            onClick = {
                                showMenu = false
                                clipboardManager.setText(AnnotatedString(post.content))
                                Toast.makeText(context, "Copied post text to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                        // Hide Post
                        DropdownMenuItem(
                            text = { Text("Hide Post", color = TextCharcoal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Hide Post",
                                    tint = TextMuted
                                )
                            },
                            onClick = {
                                showMenu = false
                                onHidePost()
                            }
                        )
                        // Unfollow Creator
                        DropdownMenuItem(
                            text = { Text("Unfollow Creator", color = TextCharcoal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PersonRemove,
                                    contentDescription = "Unfollow Creator",
                                    tint = TextMuted
                                )
                            },
                            onClick = {
                                showMenu = false
                                onUnfollowCreator()
                            }
                        )
                        // Mute Creator
                        DropdownMenuItem(
                            text = { Text("Mute Creator", color = TextCharcoal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                                    contentDescription = "Mute Creator",
                                    tint = TextMuted
                                )
                            },
                            onClick = {
                                showMenu = false
                                onMuteCreator()
                            }
                        )
                        // Report Post
                        DropdownMenuItem(
                            text = { Text("Report Post", color = Color(0xFFEF4444)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Report Post",
                                    tint = Color(0xFFEF4444)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onReportPost("Inappropriate content")
                            }
                        )
                    }
                }
            }
        }

        // ── Media area ────────────────────────────────────────────────────
        val mediaUrl = post.imageUrl ?: post.videoUrl
        if (mediaUrl != null) {
            Spacer(Modifier.height(12.dp))
            var isPlaying by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE5E7EB))
                    .let {
                        if (post.videoUrl != null && !isPlaying) {
                            it.clickable { isPlaying = true }
                        } else {
                            it
                        }
                    }
            ) {
                if (post.videoUrl != null && isPlaying) {
                    VideoPlayer(
                        videoUrl = post.videoUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (post.videoUrl != null) {
                    VideoThumbnail(
                        videoUrl = post.videoUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Play overlay for videos
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector  = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint         = Color.White,
                            modifier     = Modifier.size(52.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model             = mediaUrl,
                        contentDescription = "Post media",
                        modifier          = Modifier.fillMaxSize(),
                        contentScale      = ContentScale.Crop
                    )
                }
            }
        }

        // ── Like / Comment / Share / Bookmark row ─────────────────────────
        Spacer(Modifier.height(12.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Like
            val likeIcon = if (post.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder
            val likeColor = if (post.likedByMe) Color(0xFFEF4444) else TextMuted
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onLikeClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = likeIcon,
                    contentDescription = "Like",
                    tint = likeColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = formatCount(post.likeCount),
                    fontSize = 13.sp,
                    color = likeColor,
                    fontWeight = if (post.likedByMe) FontWeight.Bold else FontWeight.Normal
                )
            }
            Spacer(Modifier.width(16.dp))
            
            // Comment
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (post.commentsDisabled) {
                            Toast.makeText(context, "Comments are disabled for this post", Toast.LENGTH_SHORT).show()
                        } else {
                            onCommentClick()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (post.commentsDisabled) Icons.Default.CommentsDisabled else Icons.Default.ModeComment,
                    contentDescription = "Comment",
                    tint = if (post.commentsDisabled) TextMuted.copy(alpha = 0.5f) else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (post.commentsDisabled) "Off" else formatCount(post.commentCount),
                    fontSize = 13.sp,
                    color = if (post.commentsDisabled) TextMuted.copy(alpha = 0.5f) else TextMuted
                )
            }
            Spacer(Modifier.width(16.dp))

            // Share
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, post.content)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share this post via:")
                        context.startActivity(shareIntent)
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // Bookmark
            IconButton(onClick = { isSaved = !isSaved }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector  = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Save",
                    tint         = if (isSaved) PulseBlue else TextMuted,
                    modifier     = Modifier.size(20.dp)
                )
            }
        }

        // ── Caption / content text ────────────────────────────────────────
        if (post.content.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text      = post.content,
                fontSize  = 14.sp,
                color     = TextCharcoal,
                lineHeight = 21.sp,
                maxLines  = 4,
                overflow  = TextOverflow.Ellipsis
            )
        }
    }

    if (showEditDialog) {
        EditCaptionDialog(
            initialContent = post.content,
            onDismiss = { showEditDialog = false },
            onConfirm = { newContent ->
                showEditDialog = false
                onEditCaption(newContent)
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Empty feed hint
// ---------------------------------------------------------------------------
@Composable
private fun EmptyFeedHint() {
    Box(
        modifier           = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment   = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔥", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text("Your feed is warming up…", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextCharcoal)
            Spacer(Modifier.height(4.dp))
            Text("Pull to refresh or be the first to post!", fontSize = 13.sp, color = TextMuted)
        }
    }
}

// ---------------------------------------------------------------------------
// New-post bottom sheet (simple overlay)
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPostBottomSheet(
    content: String,
    onContentChange: (String) -> Unit,
    onPost: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CardBackground)
                .padding(20.dp)
        ) {
            // Handle pill
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD1D5DB))
            )
            Spacer(Modifier.height(16.dp))

            Text("Share something with Pulse", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextCharcoal)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value          = content,
                onValueChange  = onContentChange,
                placeholder    = { Text("What's happening?", color = TextMuted) },
                colors         = OutlinedTextFieldDefaults.colors(
                    focusedTextColor     = TextCharcoal,
                    unfocusedTextColor   = TextCharcoal,
                    focusedBorderColor   = PulseBlue,
                    unfocusedBorderColor = TextMuted.copy(alpha = 0.3f)
                ),
                modifier       = Modifier.fillMaxWidth().height(120.dp)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                Button(
                    onClick = onDismiss,
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E7EB)),
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = TextCharcoal, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick  = onPost,
                    enabled  = content.trim().isNotEmpty(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = PulseBlue,
                        disabledContainerColor = PulseBlue.copy(alpha = 0.4f)
                    ),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("Post", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}.${(n % 1_000_000) / 100_000}M"
    n >= 1_000     -> "${n / 1_000}.${(n % 1_000) / 100}k"
    else           -> n.toString()
}

private fun formatTime(iso: String): String {
    return try {
        val instant = java.time.Instant.parse(iso)
        val now     = java.time.Instant.now()
        val diff    = java.time.Duration.between(instant, now)
        when {
            diff.toMinutes() < 1  -> "just now"
            diff.toHours()   < 1  -> "${diff.toMinutes()}m ago"
            diff.toDays()    < 1  -> "${diff.toHours()}h ago"
            diff.toDays()    < 7  -> "${diff.toDays()}d ago"
            else                   -> "${diff.toDays() / 7}w ago"
        }
    } catch (e: Exception) {
        iso.take(10)
    }
}

// ---------------------------------------------------------------------------
// Video & Stories Helper Components
// ---------------------------------------------------------------------------

@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFirstFrameRendered by remember { mutableStateOf(false) }

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onRenderedFirstFrame() {
                isFirstFrameRendered = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            update = { view ->
                if (view.player != exoPlayer) {
                    view.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isFirstFrameRendered) {
            VideoThumbnail(
                videoUrl = videoUrl,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun VideoThumbnail(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    var thumbnail by remember(videoUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var hasError by remember(videoUrl) { mutableStateOf(false) }

    LaunchedEffect(videoUrl) {
        kotlin.runCatching {
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                val headers = HashMap<String, String>()
                retriever.setDataSource(videoUrl, headers)
                val bmp = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                retriever.release()
                bmp
            }
        }.onSuccess { bmp ->
            thumbnail = bmp
        }.onFailure {
            hasError = true
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bmp = thumbnail
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Video preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = PulseBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun StoryViewerDialog(
    stories: List<StoryUiModel>,
    onClose: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentStory = stories.getOrNull(currentIndex) ?: return
    
    var progress by remember { mutableStateOf(0f) }
    
    val isVideo = currentStory.imageUrl?.endsWith(".mp4", ignoreCase = true) == true || 
                  currentStory.imageUrl?.endsWith(".mov", ignoreCase = true) == true

    val durationMs = 10000L

    LaunchedEffect(currentIndex) {
        progress = 0f
        val startTime = System.currentTimeMillis()
        while (progress < 1f) {
            kotlinx.coroutines.delay(50)
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed.toFloat() / durationMs).coerceAtMost(1f)
        }
        if (currentIndex < stories.lastIndex) {
            currentIndex++
        } else {
            onClose()
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (currentStory.imageUrl != null) {
                if (isVideo) {
                    VideoPlayer(
                        videoUrl = currentStory.imageUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = currentStory.imageUrl,
                        contentDescription = "Story image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            if (!currentStory.content.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Text(
                        text = currentStory.content,
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { index, _ ->
                        val itemProgress = when {
                            index < currentIndex -> 1f
                            index == currentIndex -> progress
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(Color.White.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(itemProgress)
                                    .background(PulseBlue)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val initials = (currentStory.displayName ?: currentStory.username ?: "?").take(2).uppercase()
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentStory.avatarUrl != null) {
                            AsyncImage(
                                model = currentStory.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = currentStory.displayName ?: currentStory.username ?: "User",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentIndex > 0) currentIndex-- else onClose()
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentIndex < stories.lastIndex) currentIndex++ else onClose()
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateStoryDialog(
    onDismiss: () -> Unit,
    onPublish: (content: String?, mediaBytes: ByteArray?, mimeType: String?) -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var mediaPreview by remember { mutableStateOf<String?>(null) }
    var mimeType by remember { mutableStateOf<String?>(null) }
    var isPublishing by remember { mutableStateOf(false) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            selectedUri = uri
            mediaPreview = uri.toString()
            mimeType = context.contentResolver.getType(uri)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmBackground)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Create Story",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PulseBlue
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextCharcoal)
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("What's happening right now?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextCharcoal,
                        unfocusedTextColor = TextCharcoal,
                        focusedBorderColor = PulseBlue,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(Modifier.height(16.dp))

                if (mediaPreview != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        val isVideo = mimeType?.contains("video") == true
                        if (isVideo) {
                            VideoPlayer(videoUrl = mediaPreview!!, modifier = Modifier.fillMaxSize())
                        } else {
                            AsyncImage(
                                model = mediaPreview,
                                contentDescription = "Selected media",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        IconButton(
                            onClick = {
                                selectedUri = null
                                mediaPreview = null
                                mimeType = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { mediaPickerLauncher.launch("*/*") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PulseBlue)
                    ) {
                        Text("Add Photo/Video")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (content.trim().isNotEmpty() || selectedUri != null) {
                                isPublishing = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    val bytes = selectedUri?.let { uri ->
                                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    }
                                    withContext(Dispatchers.Main) {
                                        onPublish(content.trim().ifEmpty { null }, bytes, mimeType)
                                        isPublishing = false
                                        onDismiss()
                                    }
                                }
                            }
                        },
                        enabled = !isPublishing && (content.trim().isNotEmpty() || selectedUri != null),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseBlue)
                    ) {
                        if (isPublishing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Share Story")
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Comments bottom sheet
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsBottomSheet(
    postId: String,
    comments: List<CommentUiModel>,
    onSubmitComment: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Text(
                text = "Comments",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = PulseBlue,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.LightGray.copy(alpha = 0.5f))
            )

            // Comments List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💬", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No comments yet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextCharcoal
                            )
                            Text(
                                "Be the first to share your thoughts!",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(comments) { comment ->
                            CommentItem(comment = comment)
                        }
                    }
                }
            }

            // Input Row at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write a comment...", color = TextMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextCharcoal,
                        unfocusedTextColor = TextCharcoal,
                        focusedBorderColor = PulseBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.7f),
                        focusedContainerColor = WarmBackground.copy(alpha = 0.5f),
                        unfocusedContainerColor = WarmBackground.copy(alpha = 0.5f)
                    )
                )

                Spacer(Modifier.width(12.dp))

                IconButton(
                    onClick = {
                        if (commentText.trim().isNotEmpty()) {
                            onSubmitComment(commentText)
                            commentText = ""
                        }
                    },
                    enabled = commentText.trim().isNotEmpty(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (commentText.trim().isNotEmpty()) PulseBlue else Color.LightGray.copy(alpha = 0.5f))
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

@Composable
private fun CommentItem(comment: CommentUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        val initials = (comment.displayName ?: comment.username ?: "?").take(2).uppercase()
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PulseBlue.copy(alpha = 0.12f))
                .border(1.5.dp, PulseBlue.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (comment.avatarUrl != null) {
                AsyncImage(
                    model = comment.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = initials,
                    color = PulseBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = comment.displayName ?: comment.username ?: "User",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextCharcoal
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "@${comment.username ?: "user"} · ${formatTime(comment.createdAt)}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = comment.content,
                fontSize = 13.sp,
                color = TextCharcoal,
                lineHeight = 18.sp
            )
        }
    }
}
