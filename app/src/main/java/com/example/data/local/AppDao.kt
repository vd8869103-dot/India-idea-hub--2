package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Users ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)


    // --- Ideas ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdea(idea: IdeaEntity): Long

    @Delete
    suspend fun deleteIdea(idea: IdeaEntity)

    @Query("SELECT * FROM ideas WHERE id = :id")
    fun getIdeaById(id: Long): Flow<IdeaEntity?>

    @Query("SELECT * FROM ideas WHERE status = 'PUBLISHED' ORDER BY timestamp DESC")
    fun getAllPublishedIdeas(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas WHERE status = 'PUBLISHED' AND category = :category ORDER BY timestamp DESC")
    fun getIdeasByCategory(category: String): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas WHERE creatorId = :creatorId AND status = 'PUBLISHED' ORDER BY timestamp DESC")
    fun getPublishedIdeasByCreator(creatorId: String): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas WHERE creatorId = :creatorId AND status = 'DRAFT' ORDER BY timestamp DESC")
    fun getDraftIdeasByCreator(creatorId: String): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas WHERE isReported = 1 ORDER BY timestamp DESC")
    fun getReportedIdeas(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas WHERE status = 'PUBLISHED' AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')")
    fun searchIdeas(query: String): Flow<List<IdeaEntity>>

    @Query("UPDATE ideas SET likesCount = likesCount + :delta WHERE id = :ideaId")
    suspend fun updateLikesCount(ideaId: Long, delta: Int)

    @Query("UPDATE ideas SET commentsCount = commentsCount + :delta WHERE id = :ideaId")
    suspend fun updateCommentsCount(ideaId: Long, delta: Int)

    @Query("UPDATE ideas SET viewsCount = viewsCount + 1 WHERE id = :ideaId")
    suspend fun incrementViewsCount(ideaId: Long)

    @Query("UPDATE ideas SET isReported = :reported, reportReason = :reason WHERE id = :ideaId")
    suspend fun reportIdea(ideaId: Long, reported: Boolean, reason: String)


    // --- Comments ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("SELECT * FROM comments WHERE ideaId = :ideaId ORDER BY timestamp ASC")
    fun getCommentsForIdea(ideaId: Long): Flow<List<CommentEntity>>


    // --- Likes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Delete
    suspend fun deleteLike(like: LikeEntity)

    @Query("SELECT * FROM likes WHERE userId = :userId AND ideaId = :ideaId")
    fun getLike(userId: String, ideaId: Long): Flow<LikeEntity?>


    // --- Bookmarks ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks WHERE userId = :userId AND ideaId = :ideaId")
    fun getBookmark(userId: String, ideaId: Long): Flow<BookmarkEntity?>

    @Query("SELECT i.* FROM ideas i INNER JOIN bookmarks b ON i.id = b.ideaId WHERE b.userId = :userId AND i.status = 'PUBLISHED'")
    fun getBookmarkedIdeas(userId: String): Flow<List<IdeaEntity>>


    // --- Meeting Requests ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeetingRequest(request: MeetingRequestEntity)

    @Query("SELECT * FROM meeting_requests WHERE creatorId = :creatorId ORDER BY timestamp DESC")
    fun getMeetingRequestsForCreator(creatorId: String): Flow<List<MeetingRequestEntity>>

    @Query("SELECT * FROM meeting_requests WHERE investorId = :investorId ORDER BY timestamp DESC")
    fun getMeetingRequestsForInvestor(investorId: String): Flow<List<MeetingRequestEntity>>

    @Query("UPDATE meeting_requests SET status = :status WHERE id = :requestId")
    suspend fun updateMeetingRequestStatus(requestId: Long, status: String)


    // --- Notifications ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: String): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markNotificationAsRead(notificationId: Long)
}
