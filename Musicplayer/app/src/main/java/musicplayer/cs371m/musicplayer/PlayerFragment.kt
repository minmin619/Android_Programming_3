package musicplayer.cs371m.musicplayer

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import musicplayer.cs371m.musicplayer.databinding.PlayerFragmentBinding
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.floor

class PlayerFragment : Fragment() {
    // When this is true, the displayTime coroutine should not modify the seek bar
    val userModifyingSeekBar = AtomicBoolean(false)
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: RVDiffAdapter

    private var _binding: PlayerFragmentBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initRecyclerViewDividers(rv: RecyclerView) {
        // Let's have dividers between list items
        val dividerItemDecoration = DividerItemDecoration(
            rv.context, LinearLayoutManager.VERTICAL
        )
        rv.addItemDecoration(dividerItemDecoration)
    }

    // Please put all display updates in this function
    // The exception is that
    //   displayTime updates the seek bar, time passed & time remaining
    private fun updateDisplay() {
        // If settings is active, we are in the background and do
        // not have a binding.  Return early.
        if (_binding == null) {
            return
        }
        //XXX Write me. Make sure all player UI elements are up to date
        // That includes all buttons, textViews, icons & the seek bar
        // Loop indicator color
        if (viewModel.loop) {
            MainActivity.setBackgroundColor(binding.loopIndicator, Color.RED)
        } else {
            MainActivity.setBackgroundColor(binding.loopIndicator, Color.WHITE)
        }

        if (viewModel.isPlaying) {
            binding.playerPlayPauseButton.setImageResource(R.drawable.ic_pause_black_24dp)
        } else {
            binding.playerPlayPauseButton.setImageResource(R.drawable.ic_play_arrow_black_24dp)
        }

        // Play/Pause button
        if (viewModel.isPlaying) {
            MainActivity.setBackgroundDrawable(
                binding.playerPlayPauseButton,
                R.drawable.ic_pause_black_24dp
            )
        } else {
            MainActivity.setBackgroundDrawable(
                binding.playerPlayPauseButton,
                R.drawable.ic_play_arrow_black_24dp
            )
        }

        // Current/Next text
        binding.playerCurrentSongText.text = viewModel.getCurrentSongName()
        binding.playerNextSongText.text = viewModel.getNextSongName()
        binding.playerPlayPauseButton.invalidate()

        // Update adapter highlight
        adapter.setCurrentIndex(viewModel.currentIndex)
        adapter.notifyDataSetChanged()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Make the RVDiffAdapter and set it up
        //XXX Write me. Setup adapter.
        val rv = binding.playerRV
        rv.layoutManager = LinearLayoutManager(requireContext())
        initRecyclerViewDividers(rv)

        // Suppose RVDiffAdapter has a constructor that takes a lambda for item clicks
        adapter = RVDiffAdapter (viewModel){ position ->
            // If user clicks a different song item in the list
            if (position != viewModel.getCurrentSongResourceId()) {
                // If we are currently playing, that means user is switching songs => increment
                if (viewModel.isPlaying) {
                    viewModel.songsPlayed++
                }
                // Switch to the new song index
                viewModel.currentIndex = position
                // Start or prepare the new song
                if (viewModel.isPlaying) {
                    startSong()
                } else {
                    preparePlayer()
                }
                updateDisplay()
            }
        }


        rv.adapter = adapter


        val uniqueSongs = viewModel.getCopyOfSongInfo().distinctBy { it.rawId }


        adapter.submitList(uniqueSongs)
        adapter.setCurrentIndex(viewModel.currentIndex)


        //XXX Write me. Write callbacks for buttons
        // Play / pause
        binding.playerPlayPauseButton.setOnClickListener {
            if (viewModel.isPlaying) {
                pauseSong()
            } else {
                startSong()
            }
            updateDisplay()
        }

        // Skip to previous
        binding.playerSkipBackButton.setOnClickListener {
            if (viewModel.isPlaying) {
                viewModel.songsPlayed++
            }
            viewModel.prevSong()
            if (viewModel.isPlaying) {
                startSong()
            } else {
                preparePlayer()
            }
            updateDisplay()
        }

        // Skip to next
        binding.playerSkipForwardButton.setOnClickListener {
            if (viewModel.isPlaying) {
                viewModel.songsPlayed++
            }
            viewModel.nextSong()
            if (viewModel.isPlaying) {
                startSong()
            } else {
                preparePlayer()
            }
            updateDisplay()
        }

        // Shuffle (permute)
        binding.playerPermuteButton.setOnClickListener {

            val newList = viewModel.shuffleAndReturnCopyOfSongInfo()
            adapter.submitList(newList)
            adapter.setCurrentIndex(viewModel.currentIndex)
            if (viewModel.isPlaying) {
                startSong()
            } else {
                preparePlayer()
            }
            updateDisplay()
        }

        // Loop toggle
        binding.loopIndicator.setOnClickListener {
            viewModel.loop = !viewModel.loop
            // If we are currently playing, set isLooping immediately
            viewModel.player.isLooping = viewModel.loop
            updateDisplay()
        }
        // 3) SeekBar listener
        binding.playerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                // Usually do nothing here unless you want "live" updates
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                userModifyingSeekBar.set(true)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                userModifyingSeekBar.set(false)
                val newPos = seekBar?.progress ?: 0
                viewModel.player.seekTo(newPos)
            }
        })

        // Initialize UI

        //XXX Write me. binding.playerSeekBar.setOnSeekBarChangeListener

        updateDisplay()

        // Don't change this code.  We are launching a coroutine (user-level thread) that will
        // execute concurrently with our code, but will update the displayed time
        val millisec = 100L
        viewLifecycleOwner.lifecycleScope.launch {
            displayTime(millisec)
        }
    }

    // The suspend modifier marks this as a function called from a coroutine
    // Note, this whole function is somewhat reminiscent of the Timer class
    // from Fling and Peck.  We use an independent thread to manage one small
    // piece of our GUI.  This coroutine should not modify any data accessed
    // by the main thread (it can read property values)
    private suspend fun displayTime(misc: Long) {
        // This only runs while the display is active
        while (viewLifecycleOwner.lifecycleScope.coroutineContext.isActive) {
            val currentPosition = viewModel.player.currentPosition
            val maxTime = viewModel.player.duration
            // Update the seek bar (if the user isn't updating it)
            // and update the passed and remaining time
            //XXX Write me
            // if user is not dragging
            if (!userModifyingSeekBar.get()) {
                binding.playerSeekBar.max = maxTime
                binding.playerSeekBar.progress = currentPosition
            }

            // update time text
            binding.playerTimePassedText.text = convertTime(currentPosition)
            binding.playerTimeRemainingText.text = convertTime(maxTime - currentPosition)

            // Leave this code as is.  it inserts a delay so that this thread does
            // not consume too much CPU
            delay(misc)
        }
    }

    // This method converts time in milliseconds to minutes-second formatted string
    // with two digit minutes and two digit sections, e.g., 01:30
    private fun convertTime(milliseconds: Int): String {
        //XXX Write me
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)

    }

    // XXX Write me, handle player dynamics and currently playing/next song
    private fun preparePlayer() {
        try {
            viewModel.player.stop()
            viewModel.player.release()
        } catch (_: Exception) {}

        val resId = viewModel.getCurrentSongResourceId()
        viewModel.player = MediaPlayer.create(requireContext(), resId)
        viewModel.player.isLooping = viewModel.loop
    }

    /**
     * Start playing the current song (prepare first, then start).
     */
    private fun startSong() {
        preparePlayer()
        viewModel.isPlaying = true
        viewModel.player.start()
    }

    /**
     * Pause the current song.
     */
    private fun pauseSong() {
        viewModel.player.pause()
        viewModel.isPlaying = false
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
