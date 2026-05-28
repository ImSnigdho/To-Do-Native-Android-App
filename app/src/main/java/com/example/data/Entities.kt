package com.example.data

import androidx.room.*
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int // Hex color Int (e.g., 0xFF4CAF50.toInt())
)

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int // Hex color Int
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDate: Long? = null, // Epoch timestamp (ms)
    val dueTime: String? = null, // "HH:mm" formatted
    val priority: Int = 4, // 1 = P1 (Red), 2 = P2 (Orange), 3 = P3 (Yellow), 4 = P4 (Default)
    val projectId: Long? = null, // Null indicates "Inbox"
    val tagId: Long? = null, // Associated tag
    val recurrence: String = "NONE", // "NONE", "DAILY", "WEEKLY", "MONTHLY", "WEEKDAYS"
    val completedTimestamp: Long? = null,
    val completedDate: Long? = null, // Timestamp when completed
    val locationName: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val locationTriggerOnEnter: Boolean = true
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class Subtask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val userName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val action: String, // "CREATED", "COMPLETED", "DELETED", etc.
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1, // Singleton row
    val isDarkMode: Boolean? = null, // null = system default, true = Dark, false = Light
    val primaryColorHex: String = "#6750A4", // Default dynamic purple
    val startOfWeekSunday: Boolean = true,
    val timeFormat24Hour: Boolean = false,
    val defaultStartupScreen: String = "Today", // "Today", "Inbox", "Calendar"
    val notificationsEnabled: Boolean = true,
    val dailyDigestEnabled: Boolean = true,
    val dailyDigestTime: String = "08:00"
)
