package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SkillJetRepository(private val db: AppDatabase) {

    val allJobs: Flow<List<JobEntity>> = db.jobDao().getAllJobsFlow()
    val openJobs: Flow<List<JobEntity>> = db.jobDao().getOpenJobsFlow()
    val allFeedItems: Flow<List<FeedEntity>> = db.feedDao().getAllFeedFlow()
    val allNotifications: Flow<List<NotificationEntity>> = db.notificationDao().getAllNotificationsFlow()
    val unreadNotificationsCount: Flow<Int> = db.notificationDao().getUnreadNotificationsCountFlow()

    suspend fun getUser(id: String): UserEntity? {
        return db.userDao().getUserById(id)
    }

    suspend fun getUserByEmailOrPhone(emailOrPhone: String): UserEntity? {
        return db.userDao().getUserByEmailOrPhone(emailOrPhone)
    }

    suspend fun saveUser(user: UserEntity) {
        db.userDao().insertUser(user)
    }

    suspend fun createJob(job: JobEntity) {
        db.jobDao().insertJob(job)
        // Auto-create a feed item for this job post!
        val feedItem = FeedEntity(
            type = "job",
            user = job.clientName,
            time = "Just now",
            tags = "#${job.skill.uppercase()}, #${job.landmark.split(",").lastOrNull()?.trim() ?: "Accra"}",
            text = "${job.description}. Stranded near ${job.landmark}. Budget: ₵${job.budget}"
        )
        db.feedDao().insertFeedItem(feedItem)
        
        // Push a notification about this job matching skills
        val notif = NotificationEntity(
            type = "job_alert",
            title = "New ${job.skill.replaceFirstChar { it.uppercase() }} Job Post",
            message = "A new job request '${job.title}' was posted. Landmark: ${job.landmark}.",
            time = "Just now"
        )
        db.notificationDao().insertNotification(notif)
    }

    suspend fun acceptJob(jobId: Int, workerId: String, workerName: String) {
        db.jobDao().updateJobStatus(jobId, "ACCEPTED", workerId)
        
        // Notify the client/worker
        val job = db.jobDao().getJobById(jobId)
        if (job != null) {
            val notif = NotificationEntity(
                type = "message",
                title = "Job Accepted by $workerName",
                message = "The skilled worker $workerName has accepted your job: '${job.title}'. They will call you shortly.",
                time = "Just now"
            )
            db.notificationDao().insertNotification(notif)
        }
    }

    suspend fun markNotificationAsRead(id: Int) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        db.notificationDao().markAllAsRead()
    }

    suspend fun insertFeedPost(user: String, text: String, type: String = "showcase", image: String? = null, rating: Float = 0f, tags: String = "") {
        db.feedDao().insertFeedItem(
            FeedEntity(
                type = type,
                user = user,
                rating = rating,
                time = "Just now",
                text = text,
                image = image,
                tags = tags
            )
        )
    }

    suspend fun verifyWorkerSkills(userId: String, isVerified: Boolean) {
        db.userDao().updateVerificationStatus(userId, isVerified)
    }

    /**
     * Prepopulates the database with default Ghana-specific skilled labor listings,
     * user feeds, reviews, and notifications if empty.
     */
    suspend fun prepopulateIfEmpty() = withContext(Dispatchers.IO) {
        val feeds = db.feedDao().getAllFeedFlow().first()
        if (feeds.isEmpty()) {
            // 1. Initial Feed items
            db.feedDao().insertFeedItem(
                FeedEntity(
                    type = "showcase",
                    user = "Kwame Auto Works",
                    rating = 4.8f,
                    time = "5 hrs ago",
                    text = "Just finished a complete engine overhaul on this Toyota Camry in Dansoman, Accra. All fluids changed, filters replaced, now running smooth! 🚗💨 #QualityWork #AccraMechanic",
                    image = "https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=1000&q=80",
                    tags = "#MECHANIC, #DANSOMAN, #TOYOTA_SPECIALIST"
                )
            )
            db.feedDao().insertFeedItem(
                FeedEntity(
                    type = "job",
                    user = "Kofi Manu",
                    time = "2 hrs ago",
                    text = "My Toyota Corolla won't start this morning in Adenta. Making clicking sounds near Melcom. Need a mechanics urgently! 🚗",
                    tags = "#MECHANIC, #ADENTA, #URGENT"
                )
            )
            db.feedDao().insertFeedItem(
                FeedEntity(
                    type = "review",
                    user = "Ama Serwaa",
                    rating = 5.0f,
                    time = "1 day ago",
                    text = "Kwame is simply the best mechanic in Accra! He came quickly and resolved my electrical issues when I was stranded at Circle. Transparent pricing!",
                    target = "Kwame Auto Works"
                )
            )
            db.feedDao().insertFeedItem(
                FeedEntity(
                    type = "showcase",
                    user = "Aisha Fashion House",
                    rating = 4.9f,
                    time = "2 days ago",
                    text = "Hand-sewn custom Kente traditional wear prepared for an engagement ceremony this weekend. Loving the vibrant patterns and modern slim cut! 🧵✨ #Tailoring #GhanaFashion",
                    image = "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?auto=format&fit=crop&w=1200&q=80",
                    tags = "#TAILORING, #KENTE, #ENGAGEMENT"
                )
            )

            // 2. Initial Jobs list
            db.jobDao().insertJob(
                JobEntity(
                    title = "Toyota Corolla won't start",
                    skill = "mechanic",
                    description = "My engine is clicking and won't turn over. Stranded behind Adenta Melcom.",
                    landmark = "Behind Melcom Shop, Adenta",
                    urgency = "now",
                    budget = 180,
                    postedTime = "2 mins ago",
                    distance = "1.2 km away",
                    clientName = "Kofi Manu"
                )
            )
            db.jobDao().insertJob(
                JobEntity(
                    title = "Kitchen pipe leak repair",
                    skill = "plumber",
                    description = "The main pipe under the sink has burst. Gushing water needs urgent sealing.",
                    landmark = "Opposite ATTC Station, Madina",
                    urgency = "now",
                    budget = 150,
                    postedTime = "15 mins ago",
                    distance = "2.5 km away",
                    clientName = "Esi Mensah"
                )
            )
            db.jobDao().insertJob(
                JobEntity(
                    title = "Flickering living room wiring",
                    skill = "electrician",
                    description = "Lights are flickering and tripping the breaker. Diagnose the circuit issue.",
                    landmark = "East Legon, near Police Station",
                    urgency = "today",
                    budget = 200,
                    postedTime = "3 hours ago",
                    distance = "4.2 km away",
                    clientName = "Yaw Badu"
                )
            )
            db.jobDao().insertJob(
                JobEntity(
                    title = "Traditional Smock Sewing",
                    skill = "tailor",
                    description = "Need a custom-tailored Northern Smock (Fugu) prepared with thick fabric.",
                    landmark = "Makola Market, Accra Central",
                    urgency = "scheduled",
                    budget = 350,
                    postedTime = "1 day ago",
                    distance = "6.0 km away",
                    clientName = "Abena Osei"
                )
            )

            // 3. Initial Notifications list
            db.notificationDao().insertNotification(
                NotificationEntity(
                    type = "job_alert",
                    title = "New Mechanic Job Posted",
                    message = "A Toyota Corolla won't start near Adenta Melcom. Ready for immediate contact.",
                    time = "5 mins ago"
                )
            )
            db.notificationDao().insertNotification(
                NotificationEntity(
                    type = "message",
                    title = "SkillJet Verification Badge",
                    message = "Welcome to SkillJet! Skilled workers can verify their trades via the AI Hub using demo videos to get the verified badge.",
                    time = "20 mins ago"
                )
            )
        }
    }
}
