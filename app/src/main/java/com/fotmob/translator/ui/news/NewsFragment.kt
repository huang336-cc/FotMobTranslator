package com.fotmob.translator.ui.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fotmob.translator.FotMobApplication
import com.fotmob.translator.R
import com.fotmob.translator.data.model.NewsItem
import com.fotmob.translator.databinding.FragmentNewsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: NewsAdapter
    private lateinit var app: FotMobApplication

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        app = requireActivity().application as FotMobApplication

        setupRecyclerView()
        setupSwipeRefresh()
        loadNews()
    }

    private fun setupRecyclerView() {
        adapter = NewsAdapter()
        adapter.setOnNewsClickListener { newsItem ->
            val bundle = Bundle().apply {
                putString("url", newsItem.link)
                putString("title", newsItem.title)
                putString("summary", newsItem.summary)
                putString("thumbnailUrl", newsItem.thumbnailUrl)
                putLong("publishedTime", newsItem.publishedTime)
                putString("source", newsItem.source)
            }
            findNavController().navigate(R.id.action_news_to_detail, bundle)
        }
        binding.recyclerViewNews.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNews.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadNews()
        }
    }

    private fun loadNews() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val newsItems = app.repository.getNewsFeed()
                if (newsItems.isEmpty()) {
                    Toast.makeText(requireContext(), "暂无新闻", Toast.LENGTH_SHORT).show()
                }
                adapter.updateData(newsItems)

                // 立即启动翻译（异步并行）
                launch(Dispatchers.IO) {
                    translateNewsItems(newsItems)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private suspend fun translateNewsItems(newsItems: List<NewsItem>) {
        try {
            val textsToTranslate = mutableSetOf<String>()
            for (item in newsItems) {
                if (item.title.isNotBlank()) textsToTranslate.add(item.title)
                if (item.summary.isNotBlank()) textsToTranslate.add(item.summary)
            }

            if (textsToTranslate.isEmpty()) return

            val translations = app.translator.translateBatch(textsToTranslate.toList())
            val translationMap = textsToTranslate.zip(translations).toMap()

            // 切回主线程更新 UI
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                adapter.updateTranslations(translationMap)
            }
        } catch (e: Exception) {
            // 翻译失败，显示原文
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
