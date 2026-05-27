package com.fotmob.translator.ui.players

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.fotmob.translator.R
import com.fotmob.translator.data.model.Player
import com.fotmob.translator.databinding.ItemPlayerBinding

class PlayersAdapter(
    private var players: List<Player> = emptyList()
) : RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder>() {

    private var translatedTexts: Map<String, String> = emptyMap()
    private var onPlayerClickListener: ((Player) -> Unit)? = null

    fun setOnPlayerClickListener(listener: (Player) -> Unit) {
        onPlayerClickListener = listener
    }

    fun updateData(newPlayers: List<Player>) {
        players = newPlayers
        notifyDataSetChanged()
    }

    fun updateTranslations(translations: Map<String, String>) {
        translatedTexts = translatedTexts + translations
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = ItemPlayerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(players[position])
    }

    override fun getItemCount(): Int = players.size

    inner class PlayerViewHolder(
        private val binding: ItemPlayerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(player: Player) {
            val name = player.name
            binding.tvPlayerName.text = translatedTexts[name] ?: name

            val teamName = player.playerTeam?.name ?: ""
            binding.tvTeamName.text = translatedTexts[teamName] ?: teamName

            binding.tvPosition.text = player.displayPosition
            binding.tvRating.text = player.displayRating

            player.goals?.let { goals ->
                binding.tvGoals.text = goals.toString()
                binding.tvGoals.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.tvGoals.visibility = android.view.View.GONE
            }

            player.assists?.let { assists ->
                binding.tvAssists.text = assists.toString()
                binding.tvAssists.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.tvAssists.visibility = android.view.View.GONE
            }

            player.appearances?.let { apps ->
                binding.tvAppearances.text = "$apps Apps"
                binding.tvAppearances.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.tvAppearances.visibility = android.view.View.GONE
            }

            player.imageUrl?.let { url ->
                binding.ivPlayerPhoto.load(url) {
                    crossfade(true)
                    error(R.drawable.ic_launcher_background)
                }
            }

            player.playerTeam?.imageUrl?.let { url ->
                binding.ivTeamLogo.load(url) {
                    crossfade(true)
                    error(R.drawable.ic_launcher_background)
                }
            }

            binding.root.setOnClickListener {
                onPlayerClickListener?.invoke(player)
            }
        }
    }
}
