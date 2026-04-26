package com.bbsrevival.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bbsrevival.data.api.*
import com.bbsrevival.data.models.*
import com.bbsrevival.ui.navigation.Screen
import com.bbsrevival.ui.theme.BbsColors
import com.bbsrevival.ui.theme.BbsTypography
import com.bbsrevival.ui.theme.MonoFontFamily
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay

// ══════════════════════════════════════════════════════════════════════════════
//  SHARED COMPOSABLES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun BbsTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = BbsTypography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = BbsColors.Cyan)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor         = BbsColors.BgSurface,
            titleContentColor      = BbsColors.Cyan,
            actionIconContentColor = BbsColors.Cyan,
        ),
    )
}

@Composable
fun BbsButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        border   = BorderStroke(1.dp, if (enabled) BbsColors.Cyan else BbsColors.Border),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = BbsColors.Cyan),
    ) {
        Text(text, fontFamily = MonoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun BbsTextField(
    value: String, onValueChange: (String) -> Unit,
    label: String, modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, fontFamily = MonoFontFamily, fontSize = 11.sp, letterSpacing = 1.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        maxLines   = maxLines,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone   = { onDone?.invoke() }, onNext = {}),
        textStyle = BbsTypography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = BbsColors.Cyan,
            unfocusedBorderColor = BbsColors.Border,
            focusedLabelColor    = BbsColors.Cyan,
            unfocusedLabelColor  = BbsColors.FgDim,
            cursorColor          = BbsColors.Cyan,
            focusedTextColor     = BbsColors.Fg,
            unfocusedTextColor   = BbsColors.Fg,
            focusedContainerColor   = BbsColors.BgRaised,
            unfocusedContainerColor = BbsColors.BgRaised,
        ),
    )
}

@Composable
fun CenterLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BbsColors.Cyan)
    }
}

@Composable
fun ErrorText(msg: String) {
    if (msg.isNotEmpty()) {
        Text("❌ $msg", color = BbsColors.Red, fontFamily = MonoFontFamily, fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 4.dp))
    }
}

fun roleColor(role: String) = when(role) {
    "sysop"    -> BbsColors.Red
    "cosysop"  -> BbsColors.Magenta
    "validated"-> BbsColors.Cyan
    else       -> BbsColors.FgDim
}

fun roleDisplay(role: String) = when(role) {
    "sysop"    -> "SYSOP"
    "cosysop"  -> "CO-SYSOP"
    "validated"-> "VALIDATED"
    "new"      -> "NEW USER"
    "banned"   -> "BANNED"
    else       -> role.uppercase()
}

fun fmtBytes(b: Long): String = when {
    b < 1024            -> "${b}B"
    b < 1024 * 1024     -> "${"%.1f".format(b / 1024.0)}K"
    else                -> "${"%.1f".format(b / (1024.0 * 1024))}M"
}

// ══════════════════════════════════════════════════════════════════════════════
//  AUTH SCREENS
// ══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: BbsApiClient,
    val tokenStore: TokenStore,
) : ViewModel() {
    var error by mutableStateOf("")
    var loading by mutableStateOf(false)

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            loading = true; error = ""
            try {
                val res = api.login(email, password)
                if (res.ok) onSuccess() else error = res.error ?: "Login failed"
            } catch (e: Exception) { error = e.message ?: "Network error" }
            finally { loading = false }
        }
    }

    fun register(handle: String, email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            loading = true; error = ""
            try {
                val res = api.register(handle, email, password)
                if (res.ok) onSuccess() else error = res.error ?: "Registration failed"
            } catch (e: Exception) { error = e.message ?: "Network error" }
            finally { loading = false }
        }
    }
}

