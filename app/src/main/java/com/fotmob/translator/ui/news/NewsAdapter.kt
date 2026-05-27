package com.fotmob.translator.ui.news

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.fotmob.translator.R
import com.fotmob.translator.data.model.NewsItem
import com.fotmob.translator.databinding.ItemNewsBinding
import com.fotmob.translator.util.DateUtils

class NewsAdapter(
    private var newsItems: List<NewsItem> = emptyList()
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    private var translatedTexts: Map<String, String> = emptyMap()
    private var onNewsClickListener: ((NewsItem) -> Unit)? = null

    fun setOnNewsClickListener(listener: (NewsItem) -> Unit) {
        onNewsClickListener = listener
    }

    fun updateData(newItems: List<NewsItem>) {
        newsItems = newItems
        notifyDataSetChanged()
    }

    fun updateTranslations(translations: Map<String, String>) {
        translatedTexts = translatedTexts + translations
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(newsItems[position])
    }

    override fun getItemCount(): Int = newsItems.size

    inner class NewsViewHolder(
        private val binding: ItemNewsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NewsItem) {
            val title = item.title
            binding.tvTitle.text = translatedTexts[title] ?: title

            val summary = item.summary
            binding.tvSummary.text = translatedTexts[summary] ?: summary

            binding.tvDate.text = DateUtils.formatRelativeTime(item.publishedTime)

            item.source?.let { source ->
                binding.tvSource.text = source
                binding.tvSource.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.tvSource.visibility = android.view.View.GONE
            }

            item.thumbnailUrl?.let { url ->
                binding.ivThumbnail.load(url) {
                    crossfade(true)
                    error(R.drawable.ic_launcher_background)
                }
                binding.ivThumbnail.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.ivThumbnail.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onNewsClickListener?.invoke(item)
            }
        }
    }
}
