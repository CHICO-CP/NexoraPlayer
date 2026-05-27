package com.nexora.player.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import com.nexora.player.MainActivity
import com.nexora.player.R
import com.nexora.player.adapters.MediaAdapter
import com.nexora.player.databinding.FragmentLibraryBinding
import com.nexora.player.viewmodels.LibraryViewModel

class LibraryFragment : Fragment() {
    private var _b: FragmentLibraryBinding? = null
    private val b get() = _b!!
    private val vm: LibraryViewModel by viewModels()
    private lateinit var adapter: MediaAdapter
    private var currentTab = 0

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentLibraryBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MediaAdapter { (requireActivity() as MainActivity).playMedia(it) }
        b.rvLibrary.adapter = adapter
        b.tabLayoutLibrary.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { currentTab = tab.position; updateList() }
            override fun onTabUnselected(t: TabLayout.Tab) {}
            override fun onTabReselected(t: TabLayout.Tab) {}
        })
        b.swipeRefreshLibrary.setColorSchemeResources(R.color.nexora_primary, R.color.nexora_secondary)
        b.swipeRefreshLibrary.setOnRefreshListener { vm.loadMedia() }
        vm.audioFiles.observe(viewLifecycleOwner) { updateList() }
        vm.videoFiles.observe(viewLifecycleOwner) { updateList() }
        vm.isLoading.observe(viewLifecycleOwner) { b.swipeRefreshLibrary.isRefreshing = it }
    }

    private fun updateList() {
        val list = if (currentTab == 0) vm.audioFiles.value ?: emptyList() else vm.videoFiles.value ?: emptyList()
        adapter.submitList(list)
        b.tvEmptyLibrary.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
