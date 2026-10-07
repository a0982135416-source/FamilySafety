package com.example.familysafety

import com.example.familysafety.environment.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * 任務資料倉庫 (Task Repository)
 * 負責將任務資料透過 Retrofit API 傳輸至樹莓派與 MariaDB
 */
object TaskRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * 發送單筆任務至樹莓派 Server
     */
    fun sendTaskToRaspberryPi(task: Task, onResult: (Boolean, String?) -> Unit) {
        apiService.createTask(task).enqueue(object : Callback<Task> {
            override fun onResponse(call: Call<Task>, response: Response<Task>) {
                if (response.isSuccessful) {
                    onResult(true, "傳送成功")
                } else {
                    onResult(false, "伺服器回應錯誤: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Task>, t: Throwable) {
                onResult(false, "網路傳送失敗: ${t.localizedMessage}")
            }
        })
    }

    /**
     * 發送單筆任務項目 (TaskItem) 至樹莓派 Server
     */
    fun sendTaskItemToRaspberryPi(taskItem: TaskItem, onResult: (Boolean, String?) -> Unit) {
        apiService.createTaskItem(taskItem).enqueue(object : Callback<TaskItem> {
            override fun onResponse(call: Call<TaskItem>, response: Response<TaskItem>) {
                if (response.isSuccessful) {
                    onResult(true, "項目傳送成功")
                } else {
                    onResult(false, "伺服器回應錯誤: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<TaskItem>, t: Throwable) {
                onResult(false, "網路傳送失敗: ${t.localizedMessage}")
            }
        })
    }

    /**
     * 從樹莓派取得遠端最新任務清單
     */
    fun fetchTasksFromRaspberryPi(onResult: (List<Task>?, String?) -> Unit) {
        apiService.getTasks().enqueue(object : Callback<List<Task>> {
            override fun onResponse(call: Call<List<Task>>, response: Response<List<Task>>) {
                if (response.isSuccessful) {
                    onResult(response.body(), null)
                } else {
                    onResult(null, "無法讀取遠端任務: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Task>>, t: Throwable) {
                onResult(null, "連線至樹莓派失敗: ${t.localizedMessage}")
            }
        })
    }
}
