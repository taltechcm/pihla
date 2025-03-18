package ee.taltech.aireapplication.helpers

import android.util.Log
import com.robotemi.sdk.map.Floor
import ee.taltech.aireapplication.App
import ee.taltech.aireapplication.dto.AppVersion
import ee.taltech.aireapplication.dto.Article
import ee.taltech.aireapplication.dto.EventLog
import ee.taltech.aireapplication.dto.Map2Sync
import ee.taltech.aireapplication.dto.MapLocation2Sync
import ee.taltech.aireapplication.dto.MapLocationSync
import ee.taltech.aireapplication.dto.MapSync
import ee.taltech.aireapplication.dto.NewsItem
import ee.taltech.aireapplication.dto.WebLink
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.time.Duration.Companion.minutes

object BackendApiKtorSingleton {
    private const val TAG = "BackendApiKtorSingleton"

    private val client = HttpClient(OkHttp) {
        install(Resources)
        install(HttpTimeout)
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        // enable logging for debugging
        /*
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        */

    }

    suspend fun logEvent(
        tag: String,
        message: String? = null,
        intValue: Int? = null,
        doubleValue: Double? = null
    ) {
        val url = C.URL_EVENTLOG + "?apikey=" + C.TEMI_BACKEND_API_KEY

        try {
            val response: HttpResponse = client.post(url) {
                setBody(
                    EventLog(
                        androidIdCode = App.ANDROID_ID,
                        appLaunch = App.APP_LAUNCH_ID,
                        mapIdCode = App.MAP_ID,
                        mapName = App.MAP_NAME,
                        appName = App.APP_NAME,
                        tag = tag,
                        message = message,
                        intValue = intValue,
                        doubleValue = doubleValue
                    )
                )
            }
            if (!response.status.isSuccess()) {
                Log.e(TAG, "logEvent response: " + response.bodyAsText())
            } else {
                Log.d(TAG, "logEvent response: " + response.bodyAsText())
            }
        } catch (cause: Throwable) {
            Log.e(TAG, cause.toString())
        }
    }


    suspend fun syncMapLocationsToBackend2(
        mapIdCode: String,
        mapName: String,
        floors: List<Floor>,
    ): HttpResponse {
        val url = C.URL_LOCATIONS2_SYNC + "?apikey=" + C.TEMI_BACKEND_API_KEY


        val response: HttpResponse = client.post(url) {
            setBody(
                Map2Sync(
                    mapIdCode = mapIdCode,
                    mapName = mapName,
                    mapFloors = floors.map { f ->
                        Map2Sync.FloorSync(
                            floorName = f.name,
                            mapLocations = f.locations.map { l -> l.name }
                        )
                    }
                )
            )

        }
        if (!response.status.isSuccess()) {
            Log.e(TAG, "syncMapLocationsToBackend2 response: " + response.bodyAsText())
        } else {
            Log.d(TAG, "syncMapLocationsToBackend2 response: " + response.bodyAsText())
        }

        return response
    }


    suspend fun syncMapLocationsToBackend(
        mapIdCode: String,
        mapName: String,
        mapLocations: List<String>
    ): HttpResponse {
        val url = C.URL_LOCATIONS_SYNC + "?apikey=" + C.TEMI_BACKEND_API_KEY


        val response: HttpResponse = client.post(url) {
            setBody(
                MapSync(
                    mapIdCode = mapIdCode,
                    mapName = mapName,
                    mapLocations = mapLocations
                )
            )

        }
        if (!response.status.isSuccess()) {
            Log.e(TAG, "syncMapLocationsToBackend response: " + response.bodyAsText())
        } else {
            Log.d(TAG, "syncMapLocationsToBackend response: " + response.bodyAsText())
        }

        return response
    }


    suspend fun syncMapLocationsFromBackend(): List<MapLocationSync> {
        val url = C.URL_LOCATIONS_SYNC + App.MAP_ID + "/" + "?apikey=" + C.TEMI_BACKEND_API_KEY
        val response = client.get(url)

        if (!response.status.isSuccess()) {
            Log.e(TAG, "syncMapLocationsFromBackend response: " + response.bodyAsText())
            return ArrayList()
        } else {
            Log.d(TAG, "syncMapLocationsFromBackend response: " + response.bodyAsText())

        }

        val data: List<MapLocationSync> = response.body()

        return data
    }

