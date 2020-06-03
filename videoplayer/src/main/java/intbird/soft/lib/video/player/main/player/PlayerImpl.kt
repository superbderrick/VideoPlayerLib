package intbird.soft.lib.video.player.main.player;

import android.media.MediaPlayer
import android.os.Build
import android.view.Surface
import intbird.soft.lib.video.player.main.player.call.IPlayerCallback
import intbird.soft.lib.video.player.main.player.mode.MediaFileInfo
import intbird.soft.lib.video.player.utils.MediaTimeUtil.adjustValueBoundL

/**
 * created by intbird
 * on 2020/5/1
 * DingTalk id: intbird
 *
 * viewImpl: 暂时没时间view接口,直接用view实现
 */
class PlayerImpl(private val playerCallback: IPlayerCallback?) :
        IPlayer,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener {

    private var mediaPlayer: MediaPlayer? = null
    private var mediaDisplay: Surface? = null
    private var mediaPrepared = false
    private var mediaCompleted = false

    private val playerEnable
        get() = mediaDisplay != null && mediaPrepared

    private var mediaFileInfo: MediaFileInfo =
            MediaFileInfo()

    private var lastVisiblePaying = false

    private fun createMediaPlayer() {
        if (null != mediaPlayer) {
            return
        }
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setScreenOnWhilePlaying(true)
            mediaPlayer?.isLooping = false

            mediaPlayer?.setOnVideoSizeChangedListener(this)
            mediaPlayer?.setOnCompletionListener(this)
            mediaPlayer?.setOnPreparedListener(this)
            start()
        } catch (ignored: Exception) {
        }
    }

    override fun available(display: Surface?) {
        mediaDisplay = display
        createMediaPlayer()
        mediaPlayer?.setSurface(display)
        start()
    }

    override fun prepare(mediaFile: MediaFileInfo) {
        mediaFileInfo = mediaFile
        createMediaPlayer()
        if (mediaPrepared) {
            mediaPrepared = false
            mediaPlayer?.reset()
        }
        try {
            mediaPlayer?.setDataSource(mediaFileInfo.filePath)
            mediaPlayer?.prepareAsync()
        } catch (ignored: Exception) {
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mediaPrepared = true
        playerCallback?.onPrepared(mediaFileInfo)
        start()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (!mediaCompleted) {
            mediaCompleted = true
            mediaPlayer?.seekTo(getTotalTime().toInt())
            playerCallback?.onCompletion(mediaFileInfo)
        }
    }

    override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
        //if (!playerEnable) return // 去掉可用性检查
        mediaFileInfo.width = width
        mediaFileInfo.height = height
        playerCallback?.onVideoSizeChanged(mediaFileInfo)
    }

    override fun start() {
        if (!playerEnable) return
        if (mediaPlayer?.isPlaying == true) return
        if (mediaCompleted) {
            mediaCompleted = false
            mediaPlayer?.seekTo(0)
        }
        mediaPlayer?.start()
        playerCallback?.onStart()
    }

    override fun seekTo(duration: Long, start: Boolean) {
        if (!playerEnable) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaPlayer?.seekTo(duration.toLong(), MediaPlayer.SEEK_CLOSEST)
        } else {
            mediaPlayer?.seekTo(duration.toInt())
        }
        playerCallback?.onSeekTo(duration)
        mediaCompleted = (duration >= getTotalTime())
        if (start && !mediaCompleted) {
            mediaPlayer?.start()
            playerCallback?.onStart()
        }
    }

    override fun resume() {
        if (!playerEnable) return
        seekTo(mediaPlayer?.currentPosition?.toLong() ?: 0, lastVisiblePaying)
    }

    override fun pause() {
        if (!playerEnable) return
        lastVisiblePaying = mediaPlayer?.isPlaying ?: false
        if (!lastVisiblePaying) return
        mediaPlayer?.pause()
        playerCallback?.onPause()
    }

    override fun stop() {
        if (!playerEnable) return
        mediaPlayer?.stop()
        playerCallback?.onStop()
    }

    override fun destroy() {
        mediaPlayer?.release()
    }

    override fun isPlaying(): Boolean {
        return if (playerEnable) mediaPlayer?.isPlaying ?: false else false
    }

    override fun getCurrentTime(): Long {
        return if (playerEnable) adjustValueBoundL(
                (mediaPlayer?.currentPosition?.toLong()?: 0L),
                (mediaPlayer?.duration?.toLong() ?: 0L)
        ) else 0L
    }

    override fun getTotalTime(): Long {
        return if (playerEnable) mediaPlayer?.duration?.toLong() ?: 0L else 0L
    }
}