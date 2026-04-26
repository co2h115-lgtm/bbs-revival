package com.bbsrevival.data.api

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bbsrevival.BuildConfig
import com.bbsrevival.data.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context, "bbs_tokens", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(v) = if (v == null) prefs.edit().remove("access_token").apply()
                 else prefs.edit().putString("access_token", v).apply()

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(v) = if (v == null) prefs.edit().remove("refresh_token").apply()
                 else prefs.edit().putString("refresh_token", v).apply()

    fun clear() { prefs.edit().clear().apply() }
}

@Singleton
class BbsApiClient @Inject constructor(private val tokenStore: TokenStore) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient         = true
        coerceInputValues = true
    }

    val http = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            level  = LogLevel.HEADERS
            logger = Logger.ANDROID
        }
        install(HttpTimeout) {
            requestTimeoutMillis  = 30_000
            connectTimeoutMillis  = 15_000
        }
        defaultRequest {
            url(BuildConfig.API_BASE_URL)
            contentType(ContentType.Application.Json)
            tokenStore.accessToken?.let { bearerAuth(it) }
        }
    }

    // ── Auth endpoints ────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): AuthResponse {
        val res: AuthResponse = http.post("/api/auth/login") {
            setBody(mapOf("email" to email, "password" to password))
        }.body()
        res.data?.let { tokenStore.accessToken = it.accessToken; tokenStore.refreshToken = it.refreshToken }
        return res
    }

    suspend fun register(handle: String, email: String, password: String): AuthResponse {
        val res: AuthResponse = http.post("/api/auth/register") {
            setBody(mapOf("handle" to handle, "email" to email, "password" to password))
        }.body()
        res.data?.let { tokenStore.accessToken = it.accessToken; tokenStore.refreshToken = it.refreshToken }
        return res
    }

    suspend fun me(): ApiResponse<Map<String, User>> = http.get("/api/auth/me").body()

    suspend fun logout() {
        http.post("/api/auth/logout") {
            setBody(mapOf("refreshToken" to (tokenStore.refreshToken ?: "")))
        }
        tokenStore.clear()
    }

    // ── Boards ────────────────────────────────────────────────────────────────

    suspend fun getBoards(): ApiResponse<List<BoardGroup>> = http.get("/api/boards").body()

    suspend fun getThreads(boardId: String, page: Int = 1): ApiResponse<ThreadListData> =
        http.get("/api/boards/$boardId/threads") { parameter("page", page) }.body()

    suspend fun getThread(threadId: String): ApiResponse<ThreadDetail> =
        http.get("/api/boards/threads/$threadId").body()

    suspend fun getPosts(threadId: String, page: Int = 1): ApiResponse<PostListData> =
        http.get("/api/boards/threads/$threadId/posts") { parameter("page", page) }.body()

    suspend fun createThread(boardId: String, title: String, body: String): ApiResponse<CreateResult> =
        http.post("/api/boards/$boardId/threads") {
            setBody(mapOf("title" to title, "body" to body))
        }.body()

    suspend fun createPost(threadId: String, body: String, quotePostId: String? = null): ApiResponse<CreateResult> =
        http.post("/api/boards/threads/$threadId/posts") {
            setBody(buildMap {
                put("body", body)
                quotePostId?.let { put("quotePostId", it) }
            })
        }.body()

    suspend fun deletePost(postId: String): ApiResponse<Unit> =
        http.delete("/api/boards/posts/$postId").body()

    suspend fun updateThread(threadId: String, locked: Boolean? = null, pinned: Boolean? = null): ApiResponse<Unit> =
        http.patch("/api/boards/threads/$threadId") {
            setBody(buildMap {
                locked?.let { put("locked", it) }
                pinned?.let { put("pinned", it) }
            })
        }.body()

    // ── Chat ──────────────────────────────────────────────────────────────────

    suspend fun getChatRooms(): ApiResponse<List<ChatRoom>> = http.get("/api/chat/rooms").body()

    suspend fun getChatHistory(roomId: String, limit: Int = 50): ApiResponse<List<ChatMessage>> =
        http.get("/api/chat/rooms/$roomId/history") { parameter("limit", limit) }.body()

    // ── Files ─────────────────────────────────────────────────────────────────

    suspend fun getFileAreas(): ApiResponse<List<FileArea>> = http.get("/api/files/areas").body()

    suspend fun getAreaFiles(areaId: String, page: Int = 1, search: String? = null): ApiResponse<FileListData> =
        http.get("/api/files/areas/$areaId") {
            parameter("page", page)
            search?.let { parameter("search", it) }
        }.body()

    suspend fun rateFile(fileId: String, rating: Int): ApiResponse<Unit> =
        http.post("/api/files/$fileId/rate") { setBody(mapOf("rating" to rating)) }.body()

    suspend fun approveFile(fileId: String): ApiResponse<Unit> =
        http.post("/api/files/$fileId/approve").body()

    suspend fun deleteFile(fileId: String): ApiResponse<Unit> =
        http.delete("/api/files/$fileId").body()

    fun downloadUrl(fileId: String): String =
        "${BuildConfig.API_BASE_URL}/api/files/$fileId/download"

    // ── Gallery ───────────────────────────────────────────────────────────────

    suspend fun getGallery(page: Int = 1, sort: String = "newest"): ApiResponse<GalleryListData> =
        http.get("/api/gallery") { parameter("page", page); parameter("sort", sort) }.body()

    suspend fun getGalleryItem(id: String): ApiResponse<GalleryItem> =
        http.get("/api/gallery/$id").body()

    suspend fun likeGalleryItem(id: String): ApiResponse<LikeResult> =
        http.post("/api/gallery/$id/like").body()

    suspend fun createGalleryItem(title: String, description: String, content: String, tags: List<String>): ApiResponse<CreateResult> =
        http.post("/api/gallery") {
            setBody(mapOf("title" to title, "description" to description, "content" to content, "tags" to tags, "width" to 80))
        }.body()

    // ── Door Games ────────────────────────────────────────────────────────────

    suspend fun getDoorGames(): ApiResponse<List<DoorGame>> = http.get("/api/doors").body()

    suspend fun getLeaderboard(gameId: String): ApiResponse<List<LeaderboardEntry>> =
        http.get("/api/doors/$gameId/leaderboard").body()

    // ── Private Messages ──────────────────────────────────────────────────────

    suspend fun getInbox(page: Int = 1): ApiResponse<MessageListData> =
        http.get("/api/messages") { parameter("page", page) }.body()

    suspend fun getSent(page: Int = 1): ApiResponse<MessageListData> =
        http.get("/api/messages") { parameter("page", page); parameter("box", "sent") }.body()

    suspend fun getMessage(id: String): ApiResponse<PrivateMessage> =
        http.get("/api/messages/$id").body()

    suspend fun sendMessage(toHandle: String, subject: String, body: String): ApiResponse<CreateResult> =
        http.post("/api/messages") {
            setBody(mapOf("toHandle" to toHandle, "subject" to subject, "body" to body))
        }.body()

    suspend fun deleteMessage(id: String): ApiResponse<Unit> =
        http.delete("/api/messages/$id").body()

    suspend fun getUnreadCount(): ApiResponse<UnreadCount> =
        http.get("/api/messages/unread").body()

    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun search(query: String, type: String = "all"): ApiResponse<SearchResults> =
        http.get("/api/search") { parameter("q", query); parameter("type", type) }.body()

    // ── Profile ───────────────────────────────────────────────────────────────

    suspend fun updateProfile(bio: String?, location: String?): ApiResponse<Unit> =
        http.patch("/api/users/me") {
            setBody(buildMap {
                bio?.let { put("bio", it) }
                location?.let { put("location", it) }
            })
        }.body()
}

