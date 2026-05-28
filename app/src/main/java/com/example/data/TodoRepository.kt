package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart

class TodoRepository(private val db: AppDatabase) {
    private val taskDao = db.taskDao()
    private val subtaskDao = db.subtaskDao()
    private val projectDao = db.projectDao()
    private val tagDao = db.tagDao()
    private val commentDao = db.commentDao()
    private val activityLogDao = db.activityLogDao()
    private val appSettingsDao = db.appSettingsDao()

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects().onStart {
        // Prepopulate if empty
        ensureDefaultProjectsAndTags()
    }
    val allTags: Flow<List<Tag>> = tagDao.getAllTags()
    val allLogs: Flow<List<ActivityLog>> = activityLogDao.getActivityLogs()
    val settingsFlow: Flow<AppSettings?> = appSettingsDao.getSettingsFlow().onStart {
        ensureDefaultSettings()
    }

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task): Long {
        val id = taskDao.insertTask(task)
        insertLog("CREATED", "Created task: \"${task.title}\"", id)
        return id
    }

    suspend fun updateTask(task: Task) {
        val oldTask = taskDao.getTaskById(task.id)
        taskDao.updateTask(task)
        if (oldTask != null) {
            if (oldTask.isCompleted != task.isCompleted) {
                val action = if (task.isCompleted) "COMPLETED" else "REOPENED"
                insertLog(action, "$action task: \"${task.title}\"", task.id)
            } else {
                insertLog("UPDATED", "Updated task: \"${task.title}\"", task.id)
            }
        }
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        insertLog("DELETED", "Deleted task: \"${task.title}\"", task.id)
    }

    fun searchTasks(query: String): Flow<List<Task>> = taskDao.searchTasks(query)

    // Subtasks
    fun getSubtasksForTask(taskId: Long): Flow<List<Subtask>> = subtaskDao.getSubtasksForTask(taskId)

    suspend fun insertSubtask(subtask: Subtask): Long {
        val id = subtaskDao.insertSubtask(subtask)
        insertLog("SUBTASK_ADDED", "Added subtask info to task ID: ${subtask.taskId}", subtask.taskId)
        return id
    }

    suspend fun updateSubtask(subtask: Subtask) {
        subtaskDao.updateSubtask(subtask)
        insertLog("SUBTASK_UPDATED", "Updated subtask status: \"${subtask.title}\"", subtask.taskId)
    }

    suspend fun deleteSubtask(subtask: Subtask) {
        subtaskDao.deleteSubtask(subtask)
        insertLog("SUBTASK_DELETED", "Deleted subtask: \"${subtask.title}\"", subtask.taskId)
    }

    // Projects
    suspend fun insertProject(project: Project): Long = projectDao.insertProject(project)
    suspend fun deleteProject(project: Project) = projectDao.deleteProject(project)

    // Tags
    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)
    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)

    // Comments
    fun getCommentsForTask(taskId: Long): Flow<List<Comment>> = commentDao.getCommentsForTask(taskId)
    suspend fun insertComment(comment: Comment): Long = commentDao.insertComment(comment)

    // Settings
    suspend fun updateSettings(settings: AppSettings) {
        appSettingsDao.insertOrUpdateSettings(settings)
    }

    // Activity Logs
    suspend fun insertLog(action: String, details: String, taskId: Long? = null) {
        activityLogDao.insertLog(ActivityLog(action = action, details = details, taskId = taskId))
    }

    private suspend fun ensureDefaultProjectsAndTags() {
        val currentProjects = db.projectDao().getAllProjects().firstOrNull() ?: emptyList()
        if (currentProjects.isEmpty()) {
            projectDao.insertProject(Project(name = "Personal", color = 0xFF4CAF50.toInt()))
            projectDao.insertProject(Project(name = "Work", color = 0xFF2196F3.toInt()))
            projectDao.insertProject(Project(name = "Groceries", color = 0xFFFF9800.toInt()))
        }

        val currentTags = db.tagDao().getAllTags().firstOrNull() ?: emptyList()
        if (currentTags.isEmpty()) {
            tagDao.insertTag(Tag(name = "Urgent", color = 0xFFF44336.toInt()))
            tagDao.insertTag(Tag(name = "High Priority", color = 0xFFFF5722.toInt()))
            tagDao.insertTag(Tag(name = "Later", color = 0xFF9C27B0.toInt()))
        }
    }

    private suspend fun ensureDefaultSettings() {
        val current = appSettingsDao.getSettingsDirect()
        if (current == null) {
            appSettingsDao.insertOrUpdateSettings(AppSettings())
        }
    }
}
