package intbird.soft.lib.video.player.main.player.intent.call

import intbird.soft.lib.video.player.main.player.mode.MediaFileInfo

/**
 * created by intbird
 * on 2020/9/10
 * DingTalk id: intbird
 **/
interface IParamsChange {
    fun onParamsChange(mediaFileInfo: MediaFileInfo?)
}