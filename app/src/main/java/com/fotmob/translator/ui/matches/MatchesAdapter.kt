package com.fotmob.translator.ui.matches

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.fotmob.translator.R
import com.fotmob.translator.data.model.Match
import com.fotmob.translator.databinding.ItemMatchBinding
import com.fotmob.translator.util.DateUtils

class MatchesAdapter(
    private var matches: List<Match> = emptyList()
) : RecyclerView.Adapter<MatchesAdapter.MatchViewHolder>() {

    private var translatedTexts: Map<String, String> = emptyMap()
    private var onMatchClickListener: ((Match) -> Unit)? = null

    fun setOnMatchClickListener(listener: (Match) -> Unit) {
        onMatchClickListener = listener
    }

    fun updateData(newMatches: List<Match>) {
        matches = newMatches
        notifyDataSetChanged()
    }

    fun updateTranslations(translations: Map<String, String>) {
        val newTranslatedTexts = translatedTexts + translations
        if (newTranslatedTexts != translatedTexts) {
            translatedTexts = newTranslatedTexts
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = ItemMatchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(matches[position])
    }

    override fun getItemCount(): Int = matches.size

    inner class MatchViewHolder(
        private val binding: ItemMatchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(match: Match) {
            val context = binding.root.context

            // League name (translated)
            val leagueName = match.league?.name ?: ""
            binding.tvLeagueName.text = translatedTexts[leagueName] ?: leagueName

            // Home team
            val homeName = match.homeTeam?.name ?: "Unknown"
            binding.tvHomeTeam.text = translatedTexts[homeName] ?: homeName
            match.homeTeam?.imageUrl?.let { url ->
                binding.ivHomeTeam.load(url) {
                    crossfade(true)
                    error(R.drawable.ic_launcher_background)
                }
            }

            // Away team
            val awayName = match.awayTeam?.name ?: "Unknown"
            binding.tvAwayTeam.text = translatedTexts[awayName] ?: awayName
            match.awayTeam?.imageUrl?.let { url ->
                binding.ivAwayTeam.load(url) {
                    crossfade(true)
                    error(R.drawable.ic_launcher_background)
                }
            }

            // Score
            binding.tvScore.text = match.displayScore

            // Status
            binding.tvStatus.text = match.matchStateText
            when {
                match.status?.finished == true -> {
                    binding.tvStatus.setTextColor(context.getColor(R.color.match_finished))
                }
                match.status?.started == true -> {
                    binding.tvStatus.setTextColor(context.getColor(R.color.match_live))
                }
                else -> {
                    binding.tvStatus.setTextColor(context.getColor(R.color.match_upcoming))
                    binding.tvScore.text = DateUtils.formatMatchTime(match.startTime)
                }
            }

            // Match time
            binding.tvMatchTime.text = DateUtils.formatMatchTime(match.startTime)

            // Click listener
            binding.root.setOnClickListener {
                onMatchClickListener?.invoke(match)
            }
        }
    }
}
