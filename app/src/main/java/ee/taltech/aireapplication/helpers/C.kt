package ee.taltech.aireapplication.helpers

import ee.taltech.aireapplication.App
import ee.taltech.aireapplication.BuildConfig

object C {
    private const val _PREFIX = "ee.taltech.temi."

    const val EXTRA_WEBVIEW_LAUNCH_URL = _PREFIX + "WEBVIEW_LAUNCH_URL"
    const val EXTRA_WEBVIEW_SHOW_INPUT = _PREFIX + "WEBVIEW_SHOW_INPUT"


    const val EXTRA_GAMEVIEW_SETTINGS = _PREFIX + "GAMEVIEW_SETTINGS"

    const val INTENT_TIMER_SERVICE_TICK = _PREFIX + "INTENT_TIMER_SERVICE_TICK"

    const val INTENT_TTS_STATUS_CHANGE = _PREFIX + "INTENT_TTS_STATUS_CHANGE"

    const val INTENT_ASR = _PREFIX + "INTENT_ASR"
    const val INTENT_ASR_TEXT = _PREFIX + "INTENT_ASR_TEXT"


    private const val TEMI_BACKEND_URL_DEV = "http://10.0.2.2:5189/"
    private const val TEMI_BACKEND_URL_ON_TEMI = "https://temi.akaver.com/"

    const val TEMI_BACKEND_API_KEY = BuildConfig.TEMI_BACKEND_API_KEY

    val URL_EVENTLOG = getApiPrefix() + "api/v1/LogEvents/"
    val URL_LOCATIONS_SYNC = getApiPrefix() + "api/v1/MapLocations/"
    val URL_LOCATIONS2_SYNC = getApiPrefix() + "api/v2/MapLocations/"
    val URL_NEWS = getApiPrefix() + "api/v1/News/"
    val URL_WEBLINKS = getApiPrefix() + "api/v1/WebLinks/"
    val URL_ARTICLES = getApiPrefix() + "api/v1/articles/"
    val URL_APPVERSION = getApiPrefix() + "api/v1/AppVersions/"
    val URL_IFRAME_VIEW = getApiPrefix() + "Home/ViewIframe/"
    const val NAVIGATION_WAIT_TIME = 5
    const val EVENTLOG_SESSION_IDLE_SECONDS = 30

    private fun getApiPrefix(): String {
        return if (App.IS_REAL_TEMI) TEMI_BACKEND_URL_ON_TEMI else TEMI_BACKEND_URL_DEV
    }
}