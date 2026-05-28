package com.fotmob.translator.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private const val FOTMOB_DATE_FORMAT = "yyyyMMdd"

    /**
     * Get today's date in FotMob API format (YYYYMMDD)
     */
    fun getTodayDate(): String {
        val sdf = SimpleDateFormat(FOTMOB_DATE_FORMAT, Locale.US)
        return sdf.format(Date())
    }

    /**
     * Get date offset from today in FotMob API format
     * @param daysOffset positive for future, negative for past
     */
    fun getDateOffset(daysOffset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, daysOffset)
        val sdf = SimpleDateFormat(FOTMOB_DATE_FORMAT, Locale.US)
        return sdf.format(calendar.time)
    }

    /**
     * Convert timestamp to readable date string
     */
    fun formatMatchTime(timestamp: Long?): String {
        if (timestamp == null) return ""
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val now = Calendar.getInstance()
        val isToday = calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

        return if (isToday) {
            timeFormat.format(calendar.time)
        } else {
            dateFormat.format(calendar.time)
        }
    }

    /**
     * Convert timestamp to full date string
     */
    fun formatFullDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Format timestamp to relative time (e.g., "2小时前")
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
            else -> formatFullDate(timestamp)
        }
    }

    /**
     * Get day of week name
     */
    fun getDayOfWeek(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Get date string for display
     */
    fun getDisplayDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
