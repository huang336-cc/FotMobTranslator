package com.fotmob.translator.ui.leagues

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.fotmob.translator.R
import com.fotmob.translator.data.model.League
import com.fotmob.translator.databinding.ItemLeagueBinding

class LeaguesAdapter(
    private var leagues: List<League> = emptyList()
) : RecyclerView.Adapter<LeaguesAdapter.LeagueViewHolder>() {

    private var translatedTexts: Map<String, String> = emptyMap()
    private var onLeagueClickListener: ((League) -> Unit)? = null

    fun setOnLeagueClickListener(listener: (League) -> Unit) {
        onLeagueClickListener = listener
    }

    fun updateData(newLeagues: List<League>) {
        leagues = newLeagues
        notifyDataSetChanged()
    }

    fun updateTranslations(translations: Map<String, String>) {
        translatedTexts = translatedTexts + translations
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeagueViewHolder {
        val binding = ItemLeagueBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LeagueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeagueViewHolder, position: Int) {
        holder.bind(leagues[position])
    }

    override fun getItemCount(): Int = leagues.size

    inner class LeagueViewHolder(
        private val binding: ItemLeagueBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(league: League) {
            val name = league.name
            binding.tvLeagueName.text = translatedTexts[name] ?: name

            val country = league.country ?: ""
            binding.tvCountry.text = translatedTexts[country] ?: country

            league.imageUrl?.let { url ->
                binding.ivLeagueLogo.load(url) {
                    crossfade(true)
                    error(R.drawable.ic_launcher_background)
                }
            }

            binding.root.setOnClickListener {
                onLeagueClickListener?.invoke(league)
            }
        }
    }
}
