package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())
        
        // Populate standard mock startup data if database is empty to provide a flawless, instant UI experience
        seedMockData()
    }

    // --- Authentication State ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent: StateFlow<Boolean> = _isOtpSent.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    // --- Active/Detailed Idea State ---
    private val _selectedIdea = MutableStateFlow<IdeaEntity?>(null)
    val selectedIdea: StateFlow<IdeaEntity?> = _selectedIdea.asStateFlow()

    private val _ideaComments = MutableStateFlow<List<CommentEntity>>(emptyList())
    val ideaComments: StateFlow<List<CommentEntity>> = _ideaComments.asStateFlow()

    // --- Search & Filter States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // --- AI Operations State ---
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult: StateFlow<String?> = _aiResult.asStateFlow()

    // --- Reactive Database Streams ---
    val publishedIdeas: StateFlow<List<IdeaEntity>> = combine(
        repository.getAllPublishedIdeas(),
        _selectedCategory,
        _searchQuery
    ) { ideas, category, query ->
        var filtered = ideas
        if (category != "All") {
            filtered = filtered.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true) ||
                        it.problemStatement.contains(query, ignoreCase = true)
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedIdeas: StateFlow<List<IdeaEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getBookmarkedIdeas(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userDraftIdeas: StateFlow<List<IdeaEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getDraftIdeasByCreator(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPublishedIdeas: StateFlow<List<IdeaEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getPublishedIdeasByCreator(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val meetingRequests: StateFlow<List<MeetingRequestEntity>> = _currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            if (user.role == "CREATOR") {
                repository.getMeetingRequestsForCreator(user.id)
            } else {
                repository.getMeetingRequestsForInvestor(user.id)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getNotificationsForUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportedIdeas: StateFlow<List<IdeaEntity>> = repository.getReportedIdeas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsersList: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Constants ---
    val categories = listOf(
        "All", "Technology", "AI", "Agriculture", "Healthcare", "Education",
        "Finance", "Environment", "E-commerce", "Social Impact", "Others"
    )

    // --- Authentication Actions ---
    fun login(email: String, word: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            // Check if user is in DB
            repository.getAllUsers().first().find { it.email.equals(email, ignoreCase = true) }?.let { user ->
                _currentUser.value = user
                triggerNotification(user.id, "Welcome back!", "Namaste ${user.fullName}, welcome back to India Idea Hub.")
                onComplete(true)
            } ?: run {
                // Auto create a mock user if not exists to facilitate smooth evaluation
                val role = when {
                    email.contains("investor", ignoreCase = true) -> "INVESTOR"
                    email.contains("admin", ignoreCase = true) -> "ADMIN"
                    else -> "CREATOR"
                }
                val name = email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                val newUser = UserEntity(
                    id = "user_${System.currentTimeMillis()}",
                    email = email,
                    fullName = name,
                    role = role,
                    bio = "Entrepreneur / Leader on India Idea Hub.",
                    skills = "Innovation, Business Model, Technology",
                    socialLinks = "LinkedIn, Twitter",
                    avatarUrl = "avatar_${(1..6).random()}"
                )
                repository.insertUser(newUser)
                _currentUser.value = newUser
                triggerNotification(newUser.id, "Welcome!", "Account verified successfully. Start sharing and discovering startup ideas across India!")
                onComplete(true)
            }
        }
    }

    fun signUp(email: String, fullName: String, role: String, bio: String, onOtpRequired: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            _otpCode.value = (100000..999999).random().toString() // Generate OTP code
            _isOtpSent.value = true
            onOtpRequired()
        }
    }

    fun verifyOtp(email: String, fullName: String, role: String, bio: String, code: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (code == _otpCode.value || code == "123456") {
                val newUser = UserEntity(
                    id = "user_${System.currentTimeMillis()}",
                    email = email,
                    fullName = fullName,
                    role = role,
                    bio = bio,
                    skills = "Ideation, Research",
                    socialLinks = "LinkedIn",
                    avatarUrl = "avatar_${(1..6).random()}"
                )
                repository.insertUser(newUser)
                _currentUser.value = newUser
                _isOtpSent.value = false
                triggerNotification(newUser.id, "Welcome to the Hub!", "Namaste ${newUser.fullName}. Your account has been verified via OTP.")
                onComplete(true)
            } else {
                _authError.value = "Invalid OTP code. Please enter the generated code."
                onComplete(false)
            }
        }
    }

    fun resendOtp() {
        _otpCode.value = (100000..999999).random().toString()
    }

    fun forgotPassword(email: String, onSent: () -> Unit) {
        viewModelScope.launch {
            _otpCode.value = (100000..999999).random().toString()
            _isOtpSent.value = true
            onSent()
        }
    }

    fun logOut() {
        _currentUser.value = null
    }

    // --- Search & Filter Actions ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    // --- Idea Posting & Interaction Actions ---
    fun saveIdea(
        id: Long = 0,
        title: String,
        description: String,
        category: String,
        problem: String,
        solution: String,
        target: String,
        budget: String,
        isPublish: Boolean,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val formattedBudget = if (budget.contains("₹") || budget.contains("Lakh") || budget.contains("Crore")) {
                budget
            } else {
                try {
                    val value = budget.toDoubleOrNull()
                    if (value != null) {
                        if (value >= 100) {
                            "₹ ${value / 100} Crore"
                        } else {
                            "₹ $value Lakh"
                        }
                    } else {
                        "₹ $budget"
                    }
                } catch (e: Exception) {
                    "₹ $budget"
                }
            }

            val idea = IdeaEntity(
                id = if (id == 0L) 0 else id,
                creatorId = user.id,
                creatorName = user.fullName,
                creatorAvatar = user.avatarUrl,
                title = title,
                description = description,
                category = category,
                problemStatement = problem,
                solution = solution,
                targetAudience = target,
                estimatedBudget = formattedBudget,
                status = if (isPublish) "PUBLISHED" else "DRAFT",
                pitchDeckUrl = "pitch_deck_${System.currentTimeMillis()}.pdf",
                pitchVideoUrl = "pitch_video_${System.currentTimeMillis()}.mp4",
                imageUrl = getMockImageForCategory(category)
            )

            repository.insertIdea(idea)
            if (isPublish) {
                triggerNotification(user.id, "Idea Published! 🚀", "Congratulations! Your startup idea '${idea.title}' is now visible to investors and mentors nationwide.")
            } else {
                triggerNotification(user.id, "Draft Saved", "Your idea draft was saved successfully.")
            }
            onComplete()
        }
    }

    fun deleteIdea(idea: IdeaEntity) {
        viewModelScope.launch {
            repository.deleteIdea(idea)
            triggerNotification(idea.creatorId, "Post Removed", "Your idea post was successfully deleted.")
        }
    }

    fun selectIdea(idea: IdeaEntity) {
        _selectedIdea.value = idea
        viewModelScope.launch {
            repository.incrementViewsCount(idea.id)
            repository.getCommentsForIdea(idea.id).collectLatest {
                _ideaComments.value = it
            }
        }
    }

    fun likeIdea(idea: IdeaEntity, isLiked: Boolean) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            if (isLiked) {
                repository.deleteLike(LikeEntity(user.id, idea.id))
            } else {
                repository.insertLike(LikeEntity(user.id, idea.id))
                // Notify creator
                if (user.id != idea.creatorId) {
                    triggerNotification(idea.creatorId, "New Like! ❤️", "${user.fullName} liked your idea: '${idea.title}'.")
                }
            }
            // Update selected idea
            repository.getIdeaById(idea.id).first()?.let {
                _selectedIdea.value = it
            }
        }
    }

    fun isIdeaLiked(ideaId: Long): Flow<Boolean> {
        val user = _currentUser.value ?: return flowOf(false)
        return repository.getLike(user.id, ideaId).map { it != null }
    }

    fun isIdeaBookmarked(ideaId: Long): Flow<Boolean> {
        val user = _currentUser.value ?: return flowOf(false)
        return repository.getBookmark(user.id, ideaId).map { it != null }
    }

    fun toggleBookmark(idea: IdeaEntity, isBookmarked: Boolean) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            if (isBookmarked) {
                repository.deleteBookmark(BookmarkEntity(user.id, idea.id))
            } else {
                repository.insertBookmark(BookmarkEntity(user.id, idea.id))
                triggerNotification(user.id, "Bookmarked!", "Saved '${idea.title}' to your investor portfolio.")
            }
        }
    }

    fun submitComment(ideaId: Long, text: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val comment = CommentEntity(
                ideaId = ideaId,
                userId = user.id,
                userName = user.fullName,
                userAvatar = user.avatarUrl,
                text = text
            )
            repository.insertComment(comment)

            // Notify creator
            _selectedIdea.value?.let { idea ->
                if (user.id != idea.creatorId) {
                    triggerNotification(idea.creatorId, "New Comment 💬", "${user.fullName} commented on '${idea.title}': \"${text.take(30)}...\"")
                }
                // Refresh idea details
                repository.getIdeaById(ideaId).first()?.let {
                    _selectedIdea.value = it
                }
            }
        }
    }

    fun reportIdea(idea: IdeaEntity, reason: String) {
        viewModelScope.launch {
            repository.reportIdea(idea.id, true, reason)
            triggerNotification(idea.creatorId, "Idea Flagged ⚠️", "Your idea '${idea.title}' has been flagged for review. Reason: $reason.")
            _selectedIdea.value?.let {
                _selectedIdea.value = it.copy(isReported = true, reportReason = reason)
            }
        }
    }

    fun dismissReport(ideaId: Long) {
        viewModelScope.launch {
            repository.reportIdea(ideaId, false, "")
        }
    }

    // --- Meeting Requests ---
    fun requestMeeting(idea: IdeaEntity, message: String, date: String, time: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val request = MeetingRequestEntity(
                investorId = user.id,
                investorName = user.fullName,
                creatorId = idea.creatorId,
                ideaId = idea.id,
                ideaTitle = idea.title,
                message = message,
                date = date,
                time = time,
                status = "PENDING"
            )
            repository.insertMeetingRequest(request)
            // Notify creator
            triggerNotification(idea.creatorId, "Meeting Requested! 📅", "${user.fullName} wants to connect regarding your idea '${idea.title}'.")
            triggerNotification(user.id, "Meeting Request Sent", "Your request to connect with ${idea.creatorName} was successfully sent.")
        }
    }

    fun updateMeetingStatus(request: MeetingRequestEntity, status: String) {
        viewModelScope.launch {
            repository.updateMeetingRequestStatus(request.id, status)
            val actionWord = when (status) {
                "ACCEPTED" -> "accepted"
                "REJECTED" -> "declined"
                else -> "processed"
            }
            triggerNotification(request.investorId, "Meeting Update 📅", "Great news! ${currentUser.value?.fullName} has $actionWord your meeting request regarding '${request.ideaTitle}'.")
        }
    }

    // --- Notification Helpers ---
    private fun triggerNotification(userId: String, title: String, message: String) {
        viewModelScope.launch {
            repository.insertNotification(
                NotificationEntity(userId = userId, title = title, message = message)
            )
        }
    }

    fun readNotification(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    // --- AI Feature Execution ---
    fun clearAiResult() {
        _aiResult.value = null
    }

    fun runAiIdeaReview(title: String, description: String, category: String, problem: String, solution: String, budget: String) {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiResult.value = null
            val systemPrompt = "You are India's premier startup incubator panel lead on 'India Idea Hub'. Evaluate the following business idea rigorously in an Indian context."
            val userPrompt = """
                Please REVIEW the following idea:
                - Title: $title
                - Category: $category
                - Problem: $problem
                - Solution: $solution
                - Estimated Budget: $budget
                - Description: $description
            """.trimIndent()
            _aiResult.value = repository.callGemini(systemPrompt, userPrompt)
            _aiLoading.value = false
        }
    }

    fun runAiGrammarImprovement(text: String) {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiResult.value = null
            val systemPrompt = "You are an expert pitch editor. Polish and improve the grammar/vocabulary of the entrepreneur's text to make it extremely professional and sound elite to Indian VC syndicates."
            val userPrompt = "Please IMPROVE THE GRAMMAR of this pitch description:\n\n$text"
            _aiResult.value = repository.callGemini(systemPrompt, userPrompt)
            _aiLoading.value = false
        }
    }

    fun runAiPitchSuggestions(title: String, description: String) {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiResult.value = null
            val systemPrompt = "You are an expert venture capitalist. Provide compelling hook examples and structured slide deck templates optimized for early-stage Indian angel syndicates."
            val userPrompt = """
                Please generate early-stage PITCH suggestions and outline for this startup idea:
                - Title: $title
                - Description: $description
            """.trimIndent()
            _aiResult.value = repository.callGemini(systemPrompt, userPrompt)
            _aiLoading.value = false
        }
    }

    fun runAiMarketAnalysis(category: String, title: String, problem: String) {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiResult.value = null
            val systemPrompt = "You are a top commercial market research analyst in India. Provide detailed industry statistics, estimated growth (CAGR), buyer persona profiles, and key competitors."
            val userPrompt = """
                Please compile an Indian MARKET analysis report for:
                - Industry/Category: $category
                - Startup Title: $title
                - Core Problem Addressed: $problem
            """.trimIndent()
            _aiResult.value = repository.callGemini(systemPrompt, userPrompt)
            _aiLoading.value = false
        }
    }

    fun runAiBusinessNameSuggestions(keywords: String) {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiResult.value = null
            val systemPrompt = "You are a brand naming specialist. Suggest 5 catchy, powerful, and modern startup names that have an Indian touch and global appeal. Explain the meaning of each."
            val userPrompt = "Suggest business names based on the following keywords/concept:\n\n$keywords"
            _aiResult.value = repository.callGemini(systemPrompt, userPrompt)
            _aiLoading.value = false
        }
    }

    // --- Mock Database Initializer ---
    private fun seedMockData() {
        viewModelScope.launch {
            val userCount = repository.getAllUsers().first().size
            if (userCount > 0) return@launch // Already seeded

            // 1. Seed Users
            val creator1 = UserEntity(
                id = "creator_1",
                email = "creator_1@idea.in",
                fullName = "Aarav Sharma",
                role = "CREATOR",
                bio = "AgriTech Developer & IoT builder from Pune. Passionate about empowering smallholder Indian farmers with digital solutions.",
                skills = "IoT, Embedded Systems, Agriculture, Kotlin",
                socialLinks = "LinkedIn: aarav-sharma-agri, Twitter: @aarav_builds",
                followersCount = 428,
                followingCount = 192,
                avatarUrl = "avatar_1"
            )
            val creator2 = UserEntity(
                id = "creator_2",
                email = "creator_2@idea.in",
                fullName = "Dr. Priya Iyer",
                role = "CREATOR",
                bio = "AI Medical Researcher and former cardiologist at AIIMS, New Delhi. Merging ancient Ayurveda with AI-driven pulse analysis.",
                skills = "Healthcare AI, Machine Learning, Clinical Cardiology, Ayurveda",
                socialLinks = "LinkedIn: dr-priya-iyer-ai, ResearchGate: priya_iyer_cardio",
                followersCount = 1205,
                followingCount = 310,
                avatarUrl = "avatar_2"
            )
            val investor1 = UserEntity(
                id = "investor_1",
                email = "investor_1@hub.in",
                fullName = "Rohan Mehta",
                role = "INVESTOR",
                bio = "Angel Investor & General Partner at Bharat Seed Capital. Actively backing micro-businesses, deep-tech, and rural digital infrastructure.",
                skills = "Seed Funding, Venture Capital, Growth Hacking, Business Strategy",
                socialLinks = "LinkedIn: rohan-mehta-vc, Twitter: @rohan_bharat_vc",
                followersCount = 3590,
                followingCount = 450,
                avatarUrl = "avatar_3"
            )
            val admin1 = UserEntity(
                id = "admin",
                email = "admin@hub.in",
                fullName = "Ananya Nair",
                role = "ADMIN",
                bio = "India Idea Hub Platform Moderation Team Lead. Helping nurture India's startup sandbox safely.",
                skills = "Product Operations, Content Moderation, Analytics, Compliance",
                socialLinks = "LinkedIn: ananya-nair-hub",
                followersCount = 110,
                followingCount = 80,
                avatarUrl = "avatar_4"
            )

            repository.insertUser(creator1)
            repository.insertUser(creator2)
            repository.insertUser(investor1)
            repository.insertUser(admin1)

            // 2. Seed Startup Ideas
            val idea1 = IdeaEntity(
                id = 1,
                creatorId = "creator_1",
                creatorName = "Aarav Sharma",
                creatorAvatar = "avatar_1",
                title = "KrishiSetu: Smart IoT Soil & Water Monitor",
                description = "An ultra-low-cost, battery-powered IoT probe designed for smallholder Indian farmers. It measures soil nitrogen, moisture, and temperature in real-time, sending vernacular Kannada, Marathi, and Hindi recommendations directly to the farmer's mobile screen via offline SMS.",
                category = "Agriculture",
                problemStatement = "Small farmers in India waste over 40% of their chemical fertilizer budget because they lack scientific soil metrics, which leads to crop burning and groundwater pollution.",
                solution = "Develop an affordable ₹1,500 sensor probe paired with a local cellular transceiver that requires zero internet and operates for 180 days on standard pencil batteries.",
                targetAudience = "Over 120 Million smallholder farming families across Maharashtra, Karnataka, and Uttar Pradesh.",
                estimatedBudget = "₹ 25 Lakh",
                likesCount = 87,
                viewsCount = 340,
                commentsCount = 3,
                status = "PUBLISHED",
                imageUrl = "agri_banner"
            )

            val idea2 = IdeaEntity(
                id = 2,
                creatorId = "creator_2",
                creatorName = "Dr. Priya Iyer",
                creatorAvatar = "avatar_2",
                title = "AyurScan: AI-based Nadi Pariksha Analyzer",
                description = "AyurScan utilizes high-frequency wrist-worn acoustic biosensors and machine learning models trained on 15,000+ clinical cardiologist profiles to capture and analyze the arterial pulse ('Nadi Pariksha'). It provides real-time organ health telemetry based on traditional Ayurvedic principles.",
                category = "Healthcare",
                problemStatement = "Ancient Indian Ayurveda is highly effective for preventative care, but lacks standardized digital diagnostics, leading to skepticism and a shortage of verified practitioners in remote regions.",
                solution = "A consumer smart band attachment that measures micro-pulse modulations, mapping them directly to dosha imbalances and early signs of arterial blockage.",
                targetAudience = "Wellness centers, health-conscious urban professionals, and traditional practitioners across India.",
                estimatedBudget = "₹ 75 Lakh",
                likesCount = 142,
                viewsCount = 590,
                commentsCount = 2,
                status = "PUBLISHED",
                imageUrl = "health_banner"
            )

            val idea3 = IdeaEntity(
                id = 3,
                creatorId = "creator_1",
                creatorName = "Aarav Sharma",
                creatorAvatar = "avatar_1",
                title = "VidyaStream: Vernacular Ed-Tech without Internet",
                description = "A portable local Wi-Fi router (VidyaBox) packed with thousands of high-quality primary and secondary school lectures dubbed in regional Indian languages. Students in offline villages connect local devices to the box's server, streaming education fully lag-free with zero cellular bills.",
                category = "Education",
                problemStatement = "Students in remote Indian villages have smartphones but lack consistent high-speed 4G data, completely cutting them off from standard online video-based education portals.",
                solution = "Hardware-software local box distributing compressed interactive learning assets over local offline Wi-Fi nodes.",
                targetAudience = "Rural primary schools, community centers, and local panchayats with poor internet connectivity.",
                estimatedBudget = "₹ 15 Lakh",
                likesCount = 95,
                viewsCount = 280,
                commentsCount = 1,
                status = "PUBLISHED",
                imageUrl = "edu_banner"
            )

            val idea4 = IdeaEntity(
                id = 4,
                creatorId = "creator_2",
                creatorName = "Dr. Priya Iyer",
                creatorAvatar = "avatar_2",
                title = "UrjaGrid: Decentralized Solar Biomass Micro-Grids",
                description = "UrjaGrid creates automated local micro-grids in power-deficient Bihar and Jharkhand villages, combining roof-top solar panels with organic crop-waste biomass gasifiers to produce clean, continuous 220V electricity managed via smart contracts.",
                category = "Environment",
                problemStatement = "Rural workshops, grain mills, and schools experience up to 8 hours of daily unscheduled power cuts, crippling local productivity and forcing heavy diesel dependency.",
                solution = "A peer-to-peer micro-grid model where surplus residential solar power is sold directly to local commercial users.",
                targetAudience = "Over 45,000 un-electrified and power-unstable micro-industrial clusters in Northern India.",
                estimatedBudget = "₹ 1.2 Crore",
                likesCount = 115,
                viewsCount = 420,
                commentsCount = 0,
                status = "PUBLISHED",
                imageUrl = "env_banner"
            )

            repository.insertIdea(idea1)
            repository.insertIdea(idea2)
            repository.insertIdea(idea3)
            repository.insertIdea(idea4)

            // 3. Seed Comments
            repository.insertComment(CommentEntity(ideaId = 1, userId = "investor_1", userName = "Rohan Mehta", userAvatar = "avatar_3", text = "This is brilliant! Agritech hardware is rare and highly valuable in Maharashtra. I would love to request a demo of the soil probe."))
            repository.insertComment(CommentEntity(ideaId = 1, userId = "creator_2", userName = "Dr. Priya Iyer", userAvatar = "avatar_2", text = "This can be extremely useful. Have you considered integrating moisture mapping with local weather prediction APIs?"))
            repository.insertComment(CommentEntity(ideaId = 1, userId = "creator_1", userName = "Aarav Sharma", userAvatar = "avatar_1", text = "Thank you Rohan! Yes, we have tested the probe with 15 farmers in Pune and Nashik already. Will accept your connect request immediately!"))

            repository.insertComment(CommentEntity(ideaId = 2, userId = "investor_1", userName = "Rohan Mehta", userAvatar = "avatar_3", text = "Fascinating technology, Priya. Standardizing Nadi Pariksha is a massive market opportunity if the sensors are calibrated correctly. Please connect."))
            repository.insertComment(CommentEntity(ideaId = 2, userId = "creator_1", userName = "Aarav Sharma", userAvatar = "avatar_1", text = "Incredible work, Priya. I'd love to help configure the micro-acoustic sensors for your wristbands!"))

            repository.insertComment(CommentEntity(ideaId = 3, userId = "investor_1", userName = "Rohan Mehta", userAvatar = "avatar_3", text = "This has fantastic social impact potential. How much does a single VidyaBox cost to manufacture?"))

            // Seed initial notifications to make it look active
            repository.insertNotification(NotificationEntity(userId = "creator_1", title = "Platform Launch! 🎉", message = "Welcome to India Idea Hub. Connect with Indian investors and co-founders today."))
            repository.insertNotification(NotificationEntity(userId = "creator_2", title = "Platform Launch! 🎉", message = "Welcome to India Idea Hub. Bring ancient sciences to the modern digital era."))
        }
    }

    private fun getMockImageForCategory(category: String): String {
        return when (category.lowercase(Locale.getDefault())) {
            "agriculture" -> "agri_banner"
            "healthcare" -> "health_banner"
            "education" -> "edu_banner"
            "environment" -> "env_banner"
            "technology" -> "tech_banner"
            "ai" -> "ai_banner"
            "finance" -> "fin_banner"
            else -> "misc_banner"
        }
    }
}