// ── Extra response shapes ─────────────────────────────────────────────────────
@kotlinx.serialization.Serializable
data class ThreadListData(val threads: List<Thread>, val total: Int, val page: Int, val limit: Int, val board: Board? = null)

@kotlinx.serialization.Serializable
data class ThreadDetail(val id: String, val title: String, @kotlinx.serialization.SerialName("board_id") val boardId: String = "", @kotlinx.serialization.SerialName("board_name") val boardName: String = "", val locked: Boolean = false, val pinned: Boolean = false, @kotlinx.serialization.SerialName("reply_count") val replyCount: Int = 0, @kotlinx.serialization.SerialName("author_handle") val authorHandle: String = "", @kotlinx.serialization.SerialName("created_at") val createdAt: String = "")

@kotlinx.serialization.Serializable
data class PostListData(val posts: List<Post>, val total: Int, val page: Int, val limit: Int)

@kotlinx.serialization.Serializable
data class FileListData(val files: List<FileListing>, val total: Int, val page: Int, val limit: Int)

@kotlinx.serialization.Serializable
data class GalleryListData(val items: List<GalleryItem>, val total: Int, val page: Int, val limit: Int)

@kotlinx.serialization.Serializable
data class CreateResult(val id: String? = null, @kotlinx.serialization.SerialName("threadId") val threadId: String? = null, @kotlinx.serialization.SerialName("postId") val postId: String? = null, @kotlinx.serialization.SerialName("messageId") val messageId: String? = null)

@kotlinx.serialization.Serializable
data class LikeResult(val likes: Int)

@kotlinx.serialization.Serializable
data class MessageListData(val messages: List<PrivateMessage>, val total: Int = 0, val unread: Int = 0, val page: Int = 1, val limit: Int = 25)

@kotlinx.serialization.Serializable
data class UnreadCount(val count: Int)

@kotlinx.serialization.Serializable
data class SearchResults(val threads: List<Thread> = emptyList(), val posts: List<Post> = emptyList(), val total: Int = 0)
