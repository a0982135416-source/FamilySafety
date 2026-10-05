package com.example.familysafety

import java.util.Calendar

/** Local current date only; completed tasks remain part of the Home list and denominator. */
internal fun List<Task>.dueToday(now: Long = System.currentTimeMillis()): List<Task> {
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
    return filter { it.dueDate >= start.timeInMillis && it.dueDate < end.timeInMillis }
}
