package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

private data class FilterState(
    val allTasks: List<Task>,
    val search: String,
    val projectId: Long?,
    val tagId: Long?,
    val view: String
)

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = TodoRepository(db)

    // UI state flows from Repo
    val projects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> = repository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<ActivityLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettings> = repository.settingsFlow
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // UI Interactive filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<Long?>(null) // null = all or inbox depending on view
    val selectedProjectId = _selectedProjectId.asStateFlow()

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId = _selectedTagId.asStateFlow()

    private val _smartView = MutableStateFlow("Inbox") // "Inbox", "Today", "Upcoming", "Completed", "All"
    val smartView = _smartView.asStateFlow()

    private val _sortOrder = MutableStateFlow("Date") // "Date", "Priority", "Alpha", "Custom"
    val sortOrder = _sortOrder.asStateFlow()

    // Combined filtered task stream - written in a 2-stage type-safe combine flow
    private val filterFlow = combine(
        repository.allTasks,
        _searchQuery,
        _selectedProjectId,
        _selectedTagId,
        _smartView
    ) { allTasks, search, projectId, tagId, view ->
        FilterState(allTasks, search, projectId, tagId, view)
    }

    val tasks: StateFlow<List<Task>> = combine(
        filterFlow,
        _sortOrder
    ) { filter, sort ->
        var filtered = filter.allTasks

        // 1. Search Query filter
        if (filter.search.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(filter.search, ignoreCase = true) ||
                        it.description.contains(filter.search, ignoreCase = true)
            }
        }

        // 2. Project/List filter
        if (filter.projectId != null) {
            filtered = filtered.filter { it.projectId == filter.projectId }
        }

        // 3. Tag Filter
        if (filter.tagId != null) {
            filtered = filtered.filter { it.tagId == filter.tagId }
        }

        // 4. Smart View routing
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStart = calendar.timeInMillis

        filtered = when (filter.view) {
            "Inbox" -> {
                // Default landing area for uncategorized or non-completed tasks, plus completed ones completed today
                filtered.filter { 
                    it.projectId == null && (!it.isCompleted || (it.isCompleted && it.completedDate != null && it.completedDate >= todayStart))
                }
            }
            "Today" -> {
                // Active tasks due today, plus completed tasks completed today
                filtered.filter {
                    (it.dueDate != null && it.dueDate >= todayStart && it.dueDate < tomorrowStart) ||
                    (it.isCompleted && it.completedDate != null && it.completedDate >= todayStart)
                }
            }
            "Upcoming" -> {
                // Rolling future rolling 7 or 30 days (excluding completed)
                filtered.filter {
                    it.dueDate != null && it.dueDate >= todayStart && !it.isCompleted
                }
            }
            "Completed" -> {
                filtered.filter { it.isCompleted }
            }
            else -> {
                // "All" view
                filtered
            }
        }

        // 5. Sorting
        when (sort) {
            "Priority" -> {
                filtered.sortedWith(compareBy<Task> { it.priority }.thenBy { it.dueDate ?: Long.MAX_VALUE })
            }
            "Alpha" -> {
                filtered.sortedBy { it.title.lowercase() }
            }
            "Custom" -> {
                // Custom ordering, default fallback for drag-drop simulation
                filtered.sortedByDescending { it.id }
            }
            else -> {
                // "Date" sort (primary)
                filtered.sortedBy { it.dueDate ?: Long.MAX_VALUE }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice dictation NLP preview
    private val _voiceDraft = MutableStateFlow<String?>(null)
    val voiceDraft = _voiceDraft.asStateFlow()

    // Interactive actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectProject(projectId: Long?) {
        _selectedProjectId.value = projectId
        // Override smart view when looking at a specific project
        if (projectId != null) {
            _smartView.value = "All"
        }
    }

    fun selectTag(tagId: Long?) {
        _selectedTagId.value = tagId
    }

    fun setSmartView(view: String) {
        _smartView.value = view
        // Unset selected projects so smart filters work globally
        _selectedProjectId.value = null
        _selectedTagId.value = null
    }

    fun setSortOrder(order: String) {
        _sortOrder.value = order
    }

    // --- Database Operations ---

    suspend fun getTaskById(id: Long): Task? = repository.getTaskById(id)

    suspend fun addTaskAndGetId(
        title: String,
        description: String = "",
        dueDate: Long? = null,
        dueTime: String? = null,
        priority: Int = 4,
        projectId: Long? = null,
        tagId: Long? = null,
        recurrence: String = "NONE",
        locationName: String? = null,
        locationLatitude: Double? = null,
        locationLongitude: Double? = null,
        locationTriggerOnEnter: Boolean = true
    ): Long {
        val task = Task(
            title = title,
            description = description,
            dueDate = dueDate,
            dueTime = dueTime,
            priority = priority,
            projectId = projectId ?: _selectedProjectId.value,
            tagId = tagId,
            recurrence = recurrence,
            locationName = locationName,
            locationLatitude = locationLatitude,
            locationLongitude = locationLongitude,
            locationTriggerOnEnter = locationTriggerOnEnter
        )
        return repository.insertTask(task)
    }

    suspend fun addTaskWithNlpAndGetId(text: String, defaultProjectId: Long? = null): Long {
        val parsed = NlpParser.parse(text)
        return addTaskAndGetId(
            title = parsed.title,
            dueDate = parsed.dueDate,
            dueTime = parsed.dueTime,
            priority = parsed.priority,
            projectId = defaultProjectId ?: _selectedProjectId.value
        )
    }

    fun addTask(
        title: String,
        description: String = "",
        dueDate: Long? = null,
        dueTime: String? = null,
        priority: Int = 4,
        projectId: Long? = null,
        tagId: Long? = null,
        recurrence: String = "NONE",
        locationName: String? = null,
        locationLatitude: Double? = null,
        locationLongitude: Double? = null,
        locationTriggerOnEnter: Boolean = true
    ) {
        viewModelScope.launch {
            addTaskAndGetId(
                title = title,
                description = description,
                dueDate = dueDate,
                dueTime = dueTime,
                priority = priority,
                projectId = projectId,
                tagId = tagId,
                recurrence = recurrence,
                locationName = locationName,
                locationLatitude = locationLatitude,
                locationLongitude = locationLongitude,
                locationTriggerOnEnter = locationTriggerOnEnter
            )
        }
    }

    /**
     * Parses free-form text using Gemini NLP first (if online), and falls back to a precise regex rules parser.
     */
    fun addTaskWithNlp(text: String, defaultProjectId: Long? = null) {
        viewModelScope.launch {
            addTaskWithNlpAndGetId(text, defaultProjectId)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = !task.isCompleted,
                completedTimestamp = if (!task.isCompleted) System.currentTimeMillis() else null,
                completedDate = if (!task.isCompleted) System.currentTimeMillis() else null
            )
            repository.updateTask(updated)
        }
    }

    fun updateTaskDetails(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Subtasks CRUD
    fun getSubtasks(taskId: Long): Flow<List<Subtask>> = repository.getSubtasksForTask(taskId)

    fun addSubtask(taskId: Long, title: String) {
        viewModelScope.launch {
            repository.insertSubtask(Subtask(taskId = taskId, title = title))
        }
    }

    fun toggleSubtaskCompletion(subtask: Subtask) {
        viewModelScope.launch {
            repository.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted))
        }
    }

    fun deleteSubtask(subtask: Subtask) {
        viewModelScope.launch {
            repository.deleteSubtask(subtask)
        }
    }

    // Project/Tag CRUD
    fun addProject(name: String, color: Int) {
        viewModelScope.launch {
            repository.insertProject(Project(name = name, color = color))
        }
    }

    fun addTag(name: String, color: Int) {
        viewModelScope.launch {
            repository.insertTag(Tag(name = name, color = color))
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_selectedProjectId.value == project.id) {
                _selectedProjectId.value = null
            }
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            repository.deleteTag(tag)
            if (_selectedTagId.value == tag.id) {
                _selectedTagId.value = null
            }
        }
    }

    // Task Comments CRUD
    fun getComments(taskId: Long): Flow<List<Comment>> = repository.getCommentsForTask(taskId)

    fun addComment(taskId: Long, userName: String, content: String) {
        viewModelScope.launch {
            repository.insertComment(Comment(taskId = taskId, userName = userName, content = content))
        }
    }

    // Settings Updating
    fun updateThemeMode(isDarkMode: Boolean?) {
        viewModelScope.launch {
            val current = appSettings.value
            repository.updateSettings(current.copy(isDarkMode = isDarkMode))
        }
    }

    fun updateAccentColor(hexColor: String) {
        viewModelScope.launch {
            val current = appSettings.value
            repository.updateSettings(current.copy(primaryColorHex = hexColor))
        }
    }

    fun updateGeneralPreferences(startOfWeekSunday: Boolean, timeFormat24Hour: Boolean, defaultScreen: String) {
        viewModelScope.launch {
            val current = appSettings.value
            repository.updateSettings(
                current.copy(
                    startOfWeekSunday = startOfWeekSunday,
                    timeFormat24Hour = timeFormat24Hour,
                    defaultStartupScreen = defaultScreen
                )
            )
        }
    }

    fun updateNotificationToggles(pushEnabled: Boolean, digestEnabled: Boolean, digestTime: String) {
        viewModelScope.launch {
            val current = appSettings.value
            repository.updateSettings(
                current.copy(
                    notificationsEnabled = pushEnabled,
                    dailyDigestEnabled = digestEnabled,
                    dailyDigestTime = digestTime
                )
            )
        }
    }

    // Export local data to JSON formatting - using manual serialization for 100% compilation and execution reliability
    fun exportDataAsJson(): String {
        // Collect current in-memory lists safely for backup export
        val exportedTasks = tasks.value
        val exportedProjects = projects.value
        val exportedTags = tags.value

        val sb = java.lang.StringBuilder()
        sb.append("{")
        sb.append("\"exportedAt\":").append(System.currentTimeMillis()).append(",")
        
        // Projects
        sb.append("\"projects\":[")
        exportedProjects.forEachIndexed { i, p ->
            sb.append("{")
            sb.append("\"id\":").append(p.id).append(",")
            sb.append("\"name\":\"").append(p.name.replace("\"", "\\\"")).append("\",")
            sb.append("\"color\":\"").append(p.color).append("\"")
            sb.append("}")
            if (i < exportedProjects.size - 1) sb.append(",")
        }
        sb.append("],")

        // Tags
        sb.append("\"tags\":[")
        exportedTags.forEachIndexed { i, t ->
            sb.append("{")
            sb.append("\"id\":").append(t.id).append(",")
            sb.append("\"name\":\"").append(t.name.replace("\"", "\\\"")).append("\",")
            sb.append("\"color\":\"").append(t.color).append("\"")
            sb.append("}")
            if (i < exportedTags.size - 1) sb.append(",")
        }
        sb.append("],")

        // Tasks
        sb.append("\"tasks\":[")
        exportedTasks.forEachIndexed { i, t ->
            sb.append("{")
            sb.append("\"id\":").append(t.id).append(",")
            sb.append("\"title\":\"").append(t.title.replace("\"", "\\\"")).append("\",")
            sb.append("\"description\":\"").append((t.description ?: "").replace("\"", "\\\"")).append("\",")
            sb.append("\"isCompleted\":").append(t.isCompleted).append(",")
            sb.append("\"dueDate\":").append(t.dueDate ?: "null").append(",")
            sb.append("\"dueTime\":").append(if (t.dueTime == null) "null" else "\"${t.dueTime}\"").append(",")
            sb.append("\"priority\":").append(t.priority).append(",")
            sb.append("\"projectId\":").append(t.projectId ?: "null").append(",")
            sb.append("\"tagId\":").append(t.tagId ?: "null").append(",")
            sb.append("\"recurrence\":").append("\"${t.recurrence}\"")
            sb.append("}")
            if (i < exportedTasks.size - 1) sb.append(",")
        }
        sb.append("]")

        sb.append("}")
        return sb.toString()
    }

    // Cloud Synchronization Methods via Firebase Realtime Database
    private val firebaseSyncManager = com.example.data.FirebaseSyncManager(repository)

    fun syncDataToCloud(email: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val success = firebaseSyncManager.pushLocalTasksToCloud(email)
                if (success) {
                    onComplete(true, "Successfully pushed tasks to Firebase.")
                } else {
                    onComplete(false, "Cloud sync failed. Check your network or Firebase rules.")
                }
            } catch (e: Exception) {
                onComplete(false, "Sync failed: \${e.message}")
            }
        }
    }

    fun syncDataFromCloud(email: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val success = firebaseSyncManager.pullCloudTasksToLocal(email)
                if (success) {
                    onComplete(true, "Successfully restored tasks from Firebase.")
                } else {
                    onComplete(false, "Restore failed. Check your network or Firebase rules.")
                }
            } catch (e: Exception) {
                onComplete(false, "Restore failed: \${e.message}")
            }
        }
    }
}

class TodoViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
