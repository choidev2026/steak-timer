package com.seriouschoi.steaktimer.feature.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.MultiButtonLayout
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * 스테이크 타이머 타일 — 워치페이스에서 스와이프로 여는 **정적 런처**.
 *
 * 상단에 앱 이름, 그 아래 프리셋 버튼(10/20/30/60초). 버튼을 누르면 앱으로 진입한다.
 * 타일은 살아있는 세션 상태를 표시하지 않으므로(단순 스테이크 타이머), 세션/Compose/Hilt 의존이 없고
 * 레이아웃도 완전 정적이다 — 상태 변화에 따른 갱신(requestUpdate)이 필요 없다.
 *
 * Phase A: 버튼은 프리셋 구분 없이 앱만 실행한다.
 * Phase B(예정): preset 초를 intent extra로 실어 보내 setup 화면을 그 값으로 초기화한다.
 */
class SteakTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<Tile> {
        val tile = Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(Timeline.fromLayoutElement(tileLayout(requestParams.deviceConfiguration)))
            .build()
        return Futures.immediateFuture(tile)
    }

    // 정적 타일이라 리소스도 버전만 있는 빈 묶음.
    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<Resources> =
        Futures.immediateFuture(Resources.Builder().setVersion(RESOURCES_VERSION).build())

    private fun tileLayout(device: DeviceParameters): LayoutElement =
        PrimaryLayout.Builder(device)
            .setPrimaryLabelTextContent(
                Text.Builder(this, TILE_NAME)
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(argb(COLOR_ON_SURFACE))
                    .build(),
            )
            .setContent(
                MultiButtonLayout.Builder()
                    .apply { PRESET_SECONDS.forEach { addButtonContent(presetButton(it)) } }
                    .build(),
            )
            .build()

    private fun presetButton(seconds: Int): Button =
        Button.Builder(this, launchAppClickable(seconds))
            .setTextContent(seconds.toString())
            .build()

    /** Phase A: 프리셋 구분 없이 앱만 실행. Phase B에서 preset extra + setup 라우팅 추가. */
    private fun launchAppClickable(seconds: Int): Clickable =
        Clickable.Builder()
            .setId("preset_$seconds")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(APP_PACKAGE)
                            .setClassName(MAIN_ACTIVITY)
                            .build(),
                    )
                    .build(),
            )
            .build()

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val TILE_NAME = "스테이크 타이머"
        val PRESET_SECONDS = listOf(10, 20, 30, 60)
        const val COLOR_ON_SURFACE = 0xFFFFFFFF.toInt()

        // :app 클래스 컴파일 의존을 피하려 패키지+클래스명 문자열로 지정.
        const val APP_PACKAGE = "com.seriouschoi.steaktimer"
        const val MAIN_ACTIVITY = "com.seriouschoi.steaktimer.presentation.MainActivity"
    }
}
