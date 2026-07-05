package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val userType: String, // "CLIENT" or "WORKER"
    val skills: String = "", // comma-separated
    val rating: Float = 5.0f,
    val reviewsCount: Int = 0,
    val location: String = "Accra, Ghana",
    val isAiVerified: Boolean = false,
    val avatarUrl: String = ""
)

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val skill: String, // "mechanic", "plumber", "electrician", "cvwriter", "tech", "tailor", "builder"
    val description: String,
    val landmark: String,
    val landmarkPhotoUrl: String? = null,
    val urgency: String, // "now", "today", "scheduled"
    val budget: Int,
    val postedTime: String,
    val distance: String,
    val status: String = "OPEN", // "OPEN", "ACCEPTED", "COMPLETED"
    val clientName: String,
    val workerId: String? = null
)

@Entity(tableName = "feed_items")
data class FeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "job", "showcase", "review"
    val user: String,
    val rating: Float = 0.0f,
    val time: String,
    val tags: String = "", // comma-separated
    val text: String,
    val image: String? = null,
    val target: String? = null
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "job_alert", "message", "review"
    val title: String,
    val message: String,
    val time: String,
    val isRead: Boolean = false
)

// --- DAOs (Data Access Objects) ---

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :emailOrPhone OR phone = :emailOrPhone LIMIT 1")
    suspend fun getUserByEmailOrPhone(emailOrPhone: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET isAiVerified = :isVerified WHERE id = :userId")
    suspend fun updateVerificationStatus(userId: String, isVerified: Boolean)
}

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY id DESC")
    fun getAllJobsFlow(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE status = 'OPEN' ORDER BY id DESC")
    fun getOpenJobsFlow(): Flow<List<JobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobEntity)

    @Query("UPDATE jobs SET status = :status, workerId = :workerId WHERE id = :jobId")
    suspend fun updateJobStatus(jobId: Int, status: String, workerId: String?)

    @Query("SELECT * FROM jobs WHERE id = :jobId LIMIT 1")
    suspend fun getJobById(jobId: Int): JobEntity?
}

@Dao
interface FeedDao {
    @Query("SELECT * FROM feed_items ORDER BY id DESC")
    fun getAllFeedFlow(): Flow<List<FeedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedItem(feedItem: FeedEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY id DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadNotificationsCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
}

// --- AppDatabase ---

@Database(
    entities = [UserEntity::class, JobEntity::class, FeedEntity::class, NotificationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun jobDao(): JobDao
    abstract fun feedDao(): FeedDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "skilljet_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
