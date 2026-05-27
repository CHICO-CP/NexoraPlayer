package com.nexora.player.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nexora.player.R
import com.nexora.player.databinding.ItemMediaBinding
import com.nexora.player.models.MediaItem
import com.nexora.player.models.MediaType
import com.nexora.player.utils.TimeUtils

class MediaAdapter(
    private val onItemClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, MediaAdapter.ViewHolder>(DIFF_CB) {

    companion object {
        private val DIFF_CB = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(o: MediaItem, n: MediaItem) = o.id == n.id
            override fun areContentsTheSame(o: MediaItem, n: MediaItem) = o == n
        }
    }

    inner class ViewHolder(private val b: ItemMediaBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: MediaItem) {
            b.tvMediaTitle.text    = item.title
            b.tvMediaSubtitle.text = if (item.mediaType == MediaType.AUDIO) item.artist else "Video"
            b.tvMediaDuration.text = TimeUtils.formatDuration(item.duration)
            val iconRes = if (item.mediaType == MediaType.AUDIO) R.drawable.ic_music_note else R.drawable.ic_video
            b.ivMediaTypeIcon.setImageResource(iconRes)
            Glide.with(b.root.context).load(item.albumArtUri)
                .placeholder(iconRes).error(iconRes).centerCrop().into(b.ivMediaThumb)
            b.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}
