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
 * 버튼을 누르면 앱을 열고, **누른 프리셋 초를 intent extra([EXTRA_PRESET_SECONDS])로 실어 보내**
 * setup 화면이 그 값으로 초기화되게 한다(Phase B).
 *
 * ## TileService 동작 방식 (Activity/Service와 다름)
 * 타일은 우리가 직접 화면을 그리는 게 아니라, **시스템(워치의 타일 호스트)이 필요할 때
 * 우리에게 "내용 내놔"라고 물어보는** 콜백 모델이다. 우리는 화면을 갱신하지 않고, 물어볼 때마다
 * "이렇게 그려라"라는 **선언(레이아웃 명세)** 을 돌려줄 뿐 — 실제 렌더는 시스템이 한다.
 * 시스템이 부르는 두 콜백만 구현하면 된다:
 * - [onTileRequest] : "이 타일의 내용(레이아웃)이 뭐냐" → 레이아웃 명세를 담은 [Tile] 반환
 * - [onTileResourcesRequest] : "그 레이아웃이 참조하는 이미지 리소스를 다오" → 리소스 묶음 반환
 *
 * 두 콜백 다 [ListenableFuture]를 반환한다 — 타일 API가 **비동기**라 무거운 준비(IO 등)를 백그라운드에서
 * 해도 되게 열어둔 것. 우리는 즉석에서 만들 수 있으니 [Futures.immediateFuture]로 "이미 완료된" 결과를 준다.
 */
class SteakTileService : TileService() {

    /**
     * 시스템이 타일을 보여주거나 갱신할 때 호출 — **레이아웃 명세**를 반환한다.
     *
     * [Tile]은 시간축([Timeline]) 위의 레이아웃 모음이다(시간대별로 다른 화면을 줄 수도 있음).
     * 우리는 정적이라 [Timeline.fromLayoutElement]로 **레이아웃 하나짜리 타임라인**만 준다.
     * [setResourcesVersion]으로 "지금 참조하는 리소스 버전"을 알려, 이 값이 바뀔 때만
     * 시스템이 [onTileResourcesRequest]를 다시 부른다.
     *
     * [requestParams].deviceConfiguration = 화면 크기/모양 등 기기 정보 → 레이아웃 빌더에 넘겨 워치에 맞춤.
     */
    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<Tile> {
        val tile = Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(Timeline.fromLayoutElement(tileLayout(requestParams.deviceConfiguration)))
            .build()
        return Futures.immediateFuture(tile)
    }

    /**
     * 시스템이 레이아웃에 쓰인 **이미지 리소스**(drawable 등)를 요청할 때 호출.
     * 우리 타일은 텍스트뿐이라 넘길 이미지가 없어서 **버전만 붙은 빈 묶음**을 준다.
     * (반환 버전은 [onTileRequest]의 [RESOURCES_VERSION]과 같아야 시스템이 캐시를 맞게 다룬다.)
     */
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
        Button.Builder(this, launchSetupClickable(seconds))
            .setTextContent(seconds.toString())
            .build()

    /** 앱을 열되, 누른 프리셋 초를 intent extra로 실어 보내 setup을 그 값으로 초기화하게 한다. */
    private fun launchSetupClickable(seconds: Int): Clickable =
        Clickable.Builder()
            .setId("preset_$seconds")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(APP_PACKAGE)
                            .setClassName(MAIN_ACTIVITY)
                            .addKeyToExtraMapping(
                                EXTRA_PRESET_SECONDS,
                                ActionBuilders.AndroidIntExtra.Builder().setValue(seconds).build(),
                            )
                            .build(),
                    )
                    .build(),
            )
            .build()

    private companion object {
        // 리소스 묶음의 버전 태그. 이미지가 바뀌면 이 값을 올려 시스템이 리소스를 다시 받게 한다.
        // 정적 텍스트 타일이라 바뀔 리소스가 없어 "1" 고정.
        const val RESOURCES_VERSION = "1"
        const val TILE_NAME = "스테이크 타이머"
        val PRESET_SECONDS = listOf(10, 20, 30, 60)
        const val COLOR_ON_SURFACE = 0xFFFFFFFF.toInt()

        // :app 클래스 컴파일 의존을 피하려 패키지+클래스명 문자열로 지정.
        const val APP_PACKAGE = "com.seriouschoi.steaktimer"
        const val MAIN_ACTIVITY = "com.seriouschoi.steaktimer.presentation.MainActivity"

        // 프리셋 초를 전달하는 intent extra 키. MainActivity의 같은 이름 상수와 문자열이 일치해야 함.
        const val EXTRA_PRESET_SECONDS = "preset_seconds"
    }
}
