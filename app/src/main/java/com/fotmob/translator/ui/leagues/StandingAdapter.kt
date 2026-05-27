package com.fotmob.translator.ui.leagues

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.fotmob.translator.R
import com.fotmob.translator.data.model.Standing
import com.fotmob.translator.databinding.ItemStandingBinding

class StandingAdapter(
    private var standings: List<Standing> = emptyList()
) : RecyclerView.Adapter<StandingAdapter.StandingViewHolder>() {

    private var translatedTexts: Map<String, String> = emptyMap()

    fun updateData(newStandings: List<Standing>) {
        standings = newStandings
        notifyDataSetChanged()
    }

    fun updateTranslations(translations: Map<String, String>) {
        translatedTexts = translatedTexts + translations
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StandingViewHolder {
        val binding = ItemStandingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StandingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StandingViewHolder, position: Int) {
        holder.bind(standings[position])
    }

    override fun getItemCount(): Int = standings.size

    inner class StandingViewHolder(
        private val binding: ItemStandingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(standing: Standing) {
            binding.tvRank.text = standing.rank.toString()

            val teamName = standing.team?.name ?: "Unknown"
            binding.tvTeamName.text = translatedTexts[teamName] ?: teamName

            standing.team?.imageUrl?.let { url ->
                binding.ivTeamLogo.load(url) {
                    crossfade(true)
                    error(R.drawable.ic_launcher_background)
                }
            }

            binding.tvPlayed.text = standing.played.toString()
            binding.tvWins.text = standing.wins.toString()
            binding.tvDraws.text = standing.draws.toString()
            binding.tvLosses.text = standing.losses.toString()
            binding.tvGoalsFor.text = standing.goalsFor.toString()
            binding.tvGoalsAgainst.text = standing.goalsAgainst.toString()
            binding.tvGoalDiff.text = standing.goalDiffText
            binding.tvPoints.text = standing.points.toString()

            // Highlight top positions
            val context = binding.root.context
            when (standing.rank) {
                1 -> binding.tvRank.setBackgroundColor(context.getColor(R.color.champion))
                in 2..4 -> binding.tvRank.setBackgroundColor(context.getColor(R.color.champions_league))
                in 5..6 -> binding.tvRank.setBackgroundColor(context.getColor(R.color.europa_league))
                else -> binding.tvRank.setBackgroundColor(context.getColor(android.R.color.transparent))
            }
        }
    }
}
