package com.example.data

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.data.TodoRepository

class FirebaseSyncManager(private val repository: TodoRepository) {
    // Note: The user requested explanations of where to paste these functions.
    // They are fully implemented here and can be called from TodoViewModel.kt
    // inside the syncDataToCloud and syncDataFromCloud functions!
    
    // We sanitize the email to use it as a Firebase path key (Firebase keys cannot contain '.', '#', '$', '[', or ']')
    private fun sanitizeEmail(email: String): String {
        return email.replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
    }

    suspend fun pushLocalTasksToCloud(email: String): Boolean {
        return try {
            val database = FirebaseDatabase.getInstance()
            val safeEmail = sanitizeEmail(email)
            val userRef = database.getReference("users").child(safeEmail).child("tasks")
            
            // Get current local tasks once
            val localTasks = repository.allTasks.first()
            
            // Convert to a map for Firebase (Map<String, Task>)
            // using the task ID as the string key
            val taskMap = localTasks.associateBy { it.id.toString() }
            
            // Upload to Realtime Database
            userRef.setValue(taskMap).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun pullCloudTasksToLocal(email: String): Boolean {
        return try {
            val database = FirebaseDatabase.getInstance()
            val safeEmail = sanitizeEmail(email)
            val userRef = database.getReference("users").child(safeEmail).child("tasks")
            
            // Fetch from Realtime Database once
            val snapshot = userRef.get().await()
            if (snapshot.exists()) {
                val tasksToInsert = mutableListOf<Task>()
                
                // Parse each task
                for (child in snapshot.children) {
                    val task = child.getValue(Task::class.java)
                    if (task != null) {
                        tasksToInsert.add(task)
                    }
                }
                
                // Save back to local Room Database
                for (task in tasksToInsert) {
                    // Update if exists, or insert new
                    val existing = repository.getTaskById(task.id)
                    if (existing != null) {
                        repository.updateTask(task)
                    } else {
                        repository.insertTask(task)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
