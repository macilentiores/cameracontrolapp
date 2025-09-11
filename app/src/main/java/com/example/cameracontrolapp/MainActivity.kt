package com.example.cameracontrolapp

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // Camera configurations
    private val camWestIp = "192.168.88.3"
    private val camEastIp = "192.168.88.2"
    private val user = "admin"
    private val pass = "oneroom"

    // MJPEG stream URLs with embedded credentials
    private val westStreamUrl = "http://$user:$pass@$camWestIp/axis-cgi/mjpg/video.cgi"
    private val eastStreamUrl = "http://$user:$pass@$camEastIp/axis-cgi/mjpg/video.cgi"

    // Query URLs for presets
    private val westQueryUrl = "http://$user:$pass@$camWestIp/axis-cgi/com/ptz.cgi?query=presetposall"
    private val eastQueryUrl = "http://$user:$pass@$camEastIp/axis-cgi/com/ptz.cgi?query=presetposall"

    // Preset URLs (will be dynamically updated after query OR set to defaults)
    private var westChoirUrl = ""
    private var westPulpitUrl = ""
    private var westHomeUrl = ""
    private var westPanoramaUrl = ""
    private var westPreset5Url = ""
    private var westPreset6Url = ""
    private var westPreset7Url = ""

    private var eastChoirUrl = ""
    private var eastPulpitUrl = ""
    private var eastHomeUrl = ""
    private var eastPanoramaUrl = ""
    private var eastPreset5Url = ""
    private var eastPreset6Url = ""
    private var eastPreset7Url = ""

    // Maps for parsed presets (number to name)
    private val westPresets: MutableMap<Int, String> = mutableMapOf()
    private val eastPresets: MutableMap<Int, String> = mutableMapOf()

    // UI elements
    private lateinit var tvUrlWest: TextView
    private lateinit var tvUrlEast: TextView
    private lateinit var webViewWest: WebView
    private lateinit var webViewEast: WebView

    // TextViews for displaying active preset names
    private lateinit var tvActiveWestPresetDisplay: TextView
    private lateinit var tvActiveEastPresetDisplay: TextView

    // Buttons
    private lateinit var btnWestChoir: Button
    private lateinit var btnWestPulpit: Button
    private lateinit var btnWestHome: Button
    private lateinit var btnWestPanorama: Button
    private lateinit var btnWestPreset5: Button
    private lateinit var btnWestPreset6: Button
    private lateinit var btnWestPreset7: Button
    private lateinit var btnWestRefresh: Button

    private lateinit var btnEastChoir: Button
    private lateinit var btnEastPulpit: Button
    private lateinit var btnEastHome: Button
    private lateinit var btnEastPanorama: Button
    private lateinit var btnEastPreset5: Button
    private lateinit var btnEastPreset6: Button
    private lateinit var btnEastPreset7: Button
    private lateinit var btnEastRefresh: Button
    private lateinit var btnOptions: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        tvUrlWest = findViewById(R.id.tv_url_west)
        tvUrlEast = findViewById(R.id.tv_url_east)
        tvUrlWest.text = getString(R.string.camera_not_selected)
        tvUrlEast.text = getString(R.string.camera_not_selected)

        webViewWest = findViewById(R.id.webview_west)
        webViewEast = findViewById(R.id.webview_east)

        setupWebView(webViewWest, westStreamUrl)
        setupWebView(webViewEast, eastStreamUrl)

        tvActiveWestPresetDisplay = findViewById(R.id.tv_active_west_preset_display)
        tvActiveEastPresetDisplay = findViewById(R.id.tv_active_east_preset_display)
        tvActiveWestPresetDisplay.text = getString(R.string.camera_not_selected)
        tvActiveEastPresetDisplay.text = getString(R.string.camera_not_selected)

        btnWestChoir = findViewById(R.id.btn_west_choir)
        btnWestPulpit = findViewById(R.id.btn_west_pulpit)
        btnWestHome = findViewById(R.id.btn_west_home)
        btnWestPanorama = findViewById(R.id.btn_west_panorama)
        btnWestPreset5 = findViewById(R.id.btn_west_preset5)
        btnWestPreset6 = findViewById(R.id.btn_west_preset6)
        btnWestPreset7 = findViewById(R.id.btn_west_preset7)
        btnWestRefresh = findViewById(R.id.btn_west_refresh)

        btnEastChoir = findViewById(R.id.btn_east_choir)
        btnEastPulpit = findViewById(R.id.btn_east_pulpit)
        btnEastHome = findViewById(R.id.btn_east_home)
        btnEastPanorama = findViewById(R.id.btn_east_panorama)
        btnEastPreset5 = findViewById(R.id.btn_east_preset5)
        btnEastPreset6 = findViewById(R.id.btn_east_preset6)
        btnEastPreset7 = findViewById(R.id.btn_east_preset7)
        btnEastRefresh = findViewById(R.id.btn_east_refresh)

        btnOptions = findViewById(R.id.btn_options)
        btnOptions.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        updateButtonsAndUrls(emptyMap(), camWestIp)
        updateButtonsAndUrls(emptyMap(), camEastIp)

        queryPresets {
            setupButtonListeners()
        }
    }

    private fun setupWebView(webView: WebView, url: String) {
        webView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            domStorageEnabled = true
            // Disable pinch-to-zoom and related controls
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }
        webView.loadUrl(url)
    }

    private fun queryPresets(onComplete: () -> Unit) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val westResponse = fetchQuery(westQueryUrl)
            parsePresets(westResponse, westPresets, camWestIp)
            val eastResponse = fetchQuery(eastQueryUrl)
            parsePresets(eastResponse, eastPresets, camEastIp)

            runOnUiThread {
                if (westPresets.isEmpty() && eastPresets.isEmpty()) {
                    Toast.makeText(this, "Failed to sync presets\u2014using defaults", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Presets synced successfully", Toast.LENGTH_SHORT).show()
                }
                onComplete()
            }
        }
    }

    private fun fetchQuery(urlString: String): String {
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.connectTimeout = 5000
            urlConnection.readTimeout = 5000
            val responseCode = urlConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return urlConnection.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.e("PRESETS", "Query failed: Code $responseCode for URL: $urlString")
                urlConnection.errorStream?.bufferedReader()?.use { Log.e("PRESETS", "Error body for $urlString: ${it.readText()}") }
                return ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PRESETS", "Query error: ${e.message} for URL: $urlString")
            return ""
        } finally {
            urlConnection?.disconnect()
        }
    }

    private fun parsePresets(response: String, presetsMap: MutableMap<Int, String>, camIp: String) {
        presetsMap.clear()
        if (response.isNotBlank()) {
            Log.d("PRESETS", "Response for $camIp: $response")
            response.lines().forEach { line ->
                if (line.startsWith("presetposno")) {
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        val numString = parts[0].substringAfter("presetposno")
                        val num = numString.toIntOrNull()
                        if (num != null) {
                            val name = parts[1].trim()
                            presetsMap[num] = name
                            Log.d("PRESETS", "Parsed for $camIp: Preset $num = '$name'")
                        } else {
                            Log.w("PRESETS", "Failed to parse preset number from: ${parts[0]} for $camIp")
                        }
                    }
                }
            }
        } else {
            Log.w("PRESETS", "Empty response for $camIp, cannot parse presets. Using defaults.")
        }

        runOnUiThread {
            updateButtonsAndUrls(presetsMap, camIp)
        }
    }

    private fun updateButtonsAndUrls(presetsMap: Map<Int, String>, camIp: String) {
        val cameraPresetEntries = presetsMap.entries.toList().sortedBy { it.key }
        val homePresetEntry = cameraPresetEntries.find { it.value.equals("Home", ignoreCase = true) }
        val displayablePresets = mutableListOf<Map.Entry<Int, String>>()

        if (presetsMap.isNotEmpty()) {
            if (homePresetEntry != null) {
                displayablePresets.add(homePresetEntry)
            }
            cameraPresetEntries.forEach { entry ->
                if (entry.key != homePresetEntry?.key && displayablePresets.size < 7) {
                    displayablePresets.add(entry)
                }
            }
            if (homePresetEntry == null && displayablePresets.size < 7) {
                cameraPresetEntries.forEach { entry ->
                    if (displayablePresets.size < 7 && !displayablePresets.contains(entry)) {
                         displayablePresets.add(entry)
                    }
                }
            }
            displayablePresets.sortBy { entry -> entry.key }
        }


        val uiSlots = if (camIp == camWestIp) {
            listOf(
                Pair(btnWestChoir) { url: String -> westChoirUrl = url },
                Pair(btnWestPulpit) { url: String -> westPulpitUrl = url },
                Pair(btnWestHome) { url: String -> westHomeUrl = url },
                Pair(btnWestPanorama) { url: String -> westPanoramaUrl = url },
                Pair(btnWestPreset5) { url: String -> westPreset5Url = url },
                Pair(btnWestPreset6) { url: String -> westPreset6Url = url },
                Pair(btnWestPreset7) { url: String -> westPreset7Url = url }
            )
        } else { // camEastIp
            listOf(
                Pair(btnEastChoir) { url: String -> eastChoirUrl = url },
                Pair(btnEastPulpit) { url: String -> eastPulpitUrl = url },
                Pair(btnEastHome) { url: String -> eastHomeUrl = url },
                Pair(btnEastPanorama) { url: String -> eastPanoramaUrl = url },
                Pair(btnEastPreset5) { url: String -> eastPreset5Url = url },
                Pair(btnEastPreset6) { url: String -> eastPreset6Url = url },
                Pair(btnEastPreset7) { url: String -> eastPreset7Url = url }
            )
        }

        for (i in uiSlots.indices) {
            val button = uiSlots[i].first
            val setUrlLambda = uiSlots[i].second

            if (i < displayablePresets.size) {
                val cameraPresetData = displayablePresets[i]
                val actualCameraPresetNumber = cameraPresetData.key
                val actualCameraPresetName = cameraPresetData.value
                button.text = actualCameraPresetName
                button.visibility = View.VISIBLE
                val url = if (actualCameraPresetName.isNotBlank()) {
                    try {
                        "http://$user:$pass@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetname=${URLEncoder.encode(actualCameraPresetName, "UTF-8")}"
                    } catch (e: UnsupportedEncodingException) {
                        Log.e("URL_ENCODE", "Failed to encode preset name: $actualCameraPresetName for slot $i", e)
                        "http://$user:$pass@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetno=$actualCameraPresetNumber" // Fallback
                    }
                } else {
                    "http://$user:$pass@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetno=$actualCameraPresetNumber"
                }
                setUrlLambda(url)
            } else {
                val cameraPrefix = if (camIp == camWestIp) "W" else "E"
                val defaultSlotNumber = i + 1
                val defaultPresetDisplayName = "Unassigned $cameraPrefix$defaultSlotNumber"
                val defaultPresetUrlName = "Unassigned $cameraPrefix$defaultSlotNumber"


                button.text = defaultPresetDisplayName
                button.visibility = View.VISIBLE

                val defaultUrlForSlot = try {
                    "http://$user:$pass@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetname=${URLEncoder.encode(defaultPresetUrlName, "UTF-8")}"
                } catch (e: UnsupportedEncodingException) {
                    Log.e("URL_ENCODE", "Failed to encode default preset name: $defaultPresetUrlName for slot $i", e)
                    "http://$user:$pass@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetname=ErrorEncodingDefault${defaultSlotNumber}"
                }
                setUrlLambda(defaultUrlForSlot)
            }
        }
    }

    private fun setupButtonListeners() {
        // West Camera Buttons
        btnWestChoir.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = westChoirUrl
            sendRequest(westChoirUrl, buttonText)
        }
        btnWestPulpit.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = westPulpitUrl
            sendRequest(westPulpitUrl, buttonText)
        }
        btnWestHome.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = westHomeUrl
            sendRequest(westHomeUrl, buttonText)
        }
        btnWestPanorama.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = westPanoramaUrl
            sendRequest(westPanoramaUrl, buttonText)
        }
        btnWestPreset5.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = westPreset5Url
            sendRequest(westPreset5Url, buttonText)
        }
        btnWestPreset6.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = westPreset6Url
            sendRequest(westPreset6Url, buttonText)
        }
        btnWestPreset7.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText // Corrected line
            tvUrlWest.text = westPreset7Url
            sendRequest(westPreset7Url, buttonText)
        }
        btnWestRefresh.setOnClickListener {
            webViewWest.reload()
            Toast.makeText(this, "Refreshing West Camera...", Toast.LENGTH_SHORT).show()
        }

        // East Camera Buttons
        btnEastChoir.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = eastChoirUrl
            sendRequest(eastChoirUrl, buttonText)
        }
        btnEastPulpit.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = eastPulpitUrl
            sendRequest(eastPulpitUrl, buttonText)
        }
        btnEastHome.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = eastHomeUrl
            sendRequest(eastHomeUrl, buttonText)
        }
        btnEastPanorama.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = eastPanoramaUrl
            sendRequest(eastPanoramaUrl, buttonText)
        }
        btnEastPreset5.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = eastPreset5Url
            sendRequest(eastPreset5Url, buttonText)
        }
        btnEastPreset6.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = eastPreset6Url
            sendRequest(eastPreset6Url, buttonText)
        }
        btnEastPreset7.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = eastPreset7Url
            sendRequest(eastPreset7Url, buttonText)
        }
        btnEastRefresh.setOnClickListener {
            webViewEast.reload()
            Toast.makeText(this, "Refreshing East Camera...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendRequest(urlString: String, presetName: String) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.connectTimeout = 5000
                urlConnection.readTimeout = 5000

                val responseCode = urlConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, getString(R.string.ip_address_format, presetName), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = urlConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
                    Log.e("REQUEST_FAIL", "Failed to activate $presetName. Code: $responseCode. URL: $urlString. Error: $errorBody")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to activate preset: $presetName. Error: $responseCode", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("REQUEST_EXCEPTION", "Error activating preset $presetName with URL: $urlString", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error activating preset: $presetName (${e.message})", Toast.LENGTH_LONG).show()
                }
            } finally {
                urlConnection?.disconnect()
            }
        }
    }
}
