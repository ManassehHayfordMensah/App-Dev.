package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AuthState {
    object Unauthenticated : AuthState
    data class Authenticated(val user: UserEntity) : AuthState
    data class Loading(val message: String) : AuthState
    data class Error(val message: String) : AuthState
}

data class AiStudioState(
    val chatHistory: List<ChatMessage> = emptyList(),
    val chatLoading: Boolean = false,
    
    // Skill verification variables
    val verificationStatus: String = "NOT_STARTED", // "NOT_STARTED", "PENDING", "PASSED", "FAILED"
    val verificationReport: String = "",
    
    // Creative images/videos generators
    val generatedImageBase64: String? = null,
    val generatedImageBitmap: Bitmap? = null,
    val creativeLoading: Boolean = false,
    val generatedVideoMsg: String? = null
)

class SkillJetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = SkillJetRepository(db)
    private val sharedPrefs = application.getSharedPreferences("SkillJetPrefs", android.content.Context.MODE_PRIVATE)

    // --- Authentication Flow ---
    private val _showOnboarding = MutableStateFlow(!sharedPrefs.getBoolean("onboarding_completed", false))
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    fun completeOnboarding() {
        _showOnboarding.value = false
        sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
    }

    fun startOnboarding() {
        _showOnboarding.value = true
        sharedPrefs.edit().putBoolean("onboarding_completed", false).apply()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _authState.map {
        if (it is AuthState.Authenticated) it.user else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Database Flows ---
    val feedItems: StateFlow<List<FeedEntity>> = repository.allFeedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openJobs: StateFlow<List<JobEntity>> = repository.openJobs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = repository.unreadNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- AI Studio State ---
    private val _aiState = MutableStateFlow(AiStudioState())
    val aiState: StateFlow<AiStudioState> = _aiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed base Ghana jobs, feed posts, and notifications if DB is fresh
            repository.prepopulateIfEmpty()
            
            // Register demo user in database so it exists and is ready for login
            val demoUser = UserEntity(
                id = "demo_worker",
                name = "Kwame Auto Works",
                email = "kwame@skilljet.gh",
                phone = "0244123456",
                userType = "WORKER",
                skills = "Car Mechanic, Engine Overhaul, Brake Specialist, Toyota",
                rating = 4.8f,
                reviewsCount = 124,
                location = "Adenta, Accra",
                isAiVerified = true,
                avatarUrl = "https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=400&q=80"
            )
            repository.saveUser(demoUser)
            
            // Check if there is a previously saved logged-in user session
            val savedUserId = sharedPrefs.getString("logged_in_user_id", null)
            if (savedUserId != null) {
                val savedUser = repository.getUser(savedUserId)
                if (savedUser != null) {
                    _authState.value = AuthState.Authenticated(savedUser)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    // --- Authentication Logic ---

    fun login(emailOrPhone: String, isWorker: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading("Signing in...")
            try {
                // Fetch or create user in Room db
                val existing = repository.getUserByEmailOrPhone(emailOrPhone)
                val user = existing ?: run {
                    val isEmail = emailOrPhone.contains("@")
                    val resolvedEmail = if (isEmail) emailOrPhone else "$emailOrPhone@skilljet.gh"
                    val resolvedPhone = if (isEmail) "020 918 2384" else emailOrPhone
                    
                    val baseUsername = if (isEmail) emailOrPhone.substringBefore("@") else "User_$emailOrPhone"
                    val capitalizedUsername = baseUsername.replaceFirstChar { it.uppercase() }
                    val resolvedName = if (isWorker) "$capitalizedUsername Repairs" else capitalizedUsername
                    val defaultSkills = if (isWorker) "Plumber, Pipe Repair, General Maintenance" else ""
                    
                    UserEntity(
                        id = emailOrPhone,
                        name = resolvedName,
                        email = resolvedEmail,
                        phone = resolvedPhone,
                        userType = if (isWorker) "WORKER" else "CLIENT",
                        skills = defaultSkills,
                        rating = 4.7f,
                        reviewsCount = 12,
                        location = "Madina, Accra",
                        isAiVerified = isWorker, // Workers get verified status
                        avatarUrl = "https://ui-avatars.com/api/?name=${resolvedName.replace(" ", "+")}&background=006B3F&color=fff"
                    )
                }
                repository.saveUser(user)
                sharedPrefs.edit()
                    .putString("logged_in_user_id", user.id)
                    .putBoolean("onboarding_completed", true)
                    .apply()
                _showOnboarding.value = false
                _authState.value = AuthState.Authenticated(user)
                onSuccess()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signUp(name: String, email: String, phone: String, isWorker: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading("Creating your SkillJet account...")
            try {
                val defaultSkills = if (isWorker) "Custom Tailoring, Kente Fitting" else ""
                val newUser = UserEntity(
                    id = email,
                    name = name,
                    email = email,
                    phone = phone,
                    userType = if (isWorker) "WORKER" else "CLIENT",
                    skills = defaultSkills,
                    rating = 5.0f,
                    reviewsCount = 0,
                    location = "Adenta, Accra",
                    isAiVerified = false,
                    avatarUrl = "https://ui-avatars.com/api/?name=${name.replace(" ", "+")}&background=e30613&color=fff"
                )
                repository.saveUser(newUser)
                sharedPrefs.edit()
                    .putString("logged_in_user_id", newUser.id)
                    .putBoolean("onboarding_completed", true)
                    .apply()
                _showOnboarding.value = false
                _authState.value = AuthState.Authenticated(newUser)
                onSuccess()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Sign up failed")
            }
        }
    }

    fun logout() {
        sharedPrefs.edit().remove("logged_in_user_id").apply()
        _authState.value = AuthState.Unauthenticated
        // Reset chatbot
        _aiState.value = AiStudioState()
    }

    // --- Job Actions ---

    fun postJob(title: String, skill: String, description: String, landmark: String, urgency: String, budget: Int) {
        viewModelScope.launch {
            val client = currentUser.value?.name ?: "Esi Kof"
            val newJob = JobEntity(
                title = title,
                skill = skill,
                description = description,
                landmark = landmark,
                urgency = urgency,
                budget = budget,
                postedTime = "Just now",
                distance = "Nearby",
                clientName = client
            )
            repository.createJob(newJob)
        }
    }

    fun acceptJob(jobId: Int) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            if (user.userType == "WORKER") {
                repository.acceptJob(jobId, user.id, user.name)
            }
        }
    }

    fun markNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun addShowcasePost(text: String, imageUri: String? = null, tags: String = "") {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.insertFeedPost(
                user = user.name,
                text = text,
                type = "showcase",
                image = imageUri ?: "https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=400&q=80",
                tags = tags.uppercase()
            )
        }
    }

    // --- Gemini Interactive Features (AI Hub) ---

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val currentHistory = _aiState.value.chatHistory
        val userMsg = ChatMessage(sender = "USER", text = text)
        
        _aiState.update { 
            it.copy(
                chatHistory = currentHistory + userMsg,
                chatLoading = true
            )
        }

        viewModelScope.launch {
            // Set instruction role as recommended
            val sysInstruction = """
                You are the SkillJet AI Trade Mentor, Ghana's premier advisor for craftsmen, technicians, and local clients.
                Your tone is highly helpful, encouraging, and rich in local Ghanaian trade references (such as charging fair rates in Ghana Cedis ₵, sourcing materials from Makola or Circle, dealing with common Accra landmarks, and offering solid technical advice for Toyota/Corolla mechanical parts, plumbers fixing PolyTank connectors, or electricians dealing with GRIDCo voltage flickers).
                Keep answers actionable and structured. Be encouraging.
            """.trimIndent()

            // Call text generation. Use gemini-3.1-pro-preview for complex tasks (or fallback is handled elegantly)
            val aiResponse = GeminiClient.generateText(
                model = "gemini-3.5-flash",
                prompt = text,
                systemInstruction = sysInstruction,
                history = _aiState.value.chatHistory
            )

            val aiMsg = ChatMessage(sender = "AI", text = aiResponse)
            _aiState.update {
                it.copy(
                    chatHistory = it.chatHistory + aiMsg,
                    chatLoading = false
                )
            }
        }
    }

    fun clearChat() {
        _aiState.update { it.copy(chatHistory = emptyList()) }
    }

    /**
     * Skill/Craft Verification analysis.
     * Uses gemini-3.1-pro-preview with image understanding to verify worker's work or landmark photo.
     */
    fun verifyWorkerCraft(landmarkName: String, problemDetail: String, bitmap: Bitmap?) {
        _aiState.update { it.copy(verificationStatus = "PENDING", verificationReport = "Analyzing landmark/craft with Gemini AI...") }
        
        viewModelScope.launch {
            val prompt = """
                Analyze this landmark/craft submission on the Ghana SkillJet Platform:
                Landmark description: "$landmarkName"
                Details provided: "$problemDetail"
                
                Please review:
                1. Is this a valid, easily identifiable landmark in Ghana (e.g. Adenta, Madina, Circle, East Legon)?
                2. Are the problem descriptions technical and clear?
                3. Offer 2 solid instructions/safety tips for the worker trying to reach this landmark or solve this repair.
                
                Start your response clearly stating whether the landmark and task details are APPROVED or need more details.
            """.trimIndent()

            val result = if (bitmap != null) {
                GeminiClient.analyzeImage(bitmap, prompt, model = "gemini-3.1-pro-preview")
            } else {
                // If no image, analyze text with thinking mode (ThinkingLevel.HIGH)
                GeminiClient.generateText(
                    model = "gemini-3.1-pro-preview",
                    prompt = "Analyze this text-only craft request: $landmarkName. $problemDetail. Provide helpful technical directions.",
                    useThinking = true
                )
            }

            val approved = result.uppercase().contains("APPROVED") || !result.contains("failed")
            
            _aiState.update {
                it.copy(
                    verificationStatus = if (approved) "PASSED" else "FAILED",
                    verificationReport = result
                )
            }

            // If approved and the user is a worker, award them the verified badge in Room!
            val user = currentUser.value
            if (user != null && user.userType == "WORKER" && approved) {
                repository.verifyWorkerSkills(user.id, true)
                _authState.value = AuthState.Authenticated(user.copy(isAiVerified = true))
            }
        }
    }

    /**
     * Promos Studio: Generate gorgeous marketing art for workers' showcases.
     * Supports custom aspect ratios, size (1K, 2K, 4K), and studio quality model gemini-3-pro-image-preview
     */
    fun generateStudioPromo(prompt: String, aspectRatio: String, size: String, useProModel: Boolean) {
        _aiState.update { it.copy(creativeLoading = true, generatedImageBase64 = null, generatedImageBitmap = null) }
        
        viewModelScope.launch {
            val model = if (useProModel) "gemini-3-pro-image-preview" else "gemini-3.1-flash-image-preview"
            
            val fullPrompt = "$prompt, professional high-quality marketing banner style, vibrant colors, rich details"
            val base64Str = GeminiClient.generateImage(
                prompt = fullPrompt,
                aspectRatio = aspectRatio,
                size = size,
                model = model
            )

            if (base64Str.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    _aiState.update {
                        it.copy(
                            generatedImageBase64 = base64Str,
                            generatedImageBitmap = bitmap,
                            creativeLoading = false
                        )
                    }
                } catch (e: Exception) {
                    _aiState.update {
                        it.copy(
                            creativeLoading = false,
                            generatedVideoMsg = "Image generated successfully, but decoding failed."
                        )
                    }
                }
            } else {
                // Mock generator fallback so the experience is perfect if API is offline
                _aiState.update {
                    it.copy(
                        creativeLoading = false,
                        generatedVideoMsg = "Generated mockup art: '$prompt' ($aspectRatio, size $size). [API Key required for real images]"
                    )
                }
            }
        }
    }

    /**
     * Promos Studio Video: Generate marketing animations with Veo veo-3.1-fast-generate-preview
     */
    fun generateStudioPromoVideo(prompt: String, aspectRatio: String) {
        _aiState.update { it.copy(creativeLoading = true, generatedVideoMsg = null) }
        
        viewModelScope.launch {
            val result = GeminiClient.generateVideo(prompt, aspectRatio)
            _aiState.update {
                it.copy(
                    creativeLoading = false,
                    generatedVideoMsg = result
                )
            }
        }
    }
}
