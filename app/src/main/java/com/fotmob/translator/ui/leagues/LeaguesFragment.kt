package com.fotmob.translator.ui.leagues

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fotmob.translator.FotMobApplication
import com.fotmob.translator.data.model.League
import com.fotmob.translator.data.model.Standing
import com.fotmob.translator.databinding.FragmentLeaguesBinding
import kotlinx.coroutines.launch

class LeaguesFragment : Fragment() {

    private var _binding: FragmentLeaguesBinding? = null
    private val binding get() = _binding!!

    private lateinit var leaguesAdapter: LeaguesAdapter
    private lateinit var standingAdapter: StandingAdapter
    private lateinit var app: FotMobApplication

    private var leagues: List<League> = emptyList()
    private var currentLeagueId: Long = 47 // Default: Premier League

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaguesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        app = requireActivity().application as FotMobApplication

        setupRecyclerViews()
        setupSwipeRefresh()
        setupTabLayout()
        loadLeagues()
    }

    private fun setupRecyclerViews() {
        leaguesAdapter = LeaguesAdapter()
        leaguesAdapter.setOnLeagueClickListener { league ->
            currentLeagueId = league.id
            binding.tabLayout.getTabAt(1)?.select()
            loadStandings()
        }
        binding.recyclerViewLeagues.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewLeagues.adapter = leaguesAdapter

        standingAdapter = StandingAdapter()
        binding.recyclerViewStandings.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewStandings.adapter = standingAdapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            val selectedTab = binding.tabLayout.selectedTabPosition
            if (selectedTab == 0) {
                loadLeagues()
            } else {
                loadStandings()
            }
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Leagues"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Standings"))

        binding.tabLayout.addOnTabSelectedListener(object : androidx.viewpager.widget.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: androidx.viewpager.widget.TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.recyclerViewLeagues.visibility = View.VISIBLE
                        binding.recyclerViewStandings.visibility = View.GONE
                    }
                    1 -> {
                        binding.recyclerViewLeagues.visibility = View.GONE
                        binding.recyclerViewStandings.visibility = View.VISIBLE
                        if (standingAdapter.itemCount == 0) {
                            loadStandings()
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: androidx.viewpager.widget.TabLayout.Tab) {}
            override fun onTabReselected(tab: androidx.viewpager.widget.TabLayout.Tab) {}
        })
    }

    private fun loadLeagues() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                leagues = app.repository.getAllLeagues()
                leaguesAdapter.updateData(leagues)
                translateLeagues(leagues)
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun translateLeagues(leagueList: List<League>) {
        lifecycleScope.launch {
            try {
                val textsToTranslate = mutableSetOf<String>()
                for (league in leagueList) {
                    textsToTranslate.add(league.name)
                    league.country?.let { textsToTranslate.add(it) }
                }

                val translations = app.translator.translateBatch(textsToTranslate.toList())
                val translationMap = textsToTranslate.zip(translations).toMap()

                leaguesAdapter.updateTranslations(translationMap)
            } catch (e: Exception) {
                // Translation failed, show original text
            }
        }
    }

    private fun loadStandings() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val standings = app.repository.getLeagueTable(currentLeagueId)
                standingAdapter.updateData(standings)
                translateStandings(standings)
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun translateStandings(standings: List<Standing>) {
        lifecycleScope.launch {
            try {
                val textsToTranslate = mutableSetOf<String>()
                for (standing in standings) {
                    standing.team?.name?.let { textsToTranslate.add(it) }
                }

                val translations = app.translator.translateBatch(textsToTranslate.toList())
                val translationMap = textsToTranslate.zip(translations).toMap()

                standingAdapter.updateTranslations(translationMap)
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
