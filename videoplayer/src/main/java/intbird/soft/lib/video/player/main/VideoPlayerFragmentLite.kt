package intbird.soft.lib.video.player.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import intbird.soft.lib.video.player.R
import intbird.soft.lib.video.player.api.bean.MediaClarity
import intbird.soft.lib.video.player.api.bean.MediaPlayItem
import intbird.soft.lib.video.player.api.style.MediaPlayerStyle
import intbird.soft.lib.video.player.main.controller.control.ControlController
import intbird.soft.lib.video.player.main.controller.control.call.IControlCallback
import intbird.soft.lib.video.player.main.controller.touch.TouchController
import intbird.soft.lib.video.player.main.controller.touch.call.IVideoTouchCallback
import intbird.soft.lib.video.player.main.dialog.ClarityDialogFragment
import intbird.soft.lib.video.player.main.locker.LockController
import intbird.soft.lib.video.player.main.notify.ILockExecute
import intbird.soft.lib.video.player.main.notify.ITouchSystemExecute
import intbird.soft.lib.video.player.main.notify.mode.AdjustInfo
import intbird.soft.lib.video.player.main.player.IPlayer
import intbird.soft.lib.video.player.main.player.call.IPlayerCallback
import intbird.soft.lib.video.player.main.player.call.PlayerCallbacks
import intbird.soft.lib.video.player.main.player.mode.MediaFileInfo
import intbird.soft.lib.video.player.main.player.player.MediaPlayerImpl
import intbird.soft.lib.video.player.utils.MediaFileUtils
import intbird.soft.lib.video.player.utils.MediaLightUtils
import intbird.soft.lib.video.player.utils.MediaLogUtil
import intbird.soft.lib.video.player.utils.MediaScreenUtils
import kotlinx.android.synthetic.main.lib_media_player_control_pop.*
import kotlinx.android.synthetic.main.lib_media_player_control_title.*
import kotlinx.android.synthetic.main.lib_media_player_main.*
import kotlinx.android.synthetic.main.lib_media_player_touch.*
import kotlin.properties.Delegates

/**
 * created by intbird
 * on 2020/9/1
 * DingTalk id: intbird
 *
 * 时间有限
 */

open class VideoPlayerFragmentLite : Fragment(), ILockExecute {

    companion object {
        var EXTRA_FILE_URLS = "videoUrls"
        var EXTRA_FILE_INDEX = "videoIndex"
        var EXTRA_PLAYER_STYLE = "videoStyle"

        fun newInstance(playList: ArrayList<MediaPlayItem>?, playIndex: Int, playerStyle: MediaPlayerStyle): VideoPlayerFragment {
            val fragment = VideoPlayerFragment()
            var args = fragment.arguments
            if (null == args) args = Bundle()
            args.putParcelableArrayList(EXTRA_FILE_URLS, playList)
            args.putInt(EXTRA_FILE_INDEX, playIndex)
            args.putSerializable(EXTRA_PLAYER_STYLE, playerStyle)
            fragment.arguments = args
            return fragment
        }
    }

    class SharedViewModel : ViewModel() {
        val landscape = MutableLiveData<Boolean>()

        val clarityArray = MutableLiveData<ArrayList<MediaClarity>>()
        val clarityArrayChecked = MutableLiveData<MediaClarity>()
    }

    private val permissionRequestCode = 10
    private val permissionSettingsRequestCode = 11
    private var sdcardPermissionsGrand by Delegates.observable(false) { _, _, newValue ->
        if (newValue) {
            play(PlayFlag.SELF)
        }
        log("sdcardPermissionsGrand: $newValue")
    }

    private var videoPlayItems: ArrayList<MediaPlayItem>? = null
    private var videoPlayIndex: Int = 0
    private var videoPlayStyle: MediaPlayerStyle? = null

    protected var states:PlayerCallbacks? = null
    protected var player: IPlayer? = null
    protected var locker: LockController? = null
    private var videoTouchController: TouchController? = null
    private var videoControlController: ControlController? = null

