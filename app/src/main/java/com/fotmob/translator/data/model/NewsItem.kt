package com.fotmob.translator.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NewsItem(
    val title: String,
    val summary: String,
    val link: String,
    val publishedTime: Long,
    val thumbnailUrl: String? = null,
    val source: String? = null
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(publishedTime))
        }
}
