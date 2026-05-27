package com.fotmob.translator.ui.players

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fotmob.translator.FotMobApplication
import com.fotmob.translator.data.model.Player
import com.fotmob.translator.databinding.FragmentPlayersBinding
import kotlinx.coroutines.launch

class PlayersFragment : Fragment() {

    private var _binding: FragmentPlayersBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlayersAdapter
    private lateinit var app: FotMobApplication

    // Popular team IDs for the spinner
    private val popularTeams = mapOf(
        8455L to "Manchester City",
        8650L to "Manchester United",
        8634L to "Arsenal",
        8668L to "Chelsea",
        8586L to "Liverpool",
        8602L to "Real Madrid",
        8633L to "Barcelona",
        8594L to "Bayern Munich",
        8456L to "Paris Saint-Germain",
        9879L to "Inter Milan",
        8543L to "Juventus",
        9885L to "AC Milan"
    )

    private var selectedTeamId: Long = 8455L // Default: Manchester City

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        app = requireActivity().application as FotMobApplication

        setupRecyclerView()
        setupSwipeRefresh()
        setupTeamSelector()
        loadPlayers()
    }

    private fun setupRecyclerView() {
        adapter = PlayersAdapter()
        adapter.setOnPlayerClickListener { player ->
            Toast.makeText(requireContext(), "${player.name} - ${player.playerTeam?.name}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewPlayers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPlayers.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadPlayers()
        }
    }

    private fun setupTeamSelector() {
        val teamNames = popularTeams.values.toList()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            teamNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTeam.adapter = adapter

        binding.spinnerTeam.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.view.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val teamName = teamNames[position]
                selectedTeamId = popularTeams.entries.find { it.value == teamName }?.key ?: return
                loadPlayers()
            }

            override fun onNothingSelected(parent: android.view.AdapterView<*>?) {}
        }
    }

    private fun loadPlayers() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val players = app.repository.getTeamPlayers(selectedTeamId)
                adapter.updateData(players)
                translatePlayers(players)
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun translatePlayers(players: List<Player>) {
        lifecycleScope.launch {
            try {
                val textsToTranslate = mutableSetOf<String>()
                for (player in players) {
                    textsToTranslate.add(player.name)
                    player.playerTeam?.name?.let { textsToTranslate.add(it) }
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
