package musicplayer.cs371m.musicplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import musicplayer.cs371m.musicplayer.databinding.SettingsFragmentBinding

class SettingsFragment : Fragment() {
    // https://developer.android.com/topic/libraries/view-binding#fragments
    private var _binding: SettingsFragmentBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
    private fun initMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Clear menu because we don't want settings icon
                menu.clear()
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Do nothing
                return false
            }
        }, viewLifecycleOwner)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMenu()
        // XXX Write me findNavController().popBackStack() exits
        // 1) Show number of songs played so far
        binding.settingsSongsPlayedText.text = viewModel.songsPlayed.toString()

        // 2) Show current loop state in the switch
        binding.settingsLoopSwitch.isChecked = viewModel.loop

        // 3) Cancel button discards changes and returns to player
        binding.settingsCancelButton.setOnClickListener {
            // Pop the back stack -> go back to the Player
            findNavController().popBackStack()
        }

        // 4) OK button saves changes (loop mode) and returns
        binding.settingsOkButton.setOnClickListener {
            // Commit the user's loop choice
            viewModel.loop = binding.settingsLoopSwitch.isChecked
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
