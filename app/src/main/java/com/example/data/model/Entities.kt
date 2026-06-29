package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String,
    val role: String, // "CREATOR", "INVESTOR", "ADMIN"
    val bio: String = "",
    val skills: String = "", // Comma-separated list
    val socialLinks: String = "", // Comma-separated or JSON
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val avatarUrl: String = "avatar_1"
)

@Entity(tableName = "ideas")
data class IdeaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creatorId: String,
    val creatorName: String,
    val creatorAvatar: String = "avatar_1",
    val title: String,
    val description: String,
    val category: String, // Technology, AI, Agriculture, Healthcare, Education, Finance, Environment, E-commerce, Social Impact, Others
    val problemStatement: String,
    val solution: String,
    val targetAudience: String,
    val estimatedBudget: String,
    val pitchVideoUrl: String = "",
    val pitchDeckUrl: String = "",
    val imageUrl: String = "",
    val status: String, // "DRAFT", "PUBLISHED"
    val likesCount: Int = 0,
    val viewsCount: Int = 0,
    val commentsCount: Int = 0,
    val isReported: Boolean = false,
    val reportReason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ideaId: Long,
    val userId: String,
    val userName: String,
    val userAvatar: String = "avatar_1",
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "likes", primaryKeys = ["userId", "ideaId"])
data class LikeEntity(
    val userId: String,
    val ideaId: Long
)

@Entity(tableName = "bookmarks", primaryKeys = ["userId", "ideaId"])
data class BookmarkEntity(
    val userId: String,
    val ideaId: Long
)

@Entity(tableName = "meeting_requests")
data class MeetingRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val investorId: String,
    val investorName: String,
    val creatorId: String,
    val ideaId: Long,
    val ideaTitle: String,
    val message: String,
    val date: String,
    val time: String,
    val status: String, // "PENDING", "ACCEPTED", "REJECTED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
