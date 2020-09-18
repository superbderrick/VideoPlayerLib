package intbird.soft.lib.video.player.main.view

import intbird.soft.lib.video.player.R

/**
 * created by intbird
 * on 2020/9/10
 * DingTalk id: intbird
 */
enum class MediaPlayerType(
    open val layoutDisplay: Int,
    open val viewDisplay: Int,
    open val layoutControl: Int,
    open val viewControl: Int
) {
    PLAYER_STYLE_1(
        R.layout.lib_media_player_diaplay_texture,
        R.id.textureView,
        R.layout.lib_media_player_control_style_1,
        R.id.mediaRootControl
    ),
    PLAYER_STYLE_2(
        R.layout.lib_media_player_diaplay_texture,
        R.id.textureView,
        R.layout.lib_media_player_control_style_2,
        R.id.mediaRootControl
    ),
    PLAYER_STYLE_3(
        R.layout.lib_media_player_diaplay_texture,
        R.id.textureView,
        R.layout.lib_media_player_control_style_2,
        R.id.mediaRootControl
    )
}