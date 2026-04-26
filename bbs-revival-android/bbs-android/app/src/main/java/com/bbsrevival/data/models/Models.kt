package com.bbsrevival.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Auth ──────────────────────────────────────────────────────────────────────
@Serializable
data class User(
    val id: String,
    val handle: String,
    val email: String,
    val role: String,
    @SerialName("post_count") val postCount: Int = 0,
    val location: String? = null,
    val bio: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("last_seen") val lastSeen: String? = null,
)

@Serializable
data class AuthResponse(
    val ok: Boolean,
    val data: AuthData? = null,
    val error: String? = null,
)

@Serializable
data class AuthData(
    val user: User,
    @SerialName("accessToken")  val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
)

// ── Boards ────────────────────────────────────────────────────────────────────
@Serializable
data class BoardGroup(
    val id: String,
    val name: String,
    val boards: List<Board> = emptyList(),
)

@Serializable
data class Board(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("thread_count") val threadCount: Int = 0,
    @SerialName("post_count")   val postCount: Int = 0,
    @SerialName("last_post_at") val lastPostAt: String? = null,
    @SerialName("min_role")     val minRole: String = "new",
)

@Serializable
data class Thread(
    val id: String,
    val title: String,
    @SerialName("reply_count")   val replyCount: Int = 0,
    @SerialName("view_count")    val viewCount: Int = 0,
    val locked: Boolean = false,
    val pinned: Boolean = false,
    @SerialName("created_at")    val createdAt: String = "",
    @SerialName("last_post_at")  val lastPostAt: String = "",
    @SerialName("author_handle") val authorHandle: String = "",
    @SerialName("author_role")   val authorRole: String = "new",
    @SerialName("last_poster_handle") val lastPosterHandle: String? = null,
    val hasUnread: Boolean = false,
)

@Serializable
data class Post(
    val id: String,
    val body: String,
    @SerialName("created_at")        val createdAt: String = "",
    @SerialName("edited_at")         val editedAt: String? = null,
    @SerialName("author_id")         val authorId: String = "",
    @SerialName("author_handle")     val authorHandle: String = "",
    @SerialName("author_role")       val authorRole: String = "new",
    @SerialName("author_post_count") val authorPostCount: Int = 0,
    @SerialName("author_location")   val authorLocation: String? = null,
    @SerialName("author_joined")     val authorJoined: String = "",
)

// ── Chat ──────────────────────────────────────────────────────────────────────
@Serializable
data class ChatRoom(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("is_system")    val isSystem: Boolean = false,
    @SerialName("online_count") val onlineCount: Int = 0,
)

@Serializable
data class ChatMessage(
    val id: String,
    val roomId: String,
    val userId: String,
    val userHandle: String,
    val userRole: String,
    val body: String,
    val createdAt: String,
    val isSystem: Boolean = false,
)

// ── Files ─────────────────────────────────────────────────────────────────────
@Serializable
data class FileArea(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("min_role")   val minRole: String = "validated",
    @SerialName("file_count") val fileCount: Int = 0,
)

@Serializable
data class FileListing(
    val id: String,
    @SerialName("original_name")     val originalName: String,
    val description: String = "",
    @SerialName("size_bytes")        val sizeBytes: Long = 0,
    @SerialName("mime_type")         val mimeType: String = "",
    val tags: List<String> = emptyList(),
    val approved: Boolean = false,
    @SerialName("dl_count")          val dlCount: Int = 0,
    @SerialName("created_at")        val createdAt: String = "",
    @SerialName("uploader_handle")   val uploaderHandle: String = "",
    val score: Int = 0,
    @SerialName("thumbs_up")         val thumbsUp: Int = 0,
    @SerialName("thumbs_down")       val thumbsDown: Int = 0,
)

// ── Gallery ───────────────────────────────────────────────────────────────────
@Serializable
data class GalleryItem(
    val id: String,
    val title: String,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val likes: Int = 0,
    val width: Int = 80,
    @SerialName("created_at")    val createdAt: String = "",
    val preview: String = "",
    val content: String? = null,
    @SerialName("author_handle") val authorHandle: String = "",
    @SerialName("author_id")     val authorId: String = "",
    val liked: Boolean = false,
)

// ── Door Games ────────────────────────────────────────────────────────────────
@Serializable
data class DoorGame(
    val id: String,
    val slug: String,
    val name: String,
    val description: String = "",
    val author: String = "",
    @SerialName("min_role")   val minRole: String = "new",
    @SerialName("has_scores") val hasScores: Boolean = true,
    @SerialName("play_count") val playCount: Int = 0,
    @SerialName("top_score")  val topScore: Int? = null,
)

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    @SerialName("user_handle") val userHandle: String,
    val score: Int,
    @SerialName("achieved_at") val achievedAt: String,
)

// ── Private Messages ──────────────────────────────────────────────────────────
@Serializable
data class PrivateMessage(
    val id: String,
    val subject: String,
    @SerialName("created_at")   val createdAt: String = "",
    @SerialName("read_at")      val readAt: String? = null,
    @SerialName("from_handle")  val fromHandle: String? = null,
    @SerialName("to_handle")    val toHandle: String? = null,
    @SerialName("from_id")      val fromId: String? = null,
    @SerialName("to_id")        val toId: String? = null,
    val body: String? = null,
)

// ── Generic API wrapper ───────────────────────────────────────────────────────
@Serializable
data class ApiResponse<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: String? = null,
)

@Serializable
data class PagedData<T>(
    val total: Int,
    val page: Int,
    val limit: Int,
    val items: List<T> = emptyList(),  // generic name — map in repo layer
)
