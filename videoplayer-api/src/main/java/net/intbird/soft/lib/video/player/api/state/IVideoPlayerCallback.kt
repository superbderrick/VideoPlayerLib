package net.intbird.soft.lib.video.player.api.state

import androidx.fragment.app.Fragment

/**
 * created by intbird
 * on 2020/5/1
 * DingTalk id: intbird
 */
interface IVideoPlayerCallback {
    fun onCreated(fragment: Fragment)

    fun onPrepare()

    fun onPrepared()

    fun onStart()

    fun onSeekTo(progress: Long?)

    fun onPause(progress: Long?)

    fun onCompletion()

    fun onStop()

    fun onError(errorCode: Int, errorMessage: String? = "")

    fun onBuffStart()

    fun onBuffEnded()
}