package com.nexora.player.fragments

import android.os.Bundle
import android.view.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nexora.player.MainActivity
import com.nexora.player.adapters.MediaAdapter
import com.nexora.player.databinding.FragmentSearchBinding
import com.nexora.player.viewmodels.LibraryViewModel

class SearchFragment : Fragment() {
    private var _b: FragmentSearchBinding? = null
    private val b get() = _b!!
    private val vm: LibraryViewModel by viewModels()
    private lateinit var adapter: MediaAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSearchBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MediaAdapter { (requireActivity() as MainActivity).playMedia(it) }
        b.rvSearchResults.adapter = adapter
        b.etSearch.addTextChangedListener { text ->
            val query = text.toString().trim()
            if (query.isEmpty()) { adapter.submitList(emptyList()); b.tvSearchEmpty.visibility = View.GONE; return@addTextChangedListener }
            val all = (vm.audioFiles.value ?: emptyList()) + (vm.videoFiles.value ?: emptyList())
            val results = all.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
            adapter.submitList(results)
            b.tvSearchEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
