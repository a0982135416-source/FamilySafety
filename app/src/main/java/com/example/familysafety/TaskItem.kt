package com.example.familysafety

/** STEP 6-2B：可供任務指派的一般家庭工作項目。 */
//data class TaskItem(val id: Int, val title: String, val description: String)
//前端的資料模型 (Data Class) 必須與後端的資料表架構 (Schema) 完全一致，Retrofit 才能精準對接資料。


data class TaskItem(
    val id: Int,
    val title: String,
    val description: String = "",
    val assignee: String? = null,
    val due_date: String? = null,
    val status: String? = null
)