    protected var mediaPlayingItem: MediaPlayItem? = null
    protected var mediaPlayingItemChild: MediaClarity? = null
    private var playingMediaInfo = MediaFileInfo()

    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.lib_media_player_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        videoPlayItems = arguments?.getParcelableArrayList<MediaPlayItem>(EXTRA_FILE_URLS)
        videoPlayIndex = arguments?.getInt(EXTRA_FILE_INDEX) ?: 0
        videoPlayStyle = arguments?.getSerializable(EXTRA_PLAYER_STYLE) as? MediaPlayerStyle
        log("Intent args: videoPlayItems: $videoPlayItems videoPlayIndex: $videoPlayIndex videoPlayStyle: $videoPlayStyle")

        states = PlayerCallbacks(playerCallback)
        player = MediaPlayerImpl(textureView, states)
        //player = ExoPlayerImpl(textureView, playerCallback)
        videoTouchController = TouchController(player, locker, touchCallback, layoutTouchPanel)
        videoControlController = ControlController(player, locker, controlCallback, getViewLayoutControl(videoPlayStyle))

        locker = LockController(ivPopLock)
        locker?.addExecute(videoTouchController)
            ?.addExecute(videoControlController)
            ?.addExecute(this)
        executeLock(false)

        handBackPressed()

