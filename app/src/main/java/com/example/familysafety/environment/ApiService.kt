package com.example.familysafety.environment

import com.example.familysafety.Task
import com.example.familysafety.TaskItem
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * REST API 介面 (Retrofit ApiService Interface)
 * 提供對接樹莓派 Server (MariaDB) 的環境感測與任務 API
 */
interface ApiService {
    // 1. 環境感測日誌 API
    @GET("api/sensor-logs")
    fun getSensorLogs(): Call<List<SensorLog>>

    // 2. 任務清單 (Tasks) API
    @GET("api/tasks")
    fun getTasks(): Call<List<Task>>

    @POST("api/tasks")
    fun createTask(@Body task: Task): Call<Task>

    @PUT("api/tasks/{id}")
    fun updateTask(@Path("id") id: Long, @Body task: Task): Call<Task>

    @DELETE("api/tasks/{id}")
    fun deleteTask(@Path("id") id: Long): Call<Void>

    // 3. 任務項目 (TaskItems) API
    @GET("api/task-items")
    fun getTaskItems(): Call<List<TaskItem>>

    @POST("api/task-items")
    fun createTaskItem(@Body taskItem: TaskItem): Call<TaskItem>
}
