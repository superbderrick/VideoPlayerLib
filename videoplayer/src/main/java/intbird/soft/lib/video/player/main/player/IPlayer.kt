package intbird.soft.lib.video.player.main.player;

import intbird.soft.lib.video.player.main.player.intent.call.IParamsChange
import intbird.soft.lib.video.player.main.player.intent.call.IParamsStateInfo
import intbird.soft.lib.video.player.main.player.mode.MediaFileInfo

/**
 * created by intbird
 * on 2020/5/1
 * DingTalk id: intbird
 */
interface IPlayer : IParamsStateInfo, IParamsChange {

    fun prepare(mediaFileInfo: MediaFileInfo)

    fun start()

    fun seekTo(duration: Long, autoPlay: Boolean)

    fun resume()

    fun pause()

    fun last(): Boolean

    fun next(): Boolean

    fun stop()

    fun destroy()
}
