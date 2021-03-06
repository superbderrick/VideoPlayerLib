package net.intbird.soft.app

import android.content.res.Configuration
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import net.intbird.soft.app.MainActivityData.itemTestArray3
import net.intbird.soft.app.MainActivityData.itemTestArrayModel
import net.intbird.soft.app.MainActivityData.itemTestArrayString
import net.intbird.soft.app.MainActivityData.itemTestIndex
import net.intbird.soft.lib.service.loader.ServicesLoader
import net.intbird.soft.lib.video.player.api.IVideoPlayer
import net.intbird.soft.lib.video.player.api.bean.MediaRate
import net.intbird.soft.lib.video.player.main.VideoPlayerFragment
import net.intbird.soft.lib.video.player.main.view.typedui.MediaPlayerType


class MainActivity : AppCompatActivity() {

    private var videoPlayerFragment: VideoPlayerFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        info.movementMethod = ScrollingMovementMethod()

        // use as a fragment
        add1.setOnClickListener { addVideoPlayer(R.id.fragment_player, MediaPlayerType.PLAYER_STYLE_1) }
        add2.setOnClickListener { addVideoPlayer(R.id.fragment_player, MediaPlayerType.PLAYER_STYLE_2) }
        add3.setOnClickListener { addVideoPlayer(R.id.fragment_player, MediaPlayerType.PLAYER_STYLE_3) }
        add4.setOnClickListener { addVideoPlayer(R.id.fragment_player, MediaPlayerType.PLAYER_STYLE_4) }
        remove.setOnClickListener { removeAudioPlayer(R.id.fragment_player) }

        reset.setOnClickListener {
            videoPlayerFragment?.setVideoPlayerList(itemTestArray3, itemTestIndex,true)
        }
        mdf1.setOnClickListener {
            val playingItem = videoPlayerFragment?.getVideoPlayerStateInfo()?.getVideoPlayingItem() ?: return@setOnClickListener
            playingItem.mediaName = "new media title"
            videoPlayerFragment?.setVideoPlayerItem(playingItem)
        }
        mdf2.setOnClickListener {
            val playingItemInfo = videoPlayerFragment?.getVideoPlayerStateInfo()?.getVideoPlayingItemInfo() ?: return@setOnClickListener
            playingItemInfo.mediaRate = MediaRate("2x", 2.0f)
            videoPlayerFragment?.setVideoPlayerItemInfo(playingItemInfo)
        }

        last.setOnClickListener { videoPlayerFragment?.getVideoPlayerController()?.last() }
        pause.setOnClickListener { videoPlayerFragment?.getVideoPlayerController()?.pause() }
        next.setOnClickListener { videoPlayerFragment?.getVideoPlayerController()?.next() }
        info.setOnClickListener { stateText.text = "info:${videoPlayerFragment?.getVideoPlayerStateInfo()?.getVideoPlayingItemInfo()}" }

        // full screen activity
        fullScreen1.setOnClickListener {
            ServicesLoader.load(IVideoPlayer::class.java)?.startActivity(this, itemTestArrayModel, itemTestIndex, autoPlay = true)
        }

        // full screen activity
        fullScreen2.setOnClickListener {
            ServicesLoader.load(IVideoPlayer::class.java)?.startActivity(this, itemTestArrayString, itemTestIndex,autoPlay = true)
        }
    }

    private fun addVideoPlayer(rid: Int, type: MediaPlayerType) {
        removeAudioPlayer(rid)
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val fragment = VideoPlayerFragment.newInstance(itemTestArrayModel, itemTestIndex, type, autoPlay = true)
        fragmentTransaction.add(rid, fragment)
        fragmentTransaction.commit()

        setVideoCallback(fragment)
    }

    private fun setVideoCallback(fragment: VideoPlayerFragment) {
        this.videoPlayerFragment = fragment
        this.videoPlayerFragment?.setPlayerStateCallback(
            MainActivityData.ReceivePlayerCallback(
                this,
                stateText,
                fragment
            )
        )
    }

    private fun removeAudioPlayer(rid: Int) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentManager.findFragmentById(rid)?.let { fragmentTransaction.remove(it) }
        fragmentTransaction.commit()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            fragment_player.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            fragment_player.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            fragment_player.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            fragment_player.layoutParams.height = fragment_player.context.resources.getDimension(R.dimen.lib_media_playerVideoPlayerHeight).toInt()
        }
    }
}
