package com.nexora.player.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.nexora.player.BuildConfig
import com.nexora.player.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSettingsBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.tvVersion.text = "Version ${BuildConfig.VERSION_NAME}"
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