    suspend fun syncMapLocationsFromBackend2(): List<MapLocation2Sync> {
        val url = C.URL_LOCATIONS2_SYNC + App.MAP_ID + "/" + "?apikey=" + C.TEMI_BACKEND_API_KEY
        val response = client.get(url)

        if (!response.status.isSuccess()) {
            Log.e(TAG, "syncMapLocationsFromBackend response: " + response.bodyAsText())
            return ArrayList()
        } else {
            Log.d(TAG, "syncMapLocationsFromBackend response: " + response.bodyAsText())

        }

        val data: List<MapLocation2Sync> = response.body()

        return data
    }


    suspend fun getArticle(articleTitle: String, date: Date?): Article? {
        var dateStr: String? = null
        if (date != null) {
            val df: DateFormat = SimpleDateFormat("yyyy-MM-dd")
            dateStr = df.format(date)
        }

        val url = C.URL_ARTICLES + "?apikey=" + C.TEMI_BACKEND_API_KEY +
                "&articleTitle=" + articleTitle +
                "&androidIdCode=" + App.ANDROID_ID +
                "&mapIdCode=" + App.MAP_ID +
                "&mapName=" + App.MAP_NAME +
                "&appName=" + App.APP_NAME +
                if (dateStr != null) "&date=$dateStr" else ""

        val response = client.get(url)

        if (!response.status.isSuccess()) {
            Log.e(TAG, "getArticle url: " + response.request.url.toString())
            Log.e(TAG, "getArticle response: " + response.bodyAsText())
            return null
        } else {
            Log.d(TAG, "getArticle response: " + response.bodyAsText())
        }

        val data: Article = response.body()

        return data
    }


    suspend fun getNews(): List<NewsItem> {
        val url = C.URL_NEWS + "?apikey=" + C.TEMI_BACKEND_API_KEY
        val response = client.get(url)

        if (!response.status.isSuccess()) {
            Log.e(TAG, "syncMapLocationsFromBackend response: " + response.bodyAsText())
            return ArrayList()
        } else {
            Log.d(TAG, "syncMapLocationsFromBackend response: " + response.bodyAsText())

        }

        val data: List<NewsItem> = response.body()

        return data
    }


    suspend fun downloadFileWithProgress(
        url: String,
        outputStream: OutputStream,
        onProgress: (Long, Long?) -> Unit
    ) {
        client.prepareGet(
            urlString = url,
            block = {
                val timeout = 30.minutes.inWholeMilliseconds
                timeout {
                    requestTimeoutMillis = timeout
                    connectTimeoutMillis = timeout
                    socketTimeoutMillis = timeout
                }
                onDownload { bytesSentTotal, contentLength ->
                    onProgress(bytesSentTotal, contentLength)
                }
            }
        ).execute { response ->
            if (response.status.value in 200..299) {
                val byteReadChannel = response.bodyAsChannel()
                byteReadChannel.copyTo(outputStream)
            } else {
                Log.e("App", "Failed to download file. HTTP Status: ${response.status.value}")
            }
        }
    }


    suspend fun getLatestAppVersion(): AppVersion? {
        val url = C.URL_APPVERSION + "?apikey=" + C.TEMI_BACKEND_API_KEY +
                "&appName=" + App.APP_NAME

        val response = client.get(url)
        if (!response.status.isSuccess()) {
            Log.e(
                TAG,
                "Url $url give status ${response.status.description}. body: ${response.bodyAsText()}"
            )
            return null
        }
        val appVersion: AppVersion = response.body()
        Log.d(TAG, appVersion.toString())

        return appVersion
    }


    suspend fun getWebLinks(category: String): ArrayList<WebLink> {
        val url = C.URL_WEBLINKS + category + "?apikey=" + C.TEMI_BACKEND_API_KEY +
                "&androidIdCode=" + App.ANDROID_ID +
                "&mapIdCode=" + App.MAP_ID +
                "&mapName=" + App.MAP_NAME +
                "&appName=" + App.APP_NAME

        val response = client.get(url)

        if (!response.status.isSuccess()) {
            Log.e(TAG, "getWebLinks response: " + response.bodyAsText() + "\n" + url)
            return ArrayList()
        } else {
            Log.d(TAG, "getWebLinks response: " + response.bodyAsText() + "\n" + url)

        }

        val data: ArrayList<WebLink> = response.body()

        return data
    }

}