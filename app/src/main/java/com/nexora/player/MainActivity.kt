package com.nexora.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.nexora.player.databinding.ActivityMainBinding
import com.nexora.player.models.MediaItem

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled silently; fragments observe VM */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNavigation.setupWithNavController(navController)

        requestMediaPermissions()
    }

    private fun requestMediaPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
        else
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (perms.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED })
            permissionLauncher.launch(perms)
    }

    fun playMedia(item: MediaItem) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URI,    item.uri.toString())
            putExtra(PlayerActivity.EXTRA_TITLE,  item.title)
            putExtra(PlayerActivity.EXTRA_ARTIST, item.artist)
            putExtra(PlayerActivity.EXTRA_IS_VIDEO, item.albumArtUri == null)
        })
    }

    fun showMiniPlayer(visible: Boolean) {
        binding.miniPlayerContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
