package com.fotmob.translator.ui.matches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fotmob.translator.FotMobApplication
import com.fotmob.translator.data.model.Match
import com.fotmob.translator.databinding.FragmentMatchesBinding
import com.fotmob.translator.util.DateUtils
import kotlinx.coroutines.launch

class MatchesFragment : Fragment() {

    private var _binding: FragmentMatchesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MatchesAdapter
    private lateinit var app: FotMobApplication

    private var currentDateOffset = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        app = requireActivity().application as FotMobApplication

        setupRecyclerView()
        setupSwipeRefresh()
        setupDateNavigation()
        loadMatches()
    }

    private fun setupRecyclerView() {
        adapter = MatchesAdapter()
        adapter.setOnMatchClickListener { match ->
            // Could navigate to match detail in future
        }
        binding.recyclerViewMatches.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewMatches.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadMatches()
        }
    }

    private fun setupDateNavigation() {
        binding.btnPrevDay.setOnClickListener {
            currentDateOffset--
            loadMatches()
        }

        binding.btnNextDay.setOnClickListener {
            currentDateOffset++
            loadMatches()
        }

        binding.btnToday.setOnClickListener {
            currentDateOffset = 0
            loadMatches()
        }
    }

    private fun loadMatches() {
        binding.swipeRefreshLayout.isRefreshing = true
        val date = DateUtils.getDateOffset(currentDateOffset)
        binding.tvDate.text = when (currentDateOffset) {
            0 -> "Today - ${DateUtils.getTodayDate()}"
            -1 -> "Yesterday - ${date}"
            1 -> "Tomorrow - ${date}"
            else -> date
        }

        lifecycleScope.launch {
            try {
                val matches = app.repository.getMatches(date)
                adapter.updateData(matches)
                if (matches.isEmpty()) {
                    // Show empty state
                    android.widget.Toast.makeText(requireContext(), "No matches found for this date", android.widget.Toast.LENGTH_SHORT).show()
                }
                translateMatches(matches)
            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), "Failed to load matches: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun translateMatches(matches: List<Match>) {
        lifecycleScope.launch {
            try {
                val textsToTranslate = mutableSetOf<String>()

                for (match in matches) {
                    match.league?.name?.let { textsToTranslate.add(it) }
                    match.homeTeam?.name?.let { textsToTranslate.add(it) }
                    match.awayTeam?.name?.let { textsToTranslate.add(it) }
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
