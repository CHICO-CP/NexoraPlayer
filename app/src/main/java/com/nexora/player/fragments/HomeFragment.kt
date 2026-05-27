package com.nexora.player.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nexora.player.MainActivity
import com.nexora.player.adapters.MediaAdapter
import com.nexora.player.databinding.FragmentHomeBinding
import com.nexora.player.viewmodels.HomeViewModel
import java.util.Calendar

class HomeFragment : Fragment() {
    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!
    private val vm: HomeViewModel by viewModels()
    private lateinit var adapter: MediaAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentHomeBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        b.tvGreeting.text = when { hour < 12 -> "Good Morning ☀️"; hour < 18 -> "Good Afternoon 🎵"; else -> "Good Evening 🌙" }
        adapter = MediaAdapter { (requireActivity() as MainActivity).playMedia(it) }
        b.rvRecent.adapter = adapter
        vm.recentMedia.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            b.tvNoRecent.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        vm.isLoading.observe(viewLifecycleOwner) {
            b.progressHome.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
