package musicplayer.cs371m.musicplayer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import musicplayer.cs371m.musicplayer.databinding.SongRowBinding

// Pass in a function called clickListener that takes a view and a songName
// as parameters.  Call clickListener when the row is clicked.
class RVDiffAdapter(private val viewModel: MainViewModel,
                    private val clickListener: (songIndex : Int)->Unit)
    // https://developer.android.com/reference/androidx/recyclerview/widget/ListAdapter
    // Slick adapter that provides submitList, so you don't worry about how to update
    // the list, you just submit a new one when you want to change the list and the
    // Diff class computes the smallest set of changes that need to happen.
    // NB: Both the old and new lists must both be in memory at the same time.
    // So you can copy the old list, change it into a new list, then submit the new list.
    : ListAdapter<SongInfo,
        RVDiffAdapter.ViewHolder>(Diff())
{
    companion object {
        val TAG = "RVDiffAdapter"
    }
    private var currentIndex = -1

    fun setCurrentIndex(index: Int) {
        currentIndex = index

        notifyDataSetChanged()
    }

    // ViewHolder pattern holds row binding
    inner class ViewHolder(val songRowBinding : SongRowBinding)
        : RecyclerView.ViewHolder(songRowBinding.root) {
        init {
            //XXX Write me.
            // Attach a click listener to the entire row
            songRowBinding.root.setOnClickListener {
                val position = adapterPosition
                // Make sure the position is valid
                if (position != RecyclerView.NO_POSITION) {
                    clickListener(position)
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //XXX Write me.
        val inflater = LayoutInflater.from(parent.context)
        val binding = SongRowBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //XXX Write me.
        val song = getItem(position)
        // Display the song name and time in the row
        holder.songRowBinding.itemSongTitle.text = song.name
        holder.songRowBinding.itemSongTime.text = song.time
        // Highlight if it's the currently selected (playing) song
        if (position == viewModel.currentIndex) {
            MainActivity.setBackgroundColor(holder.songRowBinding.root, Color.YELLOW)
        } else {
            MainActivity.setBackgroundColor(holder.songRowBinding.root, Color.WHITE)
        }
    }

    class Diff : DiffUtil.ItemCallback<SongInfo>() {
        // Item identity
        override fun areItemsTheSame(oldItem: SongInfo, newItem: SongInfo): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
        // Item contents are the same, but the object might have changed
        override fun areContentsTheSame(oldItem: SongInfo, newItem: SongInfo): Boolean {
            return oldItem.name == newItem.name
                    && oldItem.rawId == newItem.rawId
                    && oldItem.time == newItem.time
        }
    }
}

