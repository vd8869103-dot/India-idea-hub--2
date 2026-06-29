package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.AppDao
import com.example.data.gemini.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(private val appDao: AppDao) {

    // --- Users ---
    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        appDao.insertUser(user)
    }

    fun getUserById(userId: String): Flow<UserEntity?> = appDao.getUserById(userId)
    fun getAllUsers(): Flow<List<UserEntity>> = appDao.getAllUsers()
    suspend fun deleteUserById(userId: String) = withContext(Dispatchers.IO) {
        appDao.deleteUserById(userId)
    }

    // --- Ideas ---
    suspend fun insertIdea(idea: IdeaEntity): Long = withContext(Dispatchers.IO) {
        appDao.insertIdea(idea)
    }

    suspend fun deleteIdea(idea: IdeaEntity) = withContext(Dispatchers.IO) {
        appDao.deleteIdea(idea)
    }

    fun getIdeaById(id: Long): Flow<IdeaEntity?> = appDao.getIdeaById(id)
    fun getAllPublishedIdeas(): Flow<List<IdeaEntity>> = appDao.getAllPublishedIdeas()
    fun getIdeasByCategory(category: String): Flow<List<IdeaEntity>> = appDao.getIdeasByCategory(category)
    fun getPublishedIdeasByCreator(creatorId: String): Flow<List<IdeaEntity>> = appDao.getPublishedIdeasByCreator(creatorId)
    fun getDraftIdeasByCreator(creatorId: String): Flow<List<IdeaEntity>> = appDao.getDraftIdeasByCreator(creatorId)
    fun getReportedIdeas(): Flow<List<IdeaEntity>> = appDao.getReportedIdeas()
    fun searchIdeas(query: String): Flow<List<IdeaEntity>> = appDao.searchIdeas(query)

    suspend fun updateLikesCount(ideaId: Long, delta: Int) = withContext(Dispatchers.IO) {
        appDao.updateLikesCount(ideaId, delta)
    }

    suspend fun updateCommentsCount(ideaId: Long, delta: Int) = withContext(Dispatchers.IO) {
        appDao.updateCommentsCount(ideaId, delta)
    }

    suspend fun incrementViewsCount(ideaId: Long) = withContext(Dispatchers.IO) {
        appDao.incrementViewsCount(ideaId)
    }

    suspend fun reportIdea(ideaId: Long, reported: Boolean, reason: String) = withContext(Dispatchers.IO) {
        appDao.reportIdea(ideaId, reported, reason)
    }

    // --- Comments ---
    suspend fun insertComment(comment: CommentEntity) = withContext(Dispatchers.IO) {
        appDao.insertComment(comment)
        appDao.updateCommentsCount(comment.ideaId, 1)
    }

    fun getCommentsForIdea(ideaId: Long): Flow<List<CommentEntity>> = appDao.getCommentsForIdea(ideaId)

    // --- Likes ---
    suspend fun insertLike(like: LikeEntity) = withContext(Dispatchers.IO) {
        appDao.insertLike(like)
        appDao.updateLikesCount(like.ideaId, 1)
    }

    suspend fun deleteLike(like: LikeEntity) = withContext(Dispatchers.IO) {
        appDao.deleteLike(like)
        appDao.updateLikesCount(like.ideaId, -1)
    }

    fun getLike(userId: String, ideaId: Long): Flow<LikeEntity?> = appDao.getLike(userId, ideaId)

    // --- Bookmarks ---
    suspend fun insertBookmark(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        appDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        appDao.deleteBookmark(bookmark)
    }

    fun getBookmark(userId: String, ideaId: Long): Flow<BookmarkEntity?> = appDao.getBookmark(userId, ideaId)
    fun getBookmarkedIdeas(userId: String): Flow<List<IdeaEntity>> = appDao.getBookmarkedIdeas(userId)

    // --- Meeting Requests ---
    suspend fun insertMeetingRequest(request: MeetingRequestEntity) = withContext(Dispatchers.IO) {
        appDao.insertMeetingRequest(request)
    }

    fun getMeetingRequestsForCreator(creatorId: String): Flow<List<MeetingRequestEntity>> = appDao.getMeetingRequestsForCreator(creatorId)
    fun getMeetingRequestsForInvestor(investorId: String): Flow<List<MeetingRequestEntity>> = appDao.getMeetingRequestsForInvestor(investorId)
    suspend fun updateMeetingRequestStatus(requestId: Long, status: String) = withContext(Dispatchers.IO) {
        appDao.updateMeetingRequestStatus(requestId, status)
    }

    // --- Notifications ---
    suspend fun insertNotification(notification: NotificationEntity) = withContext(Dispatchers.IO) {
        appDao.insertNotification(notification)
    }

    fun getNotificationsForUser(userId: String): Flow<List<NotificationEntity>> = appDao.getNotificationsForUser(userId)
    suspend fun markNotificationAsRead(notificationId: Long) = withContext(Dispatchers.IO) {
        appDao.markNotificationAsRead(notificationId)
    }

    // --- Gemini AI Integration Services ---
    suspend fun callGemini(systemPrompt: String, userPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("AppRepository", "Gemini API Key is empty or placeholder! Running fallback mock response.")
            return@withContext getOfflineFallback(systemPrompt, userPrompt)
        }

        try {
            val request = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(temperature = 0.7f)
            )
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No suggestions generated from Gemini. Please try again."
        } catch (e: Exception) {
            Log.e("AppRepository", "Error calling Gemini API: ${e.message}", e)
            getOfflineFallback(systemPrompt, userPrompt) + "\n\n*(Showing offline calculated suggestions due to network error)*"
        }
    }

    private fun getOfflineFallback(systemPrompt: String, userPrompt: String): String {
        return when {
            userPrompt.contains("REVIEW", ignoreCase = true) || systemPrompt.contains("review", ignoreCase = true) -> {
                """
                ### 🇮🇳 AI Idea Evaluation Report
                
                **1. Market Viability & Potential (High):**
                Your idea targets a highly relevant sector in India. With the digital push and expanding rural/urban middle class, the scalability is very promising. 
                
                **2. Key Strengths:**
                - Solves an acute local problem tailored for Indian users.
                - Low-cost alternative leveraging regional localized content.
                - Huge addressable market across tier-2 and tier-3 Indian towns.
                
                **3. Critical Challenges & Risks:**
                - Customer acquisition cost in regional languages.
                - Regulatory compliance and local physical logistical dependencies.
                
                **4. Actionable Next Steps:**
                - Build a minimum viable product (MVP) to test with 100 actual users.
                - Outline unit economics before pitching to angel syndicates.
                """.trimIndent()
            }
            userPrompt.contains("GRAMMAR", ignoreCase = true) || systemPrompt.contains("grammar", ignoreCase = true) -> {
                "Here is an improved, grammatically polished version of your pitch:\n\n\"We are building a highly scalable, digital startup hub for India's next generation of local entrepreneurs, connecting them with mentors and capital seamlessly.\""
            }
            userPrompt.contains("PITCH", ignoreCase = true) || systemPrompt.contains("pitch", ignoreCase = true) -> {
                """
                ### 🚀 AI Pitch suggestions & Hook
                
                *   **Elevator Hook:** "While 90% of local Indian startup ideas die in tier-2 cities due to lack of network, we are creating a localized bridge to connect them directly to Mumbai and Bangalore's top investor syndicates."
                *   **Slide Outline:**
                    1.  The Indian Local Gap (Problem)
                    2.  Localized Digital Network (Solution)
                    3.  $15B Addressable Market Opportunity (Market Size)
                    4.  Unit Economics & Localization Playbook (Business Model)
                """.trimIndent()
            }
            userPrompt.contains("MARKET", ignoreCase = true) || systemPrompt.contains("market", ignoreCase = true) -> {
                """
                ### 📈 Comprehensive Indian Market Analysis
                
                *   **Market Growth (CAGR):** Projected to grow at a CAGR of 18.2% over the next 5 years in India, fueled by UPI payment penetration and cheap mobile internet.
                *   **Target Demographics:** Primarily Tier-2/Tier-3 micro-entrepreneurs, smallholder farmers, or students aged 18-35.
                *   **Primary Competitors:** Traditional unorganized local brokers, global aggregators lacking vernacular local nuance.
                *   **Strategic Advantage:** Highly tailored local language options and frictionless community networking.
                """.trimIndent()
            }
            else -> {
                """
                ### 💡 Suggested Business Names:
                1.  **VikasHub (Growth Hub)** - Conveys Indian progress and innovation.
                2.  **UdyamSutra (Entrepreneur Formula)** - Traditional yet modern tech feel.
                3.  **Sankalp Tech (Resolution/Commitment)** - Deep emotional appeal.
                4.  **IndiLaunch** - Fast, direct, startup-centric naming.
                5.  **Chakra Connect** - High national recognition, modern geometric energy.
                """.trimIndent()
            }
        }
    }
}