@Composable
fun LoginScreen(navController: NavController, vm: AuthViewModel = hiltViewModel()) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(containerColor = BbsColors.Bg) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // BBS Header
            Text("[BBS]", style = MaterialTheme.typography.displayLarge,
                color = BbsColors.Cyan, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp))
            Text("REVIVAL", style = BbsTypography.labelSmall.copy(letterSpacing = 8.sp), color = BbsColors.FgDim)
            Text("─".repeat(24), color = BbsColors.Border, fontFamily = MonoFontFamily,
                modifier = Modifier.padding(vertical = 16.dp))
            Text("DIAL IN", style = BbsTypography.headlineLarge, modifier = Modifier.padding(bottom = 24.dp))

            BbsTextField(email, { email = it }, "EMAIL", imeAction = ImeAction.Next)
            Spacer(Modifier.height(12.dp))
            BbsTextField(password, { password = it }, "PASSWORD", isPassword = true,
                imeAction = ImeAction.Done,
                onDone = { vm.login(email, password) { navController.navigate(Screen.Boards.route) { popUpTo(0) } } }
            )
            Spacer(Modifier.height(8.dp))
            ErrorText(vm.error)
            Spacer(Modifier.height(16.dp))

            BbsButton("[ LOGIN ]",
                onClick = { vm.login(email, password) { navController.navigate(Screen.Boards.route) { popUpTo(0) } } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !vm.loading,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { navController.navigate(Screen.Register.route) }) {
                Text("New user? Register here", color = BbsColors.FgDim, fontFamily = MonoFontFamily, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun RegisterScreen(navController: NavController, vm: AuthViewModel = hiltViewModel()) {
    var handle   by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        containerColor = BbsColors.Bg,
        topBar = { BbsTopBar("NEW USER", onBack = { navController.popBackStack() }) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("CREATE ACCOUNT", style = BbsTypography.headlineLarge, modifier = Modifier.padding(vertical = 8.dp))
            BbsTextField(handle,   { handle = it },   "HANDLE (your username)")
            BbsTextField(email,    { email = it },    "EMAIL")
            BbsTextField(password, { password = it }, "PASSWORD", isPassword = true,
                imeAction = ImeAction.Done,
                onDone = { vm.register(handle, email, password) { navController.navigate(Screen.Boards.route) { popUpTo(0) } } }
            )
            ErrorText(vm.error)
            BbsButton("[ REGISTER ]",
                onClick = { vm.register(handle, email, password) { navController.navigate(Screen.Boards.route) { popUpTo(0) } } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !vm.loading,
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  BOARDS SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class BoardsViewModel @Inject constructor(private val api: BbsApiClient) : ViewModel() {
    var groups  by mutableStateOf<List<BoardGroup>>(emptyList())
    var loading by mutableStateOf(true)
    var error   by mutableStateOf("")

    init { load() }

    fun load() {
        viewModelScope.launch {
            loading = true; error = ""
            try {
                val res = api.getBoards()
                if (res.ok) groups = res.data ?: emptyList() else error = res.error ?: "Error"
            } catch (e: Exception) { error = e.message ?: "Network error" }
            finally { loading = false }
        }
    }
}

@Composable
fun BoardsScreen(navController: NavController, vm: BoardsViewModel = hiltViewModel()) {
    Scaffold(
        containerColor = BbsColors.Bg,
        topBar = {
            BbsTopBar("MESSAGE BOARDS", actions = {
                IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                    Icon(Icons.Default.Search, "Search", tint = BbsColors.Cyan)
                }
                IconButton(onClick = { navController.navigate(Screen.Messages.route) }) {
                    Icon(Icons.Default.Mail, "Messages", tint = BbsColors.Cyan)
                }
                IconButton(onClick = { navController.navigate(Screen.Gallery.route) }) {
                    Icon(Icons.Default.Image, "Gallery", tint = BbsColors.Cyan)
                }
            })
        }
    ) { pad ->
        if (vm.loading) { CenterLoader(); return@Scaffold }
        if (vm.error.isNotEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(vm.error, color = BbsColors.Red, fontFamily = MonoFontFamily)
                    BbsButton("[ RETRY ]", vm::load)
                }
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            vm.groups.forEach { group ->
                item {
                    Text(
                        group.name.uppercase(),
                        style = BbsTypography.labelSmall.copy(letterSpacing = 3.sp),
                        color = BbsColors.Cyan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BbsColors.BgRaised)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    HorizontalDivider(color = BbsColors.BorderDim)
                }
                items(group.boards) { board ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Screen.ThreadList.go(board.id, board.name)) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(board.name, style = BbsTypography.bodyLarge, color = BbsColors.FgBright)
                            if (board.description.isNotEmpty())
                                Text(board.description, style = BbsTypography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${board.threadCount}", style = BbsTypography.titleMedium, color = BbsColors.Cyan)
                            Text("threads", style = BbsTypography.labelSmall)
                        }
                    }
                    HorizontalDivider(color = BbsColors.BorderDim)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  THREAD LIST SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class ThreadListViewModel @Inject constructor(private val api: BbsApiClient) : ViewModel() {
    var threads  by mutableStateOf<List<Thread>>(emptyList())
    var total    by mutableStateOf(0)
    var loading  by mutableStateOf(true)
    var page     by mutableStateOf(1)
    var error    by mutableStateOf("")
    var boardId  = ""

    fun load(bid: String = boardId, pg: Int = page) {
        boardId = bid
        viewModelScope.launch {
            loading = true
            try {
                val res = api.getThreads(bid, pg)
                if (res.ok) { threads = res.data?.threads ?: emptyList(); total = res.data?.total ?: 0; page = pg }
                else error = res.error ?: "Error"
            } catch (e: Exception) { error = e.message ?: "Network error" }
            finally { loading = false }
        }
    }
}

@Composable
fun ThreadListScreen(boardId: String, boardName: String, navController: NavController, vm: ThreadListViewModel = hiltViewModel()) {
    var showNewThread by remember { mutableStateOf(false) }
    var newTitle      by remember { mutableStateOf("") }
    var newBody       by remember { mutableStateOf("") }
    var posting       by remember { mutableStateOf(false) }
    val api           = hiltViewModel<BoardsViewModel>().let { hiltViewModel<ThreadListViewModel>() }
    val apiClient     : BbsApiClient = hiltViewModel<AuthViewModel>().let {
        (androidx.hilt.navigation.compose.hiltViewModel<ThreadListViewModel>() as ThreadListViewModel).let {
            it.load(boardId)
            hiltViewModel<ThreadListViewModel>()
        }
        androidx.hilt.navigation.compose.hiltViewModel<AuthViewModel>()
    }.let { hiltViewModel<ThreadListViewModel>() }.let {
        // just trigger load
        LaunchedEffect(boardId) { vm.load(boardId) }
        hiltViewModel<AuthViewModel>()
    }

    // simpler: just call load on init via LaunchedEffect
    LaunchedEffect(boardId) { vm.load(boardId) }

    val scope       = rememberCoroutineScope()
    val apiC: BbsApiClient = hiltViewModel<AuthViewModel>().run { hiltViewModel() }

    Scaffold(
        containerColor = BbsColors.Bg,
        topBar = {
            BbsTopBar(boardName, onBack = { navController.popBackStack() }, actions = {
                IconButton(onClick = { showNewThread = !showNewThread }) {
                    Icon(if (showNewThread) Icons.Default.Close else Icons.Default.Add, null, tint = BbsColors.Cyan)
                }
            })
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (showNewThread) {
                NewThreadForm(
                    onPost = { title, body ->
                        scope.launch {
                            posting = true
                            try {
                                val res = apiC.createThread(boardId, title, body)
                                if (res.ok) {
                                    showNewThread = false; newTitle = ""; newBody = ""
                                    res.data?.threadId?.let { navController.navigate(Screen.Thread.go(it)) }
                                    vm.load(boardId)
                                }
                            } finally { posting = false }
                        }
                    },
                    posting = posting,
                )
            }
            if (vm.loading) { CenterLoader(); return@Scaffold }

            val limit = 25
            val totalPages = (vm.total + limit - 1) / limit

            LazyColumn(Modifier.weight(1f)) {
                items(vm.threads) { thread ->
                    ThreadRow(thread) { navController.navigate(Screen.Thread.go(thread.id)) }
                    HorizontalDivider(color = BbsColors.BorderDim)
                }
            }
            if (totalPages > 1) {
                PaginationRow(vm.page, totalPages,
                    onPrev = { vm.load(boardId, vm.page - 1) },
                    onNext = { vm.load(boardId, vm.page + 1) })
            }
        }
    }
}

@Composable
fun ThreadRow(thread: Thread, onClick: () -> Unit) {
    val borderColor = if (thread.pinned) BbsColors.Yellow else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (thread.pinned) BbsColors.BgOverlay else Color.Transparent)
            .border(BorderStroke(if (thread.pinned) 2.dp else 0.dp, borderColor), MaterialTheme.shapes.small)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (thread.pinned)  Text("📌", fontSize = 11.sp)
                if (thread.locked)  Text("🔒", fontSize = 11.sp)
                if (thread.hasUnread) Box(Modifier.size(6.dp).background(BbsColors.Cyan, shape = MaterialTheme.shapes.small))
                Text(thread.title,
                    style = BbsTypography.bodyLarge.copy(
                        color = if (thread.hasUnread) BbsColors.FgBright else BbsColors.Fg,
                        fontWeight = if (thread.hasUnread) FontWeight.Bold else FontWeight.Normal,
                    ),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${thread.authorHandle} · ${thread.lastPostAt.take(10)}",
                style = BbsTypography.bodySmall,
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
            Text("${thread.replyCount}", color = BbsColors.Cyan, fontFamily = MonoFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("replies", style = BbsTypography.labelSmall)
        }
    }
}

@Composable
fun NewThreadForm(onPost: (String, String) -> Unit, posting: Boolean) {
    var title by remember { mutableStateOf("") }
    var body  by remember { mutableStateOf("") }
    Column(
        Modifier.background(BbsColors.BgSurface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BbsTextField(title, { title = it }, "SUBJECT", imeAction = ImeAction.Next)
        BbsTextField(body,  { body  = it }, "MESSAGE", singleLine = false, maxLines = 6,
            imeAction = ImeAction.Default)
        BbsButton("[ POST THREAD ]", onClick = { onPost(title, body) },
            modifier = Modifier.align(Alignment.End), enabled = !posting && title.isNotBlank() && body.isNotBlank())
    }
    HorizontalDivider(color = BbsColors.Border)
}

// ══════════════════════════════════════════════════════════════════════════════
//  THREAD SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val api: BbsApiClient,
    val tokenStore: TokenStore,
) : ViewModel() {
    var thread   by mutableStateOf<ThreadDetail?>(null)
    var posts    by mutableStateOf<List<Post>>(emptyList())
    var total    by mutableStateOf(0)
    var page     by mutableStateOf(1)
    var loading  by mutableStateOf(true)
    var error    by mutableStateOf("")
    var threadId = ""

    fun load(tid: String = threadId, pg: Int = page) {
        threadId = tid
        viewModelScope.launch {
            loading = true
            try {
                if (thread == null) {
                    val tr = api.getThread(tid)
                    if (tr.ok) thread = tr.data
                }
                val pr = api.getPosts(tid, pg)
                if (pr.ok) { posts = pr.data?.posts ?: emptyList(); total = pr.data?.total ?: 0; page = pg }
                else error = pr.error ?: "Error"
            } catch (e: Exception) { error = e.message ?: "Network error" }
            finally { loading = false }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try { api.deletePost(postId); load() } catch (_: Exception) {}
        }
    }

    fun reply(body: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                api.createPost(threadId, body)
                val newLastPage = (total + 1 + 24) / 25
                load(pg = newLastPage)
                onDone()
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun ThreadScreen(threadId: String, navController: NavController, vm: ThreadViewModel = hiltViewModel()) {
    LaunchedEffect(threadId) { vm.load(threadId) }

    var replyText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    LaunchedEffect(vm.posts.size) {
        if (vm.posts.isNotEmpty()) listState.animateScrollToItem(vm.posts.size - 1)
    }

    Scaffold(
        containerColor = BbsColors.Bg,
        topBar = { BbsTopBar(vm.thread?.title ?: "Thread", onBack = { navController.popBackStack() }) },
    ) { pad ->
        if (vm.loading && vm.posts.isEmpty()) { CenterLoader(); return@Scaffold }
        val locked = vm.thread?.locked == true
        val limit  = 25
        val totalPages = (vm.total + limit - 1) / limit

        Column(Modifier.fillMaxSize().padding(pad)) {
            if (locked) {
                Box(Modifier.fillMaxWidth().background(Color(0x22F0C040)).padding(8.dp)) {
                    Text("🔒 Thread locked", color = BbsColors.Yellow, fontFamily = MonoFontFamily, fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Center))
                }
            }

            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                if (totalPages > 1) {
                    item {
                        PaginationRow(vm.page, totalPages,
                            onPrev = { vm.load(pg = vm.page - 1) },
                            onNext = { vm.load(pg = vm.page + 1) })
                    }
                }
                itemsIndexed(vm.posts) { idx, post ->
                    PostCard(
                        post = post,
                        postNum = (vm.page - 1) * limit + idx + 1,
                        currentUserId = vm.tokenStore.accessToken?.let { "" } ?: "",
                        onDelete = { vm.deletePost(post.id) },
                    )
                    HorizontalDivider(color = BbsColors.BorderDim)
                }
            }

            if (!locked) {
                ReplyBox(
                    text = replyText,
                    onTextChange = { replyText = it },
                    onSend = {
                        vm.reply(replyText) { replyText = "" }
                    }
                )
            }
        }
    }
}

@Composable
fun PostCard(post: Post, postNum: Int, currentUserId: String, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(post.authorHandle, color = roleColor(post.authorRole), fontFamily = MonoFontFamily,
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(roleDisplay(post.authorRole), style = BbsTypography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text("#$postNum", style = BbsTypography.labelSmall)
            Text(post.createdAt.take(10), style = BbsTypography.labelSmall)
            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = BbsColors.Red, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(post.body, style = BbsTypography.bodyMedium)
        if (post.editedAt != null) {
            Text("(edited)", style = BbsTypography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun ReplyBox(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(BbsColors.BgSurface)
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text, onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Write a reply...", color = BbsColors.FgDim, fontFamily = MonoFontFamily, fontSize = 13.sp) },
            textStyle  = BbsTypography.bodyMedium,
            maxLines   = 5,
            colors     = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = BbsColors.Cyan, unfocusedBorderColor = BbsColors.Border,
                focusedContainerColor= BbsColors.BgRaised, unfocusedContainerColor= BbsColors.BgRaised,
                cursorColor = BbsColors.Cyan, focusedTextColor = BbsColors.Fg, unfocusedTextColor = BbsColors.Fg,
            ),
        )
        IconButton(
            onClick = { keyboard?.hide(); onSend() },
            enabled = text.isNotBlank(),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Default.Send, "Send", tint = if (text.isNotBlank()) BbsColors.Cyan else BbsColors.FgDim)
        }
    }
}

@Composable
fun PaginationRow(page: Int, totalPages: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrev, enabled = page > 1) { Text("◀ PREV", fontFamily = MonoFontFamily, fontSize = 12.sp, color = if (page > 1) BbsColors.Cyan else BbsColors.FgDim) }
        Text("Page $page of $totalPages", style = BbsTypography.labelMedium)
        TextButton(onClick = onNext, enabled = page < totalPages) { Text("NEXT ▶", fontFamily = MonoFontFamily, fontSize = 12.sp, color = if (page < totalPages) BbsColors.Cyan else BbsColors.FgDim) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  CHAT SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val api: BbsApiClient,
    val socketManager: SocketManager,
) : ViewModel() {
    var rooms       by mutableStateOf<List<ChatRoom>>(emptyList())
    var activeRoom  by mutableStateOf<ChatRoom?>(null)
    var messages    by mutableStateOf<List<ChatMessage>>(emptyList())
    var connected   by mutableStateOf(false)
    var typingHandles by mutableStateOf<Set<String>>(emptySet())

    init {
        socketManager.connect()
        viewModelScope.launch {
            socketManager.connected.collect { connected = it }
        }
        viewModelScope.launch {
            socketManager.messages.collect { msg ->
                if (msg.roomId == activeRoom?.id) {
                    messages = (messages + msg).takeLast(200)
                }
            }
        }
        viewModelScope.launch {
            socketManager.userJoined.collect { (roomId, handle) ->
                if (roomId == activeRoom?.id) {
                    messages = messages + ChatMessage("sys-${System.currentTimeMillis()}", roomId, "system", "SYSTEM", "system", "*** $handle joined ***", "", true)
                }
            }
        }
        viewModelScope.launch {
            socketManager.userLeft.collect { (roomId, handle) ->
                if (roomId == activeRoom?.id) {
                    messages = messages + ChatMessage("sys-${System.currentTimeMillis()}", roomId, "system", "SYSTEM", "system", "*** $handle left ***", "", true)
                }
            }
        }
        viewModelScope.launch {
            socketManager.typingUsers.collect { (roomId, handle) ->
                if (roomId == activeRoom?.id) {
                    typingHandles = typingHandles + handle
                    delay(3000)
                    typingHandles = typingHandles - handle
                }
            }
        }
        loadRooms()
    }

    fun loadRooms() {
        viewModelScope.launch {
            try {
                val res = api.getChatRooms()
                if (res.ok) rooms = res.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun joinRoom(room: ChatRoom) {
        activeRoom?.let { socketManager.leaveRoom(it.id) }
        activeRoom = room
        messages   = emptyList()
        socketManager.joinRoom(room.id)
        viewModelScope.launch {
            try {
                val res = api.getChatHistory(room.id)
                if (res.ok) messages = res.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun sendMessage(body: String) {
        val room = activeRoom ?: return
        socketManager.sendChatMessage(room.id, body)
    }

    fun sendTyping() { activeRoom?.let { socketManager.sendTyping(it.id) } }

    override fun onCleared() {
        activeRoom?.let { socketManager.leaveRoom(it.id) }
        super.onCleared()
    }
}

@Composable
fun ChatScreen(vm: ChatViewModel = hiltViewModel()) {
    val listState = rememberLazyListState()
    var input     by remember { mutableStateOf("") }
    val scope     = rememberCoroutineScope()

    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.size - 1)
    }

    if (vm.activeRoom == null) {
        Scaffold(
            containerColor = BbsColors.Bg,
            topBar = { BbsTopBar("CHAT ROOMS") }
        ) { pad ->
            LazyColumn(Modifier.fillMaxSize().padding(pad)) {
                items(vm.rooms) { room ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.joinRoom(room) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(room.name, style = BbsTypography.bodyLarge, color = BbsColors.FgBright)
                            if (room.description.isNotEmpty()) Text(room.description, style = BbsTypography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(6.dp).background(BbsColors.Green, MaterialTheme.shapes.small))
                            Text("${room.onlineCount}", color = BbsColors.Green, fontFamily = MonoFontFamily, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = BbsColors.BorderDim)
                }
            }
        }
        return
    }

    Scaffold(
        containerColor = BbsColors.Bg,
        topBar = {
            BbsTopBar(vm.activeRoom!!.name, onBack = { vm.activeRoom = null }, actions = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                    Box(Modifier.size(7.dp).background(if (vm.connected) BbsColors.Green else BbsColors.Yellow, MaterialTheme.shapes.small))
                }
            })
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                items(vm.messages, key = { it.id }) { msg ->
                    if (msg.isSystem) {
                        Text("${msg.body}", color = BbsColors.FgDim, fontFamily = MonoFontFamily,
                            fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    } else {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(msg.userHandle, color = roleColor(msg.userRole), fontFamily = MonoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(msg.createdAt.takeLast(8).take(5), style = BbsTypography.labelSmall)
                            }
                            Text(msg.body, style = BbsTypography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
            if (vm.typingHandles.isNotEmpty()) {
                Text("${vm.typingHandles.joinToString(", ")} typing...",
                    color = BbsColors.FgDim, fontFamily = MonoFontFamily, fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
            }
            Row(
                Modifier.fillMaxWidth().background(BbsColors.BgSurface).padding(8.dp),
                verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it; vm.sendTyping() },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message...", color = BbsColors.FgDim, fontFamily = MonoFontFamily, fontSize = 13.sp) },
                    textStyle = BbsTypography.bodyMedium, maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BbsColors.Cyan, unfocusedBorderColor = BbsColors.Border,
                        focusedContainerColor = BbsColors.BgRaised, unfocusedContainerColor = BbsColors.BgRaised,
                        cursorColor = BbsColors.Cyan, focusedTextColor = BbsColors.Fg, unfocusedTextColor = BbsColors.Fg,
                    ),
                )
                IconButton(onClick = { if (input.isNotBlank()) { vm.sendMessage(input); input = "" } }, enabled = input.isNotBlank()) {
                    Icon(Icons.Default.Send, "Send", tint = if (input.isNotBlank()) BbsColors.Cyan else BbsColors.FgDim)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  FILES SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class FilesViewModel @Inject constructor(private val api: BbsApiClient) : ViewModel() {
    var areas      by mutableStateOf<List<FileArea>>(emptyList())
    var files      by mutableStateOf<List<FileListing>>(emptyList())
    var activeArea by mutableStateOf<FileArea?>(null)
    var loading    by mutableStateOf(true)
    var total      by mutableStateOf(0)
    var page       by mutableStateOf(1)

    init { loadAreas() }

    fun loadAreas() {
        viewModelScope.launch {
            loading = true
            try { val r = api.getFileAreas(); if (r.ok) areas = r.data ?: emptyList() }
            catch (_: Exception) {}
            finally { loading = false }
        }
    }

    fun loadFiles(area: FileArea, pg: Int = 1) {
        activeArea = area
        viewModelScope.launch {
            loading = true
            try {
                val r = api.getAreaFiles(area.id, pg)
                if (r.ok) { files = r.data?.files ?: emptyList(); total = r.data?.total ?: 0; page = pg }
            } catch (_: Exception) {}
            finally { loading = false }
        }
    }
}

@Composable
fun FilesScreen(navController: NavController, vm: FilesViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val api: BbsApiClient = hiltViewModel<AuthViewModel>().let { hiltViewModel() }

    if (vm.activeArea == null) {
        Scaffold(containerColor = BbsColors.Bg, topBar = { BbsTopBar("FILE AREAS") }) { pad ->
            if (vm.loading) { CenterLoader(); return@Scaffold }
            LazyColumn(Modifier.fillMaxSize().padding(pad)) {
                items(vm.areas) { area ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.loadFiles(area) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(area.name, style = BbsTypography.bodyLarge, color = BbsColors.FgBright)
                            if (area.description.isNotEmpty()) Text(area.description, style = BbsTypography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${area.fileCount}", color = BbsColors.Cyan, fontFamily = MonoFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("files", style = BbsTypography.labelSmall)
                        }
                    }
                    HorizontalDivider(color = BbsColors.BorderDim)
                }
            }
        }
        return
    }

    val limit = 25
    val totalPages = (vm.total + limit - 1) / limit

    Scaffold(
        containerColor = BbsColors.Bg,
        topBar = { BbsTopBar(vm.activeArea!!.name, onBack = { vm.activeArea = null; vm.files = emptyList() }) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (vm.loading) { CenterLoader(); return@Scaffold }
            LazyColumn(Modifier.weight(1f)) {
                items(vm.files) { file ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!file.approved) {
                                    Surface(color = BbsColors.Yellow.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                                        Text("PENDING", color = BbsColors.Yellow, fontFamily = MonoFontFamily, fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                                Text(file.originalName, style = BbsTypography.bodyMedium, color = BbsColors.FgBright,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (file.description.isNotEmpty())
                                Text(file.description, style = BbsTypography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${fmtBytes(file.sizeBytes)} · ${file.uploaderHandle} · ${file.dlCount} DL · ▲${file.thumbsUp} ▼${file.thumbsDown}",
                                style = BbsTypography.labelSmall)
                        }
                        if (file.approved) {
                            IconButton(onClick = {
                                val url = api.downloadUrl(file.id)
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }) {
                                Icon(Icons.Default.Download, "Download", tint = BbsColors.Cyan)
                            }
                        }
                    }
                    HorizontalDivider(color = BbsColors.BorderDim)
                }
            }
            if (totalPages > 1) {
                PaginationRow(vm.page, totalPages,
                    onPrev = { vm.loadFiles(vm.activeArea!!, vm.page - 1) },
                    onNext = { vm.loadFiles(vm.activeArea!!, vm.page + 1) })
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  DOOR GAMES SCREEN + TERMINAL
// ══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class DoorsViewModel @Inject constructor(private val api: BbsApiClient) : ViewModel() {
    var games       by mutableStateOf<List<DoorGame>>(emptyList())
    var leaderboard by mutableStateOf<List<LeaderboardEntry>>(emptyList())
    var loading     by mutableStateOf(true)

    init { loadGames() }

    fun loadGames() {
        viewModelScope.launch {
            loading = true
            try { val r = api.getDoorGames(); if (r.ok) games = r.data ?: emptyList() }
            catch (_: Exception) {}
            finally { loading = false }
        }
    }

    fun loadLeaderboard(gameId: String) {
        viewModelScope.launch {
            try { val r = api.getLeaderboard(gameId); if (r.ok) leaderboard = r.data ?: emptyList() }
            catch (_: Exception) {}
        }
    }
}

@Composable
fun DoorsScreen(navController: NavController, vm: DoorsViewModel = hiltViewModel()) {
    Scaffold(containerColor = BbsColors.Bg, topBar = { BbsTopBar("DOOR GAMES") }) { pad ->
        if (vm.loading) { CenterLoader(); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            items(vm.games) { game ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        vm.loadLeaderboard(game.id)
                        navController.navigate(Screen.DoorGame.go(game.id, game.slug, game.name))
                    }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(game.name, style = BbsTypography.bodyLarge, color = BbsColors.FgBright, fontWeight = FontWeight.Bold)
                        Text(game.description, style = BbsTypography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("by ${game.author} · ${game.playCount} plays${game.topScore?.let { " · Top: $it" } ?: ""}",
                            style = BbsTypography.labelSmall)
                    }
                    Icon(Icons.Default.PlayArrow, "Play", tint = BbsColors.Cyan, modifier = Modifier.size(28.dp))
                }
                HorizontalDivider(color = BbsColors.BorderDim)
            }
        }
    }
}

// ANSI strip — removes escape codes for plain terminal rendering on Android
fun stripAnsi(s: String) = s.replace(Regex("\u001B\\[[0-9;]*m"), "")

@HiltViewModel
class DoorGameViewModel @Inject constructor(val socketManager: SocketManager) : ViewModel() {
    var termLines   by mutableStateOf<List<String>>(emptyList())
    var gameEnded   by mutableStateOf(false)
    var finalScore  by mutableStateOf<Int?>(null)
    var activeSlug  = ""

    fun start(gameSlug: String) {
        activeSlug = gameSlug
        termLines  = emptyList()
        gameEnded  = false
        finalScore = null
        socketManager.startDoorGame(gameSlug)
        viewModelScope.launch {
            socketManager.doorOutput.collect { (slug, output) ->
                if (slug != activeSlug) return@collect
                val lines = stripAnsi(output).split("\n")
                termLines = (termLines + lines).takeLast(300)
            }
        }
        viewModelScope.launch {
            socketManager.doorEnd.collect { (slug, score) ->
                if (slug != activeSlug) return@collect
                gameEnded  = true
                finalScore = score
                termLines  = termLines + listOf("", "─".repeat(40),
                    if (score != null) "Game over. Score: $score" else "Session ended.", "")
            }
        }
    }

    fun sendInput(input: String) {
        socketManager.sendDoorInput(activeSlug, input)
        termLines = termLines + listOf("> $input")
    }
}

@Composable
fun DoorGameScreen(gameId: String, gameSlug: String, gameName: String, navController: NavController,
                   vm: DoorGameViewModel = hiltViewModel(), doorsVm: DoorsViewModel = hiltViewModel()) {
    LaunchedEffect(gameSlug) { vm.start(gameSlug) }

    val scrollState = rememberScrollState()
    var input       by remember { mutableStateOf("") }
    val scope       = rememberCoroutineScope()
    val keyboard    = LocalSoftwareKeyboardController.current
    val focus       = remember { FocusRequester() }

    LaunchedEffect(vm.termLines.size) {
        scope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
    }

    Scaffold(
        containerColor = BbsColors.Bg,
        topBar = {
            BbsTopBar(gameName, onBack = { navController.popBackStack() }, actions = {
                if (vm.gameEnded) {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("EXIT", color = BbsColors.Cyan, fontFamily = MonoFontFamily, fontSize = 12.sp)
                    }
                }
            })
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // Leaderboard strip (top 3)
            val board = doorsVm.leaderboard.take(3)
            if (board.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().background(BbsColors.BgRaised).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    board.forEach { entry ->
                        val medal = when(entry.rank) { 1 -> "🥇"; 2 -> "🥈"; else -> "🥉" }
                        Text("$medal ${entry.userHandle}: ${entry.score}", style = BbsTypography.labelSmall, color = BbsColors.Yellow)
                    }
                }
                HorizontalDivider(color = BbsColors.BorderDim)
            }

            // Terminal output
            Column(
                Modifier.weight(1f).background(Color(0xFF000000))
                    .verticalScroll(scrollState).padding(10.dp)
            ) {
                vm.termLines.forEach { line ->
                    Text(
                        line,
                        fontFamily = MonoFontFamily,
                        fontSize   = 13.sp,
                        color      = Color(0xFFCCCCCC),
                        lineHeight = 18.sp,
                    )
                }
                if (vm.gameEnded && vm.finalScore != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("FINAL SCORE: ${vm.finalScore}", color = BbsColors.Yellow,
                        fontFamily = MonoFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Input row
            if (!vm.gameEnded) {
                Row(
                    Modifier.fillMaxWidth().background(BbsColors.BgSurface).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        modifier = Modifier.weight(1f).focusRequester(focus),
                        placeholder = { Text("Enter command...", color = BbsColors.FgDim, fontFamily = MonoFontFamily, fontSize = 13.sp) },
                        textStyle  = BbsTypography.bodyMedium.copy(fontFamily = MonoFontFamily),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            vm.sendInput(input); input = ""
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BbsColors.Cyan, unfocusedBorderColor = BbsColors.Border,
                            focusedContainerColor = BbsColors.BgRaised, unfocusedContainerColor = BbsColors.BgRaised,
                            cursorColor = BbsColors.Cyan, focusedTextColor = BbsColors.Fg, unfocusedTextColor = BbsColors.Fg,
                        ),
                    )
                    IconButton(onClick = { vm.sendInput(input); input = ""; keyboard?.hide() }, enabled = input.isNotBlank()) {
                        Icon(Icons.Default.Send, "Send", tint = if (input.isNotBlank()) BbsColors.Cyan else BbsColors.FgDim)
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().background(BbsColors.BgSurface).padding(12.dp), contentAlignment = Alignment.Center) {
                    BbsButton("[ BACK TO GAMES ]", onClick = { navController.popBackStack() })
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  GALLERY SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class GalleryViewModel @Inject constructor(private val api: BbsApiClient) : ViewModel() {
    var items   by mutableStateOf<List<GalleryItem>>(emptyList())
    var loading by mutableStateOf(true)
    var page    by mutableStateOf(1)
    var total   by mutableStateOf(0)
    var sort    by mutableStateOf("newest")
    var selected by mutableStateOf<GalleryItem?>(null)

    init { load() }

    fun load(pg: Int = page, s: String = sort) {
        sort = s
        viewModelScope.launch {
            loading = true
            try {
                val r = api.getGallery(pg, s)
                if (r.ok) { items = r.data?.items ?: emptyList(); total = r.data?.total ?: 0; page = pg }
            } catch (_: Exception) {}
            finally { loading = false }
        }
    }

    fun openItem(item: GalleryItem) {
        viewModelScope.launch {
            try {
                val r = api.getGalleryItem(item.id)
                if (r.ok) selected = r.data
            } catch (_: Exception) { selected = item }
        }
    }

    fun like(item: GalleryItem) {
        viewModelScope.launch {
            try {
                val r = api.likeGalleryItem(item.id)
                if (r.ok) {
                    val likes = r.data?.likes ?: return@launch
                    items = items.map { if (it.id == item.id) it.copy(likes = likes, liked = !it.liked) else it }
                    if (selected?.id == item.id) selected = selected?.copy(likes = likes, liked = !(selected?.liked ?: false))
                }
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun GalleryScreen(vm: GalleryViewModel = hiltViewModel()) {
    val limit = 20
    val totalPages = (vm.total + limit - 1) / limit

    if (vm.selected != null) {
        GalleryItemDetail(item = vm.selected!!, onBack = { vm.selected = null }, onLike = { vm.like(vm.selected!!) })
        return
    }

    Scaffold(containerColor = BbsColors.Bg, topBar = {
        BbsTopBar("ANSI GALLERY", actions = {
            TextButton(onClick = { vm.load(1, if (vm.sort == "newest") "top" else "newest") }) {
                Text(if (vm.sort == "newest") "TOP" else "NEWEST", color = BbsColors.Cyan, fontFamily = MonoFontFamily, fontSize = 12.sp)
            }
        })
    }) { pad ->
        if (vm.loading && vm.items.isEmpty()) { CenterLoader(); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            items(vm.items) { item ->
                Column(
                    Modifier.fillMaxWidth().clickable { vm.openItem(item) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = BbsTypography.bodyLarge, color = BbsColors.FgBright, fontWeight = FontWeight.Bold)
                            Text("by ${item.authorHandle} · ${item.createdAt.take(10)}", style = BbsTypography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { vm.like(item) }, modifier = Modifier.size(32.dp)) {
                                Icon(if (item.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Like",
                                    tint = if (item.liked) Color(0xFFFF6B8A) else BbsColors.FgDim, modifier = Modifier.size(18.dp))
                            }
                            Text("${item.likes}", style = BbsTypography.bodySmall, color = if (item.liked) Color(0xFFFF6B8A) else BbsColors.FgDim)
                        }
                    }
                    // ASCII preview (plain text, first 8 lines)
                    Text(
                        stripAnsi(item.preview).lines().take(8).joinToString("\n"),
                        fontFamily = MonoFontFamily, fontSize = 11.sp, color = Color(0xFFAAAAAA),
                        modifier = Modifier.background(Color(0xFF000000)).padding(6.dp).fillMaxWidth(),
                        lineHeight = 14.sp,
                    )
                    if (item.tags.isNotEmpty()) {
                        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            item.tags.take(4).forEach { tag ->
                                Surface(color = BbsColors.BgOverlay, shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, BbsColors.BorderDim)) {
                                    Text(tag, color = BbsColors.FgDim, fontFamily = MonoFontFamily, fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = BbsColors.BorderDim)
            }
            if (totalPages > 1) {
                item {
                    PaginationRow(vm.page, totalPages,
                        onPrev = { vm.load(vm.page - 1) },
                        onNext = { vm.load(vm.page + 1) })
                }
            }
        }
    }
}

@Composable
fun GalleryItemDetail(item: GalleryItem, onBack: () -> Unit, onLike: () -> Unit) {
    Scaffold(containerColor = BbsColors.Bg, topBar = {
        BbsTopBar(item.title, onBack = onBack, actions = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                IconButton(onClick = onLike) {
                    Icon(if (item.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Like",
                        tint = if (item.liked) Color(0xFFFF6B8A) else BbsColors.FgDim)
                }
                Text("${item.likes}", color = if (item.liked) Color(0xFFFF6B8A) else BbsColors.FgDim, fontFamily = MonoFontFamily, fontSize = 13.sp)
            }
        })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
            Text("by ${item.authorHandle} · ${item.createdAt.take(10)}", style = BbsTypography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Box(Modifier.fillMaxWidth().background(Color(0xFF000000)).horizontalScroll(rememberScrollState()).padding(10.dp)) {
                Text(
                    stripAnsi(item.content ?: item.preview),
                    fontFamily = MonoFontFamily, fontSize = 13.sp, color = Color(0xFFCCCCCC), lineHeight = 16.sp,
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  MESSAGES SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class MessagesViewModel @Inject constructor(private val api: BbsApiClient) : ViewModel() {
    var messages by mutableStateOf<List<PrivateMessage>>(emptyList())
    var unread   by mutableStateOf(0)
    var loading  by mutableStateOf(true)
    var box      by mutableStateOf("inbox")
    var selected by mutableStateOf<PrivateMessage?>(null)
    var showCompose by mutableStateOf(false)

    init { load() }

    fun load() {
        viewModelScope.launch {
            loading = true
            try {
                val r = if (box == "inbox") api.getInbox() else api.getSent()
                if (r.ok) { messages = r.data?.messages ?: emptyList(); unread = r.data?.unread ?: 0 }
            } catch (_: Exception) {}
            finally { loading = false }
        }
    }

    fun open(msg: PrivateMessage) {
        viewModelScope.launch {
            try { val r = api.getMessage(msg.id); if (r.ok) selected = r.data } catch (_: Exception) { selected = msg }
        }
    }

    fun send(toHandle: String, subject: String, body: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val r = api.sendMessage(toHandle, subject, body)
                if (r.ok) { showCompose = false; onDone() }
            } catch (_: Exception) {}
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try { api.deleteMessage(id); selected = null; load() } catch (_: Exception) {}
        }
    }
}

@Composable
fun MessagesScreen(vm: MessagesViewModel = hiltViewModel()) {
    if (vm.selected != null) {
        MessageDetail(msg = vm.selected!!, onBack = { vm.selected = null }, onDelete = { vm.delete(vm.selected!!.id) }, onReply = { vm.selected = null; vm.showCompose = true })
        return
    }
    if (vm.showCompose) {
        ComposeMessage(onSend = { to, subj, body -> vm.send(to, subj, body) { vm.load() } }, onCancel = { vm.showCompose = false })
        return
    }

    Scaffold(containerColor = BbsColors.Bg, topBar = {
        BbsTopBar("PRIVATE MAIL${if (vm.unread > 0) " (${vm.unread})" else ""}", actions = {
            IconButton(onClick = { vm.showCompose = true }) { Icon(Icons.Default.Edit, "Compose", tint = BbsColors.Cyan) }
        })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.fillMaxWidth().background(BbsColors.BgSurface)) {
                listOf("inbox", "sent").forEach { b ->
                    TextButton(onClick = { vm.box = b; vm.load() }, modifier = Modifier.weight(1f)) {
                        Text(b.uppercase(), color = if (vm.box == b) BbsColors.Cyan else BbsColors.FgDim,
                            fontFamily = MonoFontFamily, fontSize = 12.sp, fontWeight = if (vm.box == b) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            HorizontalDivider(color = BbsColors.Border)
            if (vm.loading) { CenterLoader(); return@Scaffold }
            if (vm.messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Empty.", color = BbsColors.FgDim, fontFamily = MonoFontFamily)
                }
                return@Scaffold
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(vm.messages) { msg ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.open(msg) }
                            .background(if (msg.readAt == null && vm.box == "inbox") BbsColors.BgRaised else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (msg.readAt == null && vm.box == "inbox") {
                            Box(Modifier.size(6.dp).background(BbsColors.Cyan, MaterialTheme.shapes.small).padding(end = 8.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(if (vm.box == "inbox") msg.fromHandle ?: "?" else "→ ${msg.toHandle ?: "?"}",
                                color = BbsColors.Yellow, fontFamily = MonoFontFamily, fontSize = 12.sp)
                            Text(msg.subject, style = BbsTypography.bodyMedium,
                                color = if (msg.readAt == null && vm.box == "inbox") BbsColors.FgBright else BbsColors.Fg,
                                fontWeight = if (msg.readAt == null && vm.box == "inbox") FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(msg.createdAt.take(10), style = BbsTypography.labelSmall)
                    }
                    HorizontalDivider(color = BbsColors.BorderDim)
                }
            }
        }
    }
}

@Composable
fun MessageDetail(msg: PrivateMessage, onBack: () -> Unit, onDelete: () -> Unit, onReply: () -> Unit) {
    Scaffold(containerColor = BbsColors.Bg, topBar = { BbsTopBar(msg.subject, onBack = onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.fillMaxWidth().background(BbsColors.BgSurface).padding(16.dp)) {
                Text("From: ${msg.fromHandle ?: "?"}", color = BbsColors.Yellow, fontFamily = MonoFontFamily, fontSize = 13.sp)
                Text(msg.createdAt, style = BbsTypography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            }
            HorizontalDivider(color = BbsColors.Border)
            Text(msg.body ?: "", style = BbsTypography.bodyMedium,
                modifier = Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()))
            Row(Modifier.fillMaxWidth().background(BbsColors.BgSurface).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BbsButton("[ REPLY ]", onReply, Modifier.weight(1f))
                OutlinedButton(onClick = onDelete, border = BorderStroke(1.dp, BbsColors.Red), colors = ButtonDefaults.outlinedButtonColors(contentColor = BbsColors.Red), modifier = Modifier.weight(1f)) {
                    Text("[ DELETE ]", fontFamily = MonoFontFamily, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ComposeMessage(onSend: (String, String, String) -> Unit, onCancel: () -> Unit) {
    var to      by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body    by remember { mutableStateOf("") }

    Scaffold(containerColor = BbsColors.Bg, topBar = { BbsTopBar("COMPOSE", onBack = onCancel) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BbsTextField(to,      { to = it },      "TO (handle)")
            BbsTextField(subject, { subject = it }, "SUBJECT")
            BbsTextField(body,    { body = it },    "MESSAGE", singleLine = false, maxLines = 12, imeAction = ImeAction.Default)
            BbsButton("[ SEND MESSAGE ]", onClick = { onSend(to, subject, body) },
                modifier = Modifier.fillMaxWidth(),
                enabled  = to.isNotBlank() && subject.isNotBlank() && body.isNotBlank())
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SEARCH SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class SearchViewModel @Inject constructor(private val api: BbsApiClient) : ViewModel() {
    var query   by mutableStateOf("")
    var results by mutableStateOf<SearchResults?>(null)
    var loading by mutableStateOf(false)

    fun search() {
        if (query.length < 2) return
        viewModelScope.launch {
            loading = true
            try { val r = api.search(query); if (r.ok) results = r.data }
            catch (_: Exception) {}
            finally { loading = false }
        }
    }
}

@Composable
fun SearchScreen(navController: NavController, vm: SearchViewModel = hiltViewModel()) {
    val keyboard = LocalSoftwareKeyboardController.current
    Scaffold(containerColor = BbsColors.Bg, topBar = { BbsTopBar("SEARCH") }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.fillMaxWidth().background(BbsColors.BgSurface).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vm.query, onValueChange = { vm.query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search boards & posts...", color = BbsColors.FgDim, fontFamily = MonoFontFamily, fontSize = 13.sp) },
                    textStyle   = BbsTypography.bodyMedium,
                    singleLine  = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.search(); keyboard?.hide() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BbsColors.Cyan, unfocusedBorderColor = BbsColors.Border,
                        focusedContainerColor = BbsColors.BgRaised, unfocusedContainerColor = BbsColors.BgRaised,
                        cursorColor = BbsColors.Cyan, focusedTextColor = BbsColors.Fg, unfocusedTextColor = BbsColors.Fg,
                    ),
                )
                IconButton(onClick = { vm.search(); keyboard?.hide() }) { Icon(Icons.Default.Search, "Search", tint = BbsColors.Cyan) }
            }
            HorizontalDivider(color = BbsColors.Border)

            if (vm.loading) { CenterLoader(); return@Scaffold }

            vm.results?.let { results ->
                if (results.total == 0) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No results for \"${vm.query}\"", color = BbsColors.FgDim, fontFamily = MonoFontFamily)
                    }
                    return@let
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    if (results.threads.isNotEmpty()) {
                        item { Text("▸ THREADS (${results.threads.size})", style = BbsTypography.labelMedium.copy(color = BbsColors.Yellow), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                        items(results.threads) { thread ->
                            Column(Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Thread.go(thread.id)) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text(thread.title, style = BbsTypography.bodyLarge, color = BbsColors.Cyan)
                                Text("by ${thread.authorHandle} · ${thread.replyCount} replies", style = BbsTypography.bodySmall)
                            }
                            HorizontalDivider(color = BbsColors.BorderDim)
                        }
                    }
                    if (results.posts.isNotEmpty()) {
                        item { Text("▸ POSTS (${results.posts.size})", style = BbsTypography.labelMedium.copy(color = BbsColors.Yellow), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                        items(results.posts) { post ->
                            Column(Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Thread.go(post.id)) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text(post.body.take(120), style = BbsTypography.bodySmall.copy(color = BbsColors.Fg), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("by ${post.authorHandle}", style = BbsTypography.bodySmall)
                            }
                            HorizontalDivider(color = BbsColors.BorderDim)
                        }
                    }
                }
            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Type to search boards, threads, and posts.", color = BbsColors.FgDim, fontFamily = MonoFontFamily, fontSize = 12.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  PROFILE SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: BbsApiClient,
    val tokenStore: TokenStore,
) : ViewModel() {
    var user    by mutableStateOf<User?>(null)
    var loading by mutableStateOf(true)
    var editing by mutableStateOf(false)
    var bio     by mutableStateOf("")
    var location by mutableStateOf("")

    init { load() }

    fun load() {
        viewModelScope.launch {
            loading = true
            try {
                val r = api.me()
                if (r.ok) {
                    val u = r.data?.get("user")
                    user     = u
                    bio      = u?.bio ?: ""
                    location = u?.location ?: ""
                }
            } catch (_: Exception) {}
            finally { loading = false }
        }
    }

    fun save() {
        viewModelScope.launch {
            try {
                api.updateProfile(bio, location)
                editing = false
                load()
            } catch (_: Exception) {}
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) { tokenStore.clear() }
            onDone()
        }
    }
}

@Composable
fun ProfileScreen(navController: NavController, vm: ProfileViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    Scaffold(containerColor = BbsColors.Bg, topBar = { BbsTopBar("MY PROFILE") }) { pad ->
        if (vm.loading) { CenterLoader(); return@Scaffold }
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Handle card
            Surface(color = BbsColors.BgSurface, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, BbsColors.Cyan)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(vm.user?.handle ?: "...", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold))
                    Text(roleDisplay(vm.user?.role ?: "new"), color = BbsColors.Yellow, fontFamily = MonoFontFamily, fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(vm.user?.email ?: "", style = BbsTypography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }

            // Edit section
            Surface(color = BbsColors.BgSurface, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, BbsColors.Border)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("PROFILE", style = BbsTypography.labelSmall.copy(letterSpacing = 3.sp), color = BbsColors.Cyan)
                        if (!vm.editing) {
                            TextButton(onClick = { vm.editing = true }) { Text("EDIT", color = BbsColors.Cyan, fontFamily = MonoFontFamily, fontSize = 12.sp) }
                        }
                    }
                    if (vm.editing) {
                        BbsTextField(vm.bio,      { vm.bio = it },      "BIO",      singleLine = false, maxLines = 4, imeAction = ImeAction.Default)
                        BbsTextField(vm.location, { vm.location = it }, "LOCATION")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BbsButton("[ SAVE ]",   onClick = { vm.save() },         modifier = Modifier.weight(1f))
                            BbsButton("[ CANCEL ]", onClick = { vm.editing = false }, modifier = Modifier.weight(1f))
                        }
                    } else {
                        if (!vm.bio.isNullOrBlank()) Text(vm.bio, style = BbsTypography.bodyMedium)
                        if (!vm.location.isNullOrBlank()) Text("📍 ${vm.location}", style = BbsTypography.bodySmall)
                    }
                }
            }

            // Stats
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    "POSTS" to "${vm.user?.postCount ?: 0}",
                    "ROLE"  to roleDisplay(vm.user?.role ?: "new"),
                ).forEach { (label, value) ->
                    Surface(color = BbsColors.BgSurface, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, BbsColors.Border), modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(value, color = BbsColors.Cyan, fontFamily = MonoFontFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(label, style = BbsTypography.labelSmall)
                        }
                    }
                }
            }

            // Logout
            OutlinedButton(
                onClick = { vm.logout { navController.navigate(Screen.Login.route) { popUpTo(0) } } },
                border  = BorderStroke(1.dp, BbsColors.Red),
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = BbsColors.Red),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("[ LOG OUT ]", fontFamily = MonoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}
