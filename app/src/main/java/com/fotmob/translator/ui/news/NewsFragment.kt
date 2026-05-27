package com.fotmob.translator.ui.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fotmob.translator.FotMobApplication
import com.fotmob.translator.data.model.NewsItem
import com.fotmob.translator.databinding.FragmentNewsBinding
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
            if (newsItem.link.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.link))
                startActivity(intent)
            }
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
                adapter.updateData(newsItems)
                translateNews(newsItems)
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun translateNews(newsItems: List<NewsItem>) {
        lifecycleScope.launch {
            try {
                val textsToTranslate = mutableSetOf<String>()
                for (item in newsItems) {
                    if (item.title.isNotBlank()) textsToTranslate.add(item.title)
                    if (item.summary.isNotBlank()) textsToTranslate.add(item.summary)
                }

                val translations = app.translator.translateBatch(textsToTranslate.toList())
                val translationMap = textsToTranslate.zip(translations).toMap()

                adapter.updateTranslations(translationMap)
            } catch (e: Exception) {
                // Translation failed, show original text
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