        checkPermissionManifest(getInternalActivity(), permissionRequestCode)
        // checkPermissionSettings(this, permissionSettingsRequestCode)
    }

    protected fun setVideoPlayerItems(playList: ArrayList<MediaPlayItem>?, playIndex: Int) {
        this.videoPlayItems = playList
        this.videoPlayIndex = playIndex
    }

    private fun handBackPressed() {
        // reset screen orientation when onBackPressed
        getInternalActivity().onBackPressedDispatcher.addCallback(object :
            OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                log("handleOnBackPressed")
            }
        })

        ivBack?.setOnClickListener {
            log("ivBack ${resources.configuration.orientation}  ${getInternalActivity().requestedOrientation}")
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                getInternalActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                getInternalActivity().finish()
            }
        }
    }

    private fun getViewLayoutControl(viewStyle: MediaPlayerStyle?): View? {
        return when (viewStyle ?: MediaPlayerStyle.SHOW_LAST_NEXT) {
            MediaPlayerStyle.SHOW_LAST_NEXT -> {
                val stub: ViewStub? = view?.findViewById(R.id.viewStub1)
                stub?.inflate()
            }
            MediaPlayerStyle.SHOW_BACKWARD_FORWARD -> {
                val stub: ViewStub? = view?.findViewById(R.id.viewStub2)
                stub?.inflate()
            }
        }
    }

    private fun getInternalActivity(): FragmentActivity {
        return requireActivity()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.clarityArrayChecked.observe(viewLifecycleOwner, Observer<MediaClarity> { mediaClarity ->
            if (null == mediaClarity || !mediaClarity.selectedByUser) return@Observer
            mediaClarity.clarityProgress = player?.getCurrentTime() ?: 0
            play(PlayFlag.SELF)
            log("clarityArrayChecked: $mediaClarity")
        })
    }

    override fun onResume() {
        super.onResume()
        player?.resume()
        log("onResume")
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
        log("onPause")
    }

    protected fun isFinishing(): Boolean {
        return (isRemoving || isDetached || !isAdded)
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.destroy()
        locker?.destroy()
        videoTouchController?.destroy()
        videoControlController?.destroy()
        log("onDestroy")
    }

    override fun executeLock(lock: Boolean) {
        // 方向锁定
        lockScreenOrientation(getInternalActivity(), lock)
        // 是否禁用自动转屏
        if (MediaLightUtils.checkSystemWritePermission(getInternalActivity()))
            Settings.System.putInt(
                getInternalActivity().contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (lock) 0 else 1
            )
    }

    private fun lockScreenOrientation(activity: Activity, lock: Boolean) {
        val display = activity.windowManager.defaultDisplay
        when (display.rotation) {
            // 横屏
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if(lock) {
                    this.getInternalActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                } else {
                    this.getInternalActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if(lock) {
                    this.getInternalActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                } else {
                    this.getInternalActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
            }
        }
    }

    enum class PlayFlag(val value: Int) {
        NEXT(1),
        SELF(0),
        LAST(-1)
    }

    protected fun play(crease: PlayFlag) : Boolean {
        val creasedFile = getCreasedFile(crease.value)
        log("play file: $creasedFile")
        return if (null != creasedFile) {
            videoPlayIndex = videoPlayIndex.plus(crease.value)
            player?.prepare(creasedFile)
            true
        } else {
            states?.onError()
            false
        }
    }

    private fun getCreasedFile(crease: Int): MediaFileInfo? {
        if (null == videoPlayItems) {
            return null
        }
        val newIndex = videoPlayIndex + crease
        if (newIndex < 0) {
            return null
        }
        if (newIndex > videoPlayItems!!.size - 1) {
            return null
        }
        log("ceasedFile index :$newIndex")
        val playItem = videoPlayItems!![newIndex]
        val mediaItem = getPlayerMediaItem(playItem)
        val support = MediaFileUtils.supportFileType(mediaItem.mediaUrl)
        log("support file :$support")
        if (!support) {
            return null
        }
        val mediaFileInfo = MediaFileInfo(
            playItem.mediaId,
            playItem.mediaName,
            mediaItem.mediaUrl,
            mediaItem.mediaHeaders,
            mediaItem.clarityText
        )
        log("MediaFileInfo:$mediaFileInfo")
        return mediaFileInfo
    }

    private fun getPlayerMediaItem(playItem: MediaPlayItem): MediaClarity {
        log("mediaItem: $playItem")

        val clarityConfig = playItem.mediaConfig
        this.viewModel.clarityArray.value = clarityConfig
        this.mediaPlayingItem = playItem
        log("mediaItem-Config: $clarityConfig")

        val clarityConfigItem = getPlayerMediaConfigItem(playItem)
        this.viewModel.clarityArrayChecked.value = clarityConfigItem
        this.mediaPlayingItemChild = clarityConfigItem
        log("mediaItem-Config-Item: $clarityConfigItem")
        return clarityConfigItem
    }

    private fun getPlayerMediaConfigItem(playItem: MediaPlayItem): MediaClarity {
        val checkedMediaClarity = viewModel.clarityArrayChecked.value
        var index = if (null != checkedMediaClarity
            && checkedMediaClarity.selectedByUser
            && checkedMediaClarity.mediaId == playItem.mediaId
        ) {
            checkedMediaClarity.clarityIndex
        } else {
            playItem.defaultSelected ?: 0
        }
        if (index !in 0 until playItem.mediaConfig.size) index = 0
        val configItem = playItem.mediaConfig[index]
        configItem.selectedByUser = false
        configItem.clarityProgress = checkedMediaClarity?.clarityProgress ?: (playItem.defaultProgress ?: 0)
        return configItem
    }

    private fun checkPermissionSettings(activity: Activity, requestCode: Int) {
        val permission = MediaLightUtils.checkSystemWritePermission(activity)
        if (!permission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:" + activity.packageName)
                activity.startActivityForResult(intent, requestCode)
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.WRITE_SETTINGS),
                    requestCode
                )
            }
        }
    }

    private fun checkPermissionManifest(context: Activity, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            sdcardPermissionsGrand = true
        } else {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                requestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.isNotEmpty()) {
                for (index in grantResults.indices) {
                    if ((grantResults[index] == PackageManager.PERMISSION_GRANTED) && (permissions[index] == Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        sdcardPermissionsGrand = true
                    }
                }
            }
        } else if (requestCode == permissionSettingsRequestCode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                var hasPermission = Settings.System.canWrite(getInternalActivity())
            } else if (grantResults.isNotEmpty()) {
                for (index in grantResults.indices) {
                    if (permissions[index] == Manifest.permission.WRITE_SETTINGS) {
                    }
                }
            }
        }
    }

    //----显示器,播放器,触摸和控制功能回调 start----
    private val playerCallback = object : IPlayerCallback {

        override fun onPrepare(mediaFileInfo: MediaFileInfo) {
            contentLoading.show()
        }

        override fun onPrepared(mediaFileInfo: MediaFileInfo) {
            contentLoading.hide()
            tvVideoName.text = mediaFileInfo.mediaName ?: ""
            videoControlController?.onPrepared(mediaFileInfo)

            // 继续播放
            val mediaClarity = viewModel.clarityArrayChecked.value
            if (null != mediaClarity
                && mediaClarity.clarityProgress > 0
                && mediaClarity.mediaId == mediaFileInfo.mediaId
            ) {
                player?.seekTo(mediaClarity.clarityProgress, true)
                mediaClarity.clarityProgress = 0
            }
        }

        override fun onStart() {
            contentLoading.hide()
            videoControlController?.onStart()
            log("player onStart")
        }

        override fun onSeekTo(duration: Long) {
            videoControlController?.onSeekTo(duration)
            log("player onSeekTo$duration")
        }

        override fun onPause() {
            videoControlController?.onPause()
            log("player onPause")
        }

        override fun onCompletion(mediaFileInfo: MediaFileInfo) {
            contentLoading.hide()
            videoControlController?.onCompletion()
            log("player onCompletion:$mediaFileInfo")
        }

        override fun onStop() {
            contentLoading.hide()
            videoControlController?.onStop()
            log("player onStop")
        }

        override fun onError(errorMessage: String?) {
            contentLoading.hide()
            videoControlController?.onStop()
            log("player onError")
        }

        override fun onVideoSizeChanged(mediaFileInfo: MediaFileInfo) {
            playingMediaInfo = mediaFileInfo
            log("player onVideoSizeChanged:$playingMediaInfo")
            setFitToFillAspectRatio(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        }

        override fun onBuffStart() {
            contentLoading.show()
        }

        override fun onBuffEnded() {
            contentLoading.hide()
        }
    }

    private val touchCallback = object : IVideoTouchCallback {
        override fun onSingleTap() {
            videoControlController?.toggleVisible()
        }

        override fun onDoubleTap() {
            if (player?.isPlaying() == true) player?.pause() else player?.start()
            videoControlController?.toggleVisible(player?.isPlaying() == false)
        }

        override fun onBeforeDropSeek() {
            this@VideoPlayerFragmentLite.onBeforeDropSeek(false)
        }

        override fun onDroppingSeek(progress: Long) {
            this@VideoPlayerFragmentLite.onDroppingSeek(progress)
        }

        override fun onAfterDropSeek() {
            this@VideoPlayerFragmentLite.onAfterDropSeek()
        }

        override fun getVolumeInfo(): AdjustInfo {
            return touchImpl.getVolumeInfo()
        }

        override fun changeSystemVolumeImpl(newVolume: Float) {
            touchImpl.changeSystemVolumeImpl(newVolume)
        }

        override fun getBrightnessInfo(): AdjustInfo {
            return touchImpl.getBrightnessInfo()
        }

        override fun changeBrightnessImpl(newBrightness: Float) {
            touchImpl.changeBrightnessImpl(newBrightness)
        }
    }

    private val touchImpl = object : ITouchSystemExecute {

        override fun getVolumeInfo(): AdjustInfo {
            val audioManager =
                getInternalActivity().getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                audioManager?.getStreamMinVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 0f else 0f
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 0f
            val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 0f
            return AdjustInfo(min, max, current)
        }

        override fun changeSystemVolumeImpl(newVolume: Float) {
            val audioManager =
                getInternalActivity().getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                newVolume.toInt(),
                AudioManager.FLAG_VIBRATE
            )
        }

        override fun getBrightnessInfo(): AdjustInfo {
            val current = (MediaLightUtils.getActivityBrightness(getInternalActivity()))
            return AdjustInfo(
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF,
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL,
                current
            )
        }

        override fun changeBrightnessImpl(newBrightness: Float) {
            MediaLightUtils.setAppBrightness(newBrightness, getInternalActivity())
        }
    }

    private val controlCallback = object : IControlCallback {
        override fun clarity(show: Boolean) {
            if (show) {
                ClarityDialogFragment.showDialog(parentFragmentManager)
            } else {
                ClarityDialogFragment.dismissDialog(parentFragmentManager)
            }
            log("clarity: $show")
        }



        override fun backward(long: Long) {
            val progress = (player?.getCurrentTime() ?: 0) - long
            player?.seekTo(progress, player?.isPlaying()?:false)
            log("backward $progress")
        }

        override fun forward(long: Long) {
            val progress = (player?.getCurrentTime() ?: 0) + long
            player?.seekTo(progress, player?.isPlaying()?:false)
            log("forward $progress")
        }

        override fun last(): Boolean {
            return play(PlayFlag.LAST)
        }

        override fun next(): Boolean {
            return play(PlayFlag.NEXT)
        }

        override fun landscape() {
            val currentOrientation = resources.configuration.orientation
            val activity: Activity = getInternalActivity()
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }

            log("landscape")
        }

        override fun portrait() {
            val currentOrientation = resources.configuration.orientation
            val activity: Activity = getInternalActivity()
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }

            log("portrait")
        }

        override fun onBeforeDropSeek() {
            this@VideoPlayerFragmentLite.onBeforeDropSeek(true)
        }

        override fun onDroppingSeek(progress: Long) {
            this@VideoPlayerFragmentLite.onDroppingSeek(progress)
        }

        override fun onAfterDropSeek() {
            this@VideoPlayerFragmentLite.onAfterDropSeek()
        }
    }
    //----end----

    //---- 横竖屏处理 start---
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        log("onConfigurationChanged: orientation: ${newConfig.orientation}")
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewModel.landscape.value = true
            setFitToFillAspectRatio(true)
            videoControlController?.onLandscape()
            videoTouchController?.onLandscape()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            viewModel.landscape.value = false
            setFitToFillAspectRatio(false)
            videoControlController?.onPortrait()
            videoTouchController?.onPortrait()
        }
    }

    private val rootViewWidth by lazy {
        view?.width ?: MediaScreenUtils.getScreenWidth(
            getInternalActivity()
        )
    }
    private val rootViewHeight by lazy {
        view?.height ?: MediaScreenUtils.getScreenHeight(
            getInternalActivity()
        )
    }

    private fun setFitToFillAspectRatio(landscape: Boolean) {
        setFitToFillAspectRatio(
            landscape,
            rootViewWidth,
            rootViewHeight,
            playingMediaInfo.width,
            playingMediaInfo.height
        )
    }

    private fun setFitToFillAspectRatio(
        landscape: Boolean,
        viewWidth: Int,
        viewHeight: Int,
        videoWidth: Int,
        videoHeight: Int
    ) {
        val lp = textureView.layoutParams
        val rate = videoWidth.toDouble() / videoHeight.toDouble()
        if (landscape) {
            lp.height = viewWidth
            lp.width = (viewWidth.toFloat() * rate).toInt()
        } else {
            lp.width = viewWidth
            lp.height = (viewWidth.toFloat() / rate).toInt()
        }
        log("viewWidth: $viewWidth viewHeight:$viewHeight videoWidth:$videoWidth videoHeight:$videoHeight  lpW:${lp.width} lpH:${lp.height}")
        textureView.layoutParams = lp
    }

    //---- end ---

    //---- 进度条拖动 start ----
    private var targetProgress = 0L
    private var playingState = false

    fun onBeforeDropSeek(visible: Boolean) {
        targetProgress = 0L
        playingState = player?.isPlaying() ?: false
        if (playingState) {
            player?.pause()
        }
        videoControlController?.toggleVisible(
            visible,
            autoDismiss = false,
            animation = true
        )
    }

    fun onDroppingSeek(progress: Long) {
        //player?.seekTo(progress)
        targetProgress = progress
    }

    fun onAfterDropSeek() {
        player?.seekTo(targetProgress, playingState)
        videoControlController?.toggleVisible(true)
    }
    //---- end ----

    protected fun log(message: String) {
        MediaLogUtil.log("VideoFragment  $message")
    }


}