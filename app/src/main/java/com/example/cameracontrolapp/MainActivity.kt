package com.example.cameracontrolapp

import android.content.Intent // Added
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageButton // Added
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
    private val CAM_WEST = "192.168.88.3"
    private val CAM_EAST = "192.168.88.2"
    private val USER = "admin"
    private val PASS = "oneroom"

    // MJPEG stream URLs with embedded credentials
    private val WEST_STREAM_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/mjpg/video.cgi"
    private val EAST_STREAM_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/mjpg/video.cgi"

    // Query URLs for presets
    private val WEST_QUERY_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?query=presetposall"
    private val EAST_QUERY_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?query=presetposall"

    // Preset URLs (will be dynamically updated after query)
    private var WEST_CHOIR_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Home" // Default
    private var WEST_PULPIT_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Choir"
    private var WEST_HOME_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Pulpit"
    private var WEST_PANORAMA_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Panorama"
    private var WEST_PRESET5_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetname=West%20P5"
    private var WEST_PRESET6_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetname=West%20P6"
    private var WEST_PRESET7_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetname=West%20P7"

    private var EAST_CHOIR_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Choir" // Default
    private var EAST_PULPIT_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Pulpit"
    private var EAST_HOME_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Home"
    private var EAST_PANORAMA_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Panorama"
    private var EAST_PRESET5_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Preset5"
    private var EAST_PRESET6_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Preset6"
    private var EAST_PRESET7_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Preset7"

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
    private lateinit var btnOptions: ImageButton // Added

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_main)

        // Make the activity full screen and hide system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars: they will appear temporarily with a swipe gesture.
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar.
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Initialize UI elements
        tvUrlWest = findViewById(R.id.tv_url_west)
        tvUrlEast = findViewById(R.id.tv_url_east)
        tvUrlWest.text = "West Camera URL will appear here"
        tvUrlEast.text = "East Camera URL will appear here"

        webViewWest = findViewById(R.id.webview_west)
        webViewEast = findViewById(R.id.webview_east)

        setupWebView(webViewWest, WEST_STREAM_URL)
        setupWebView(webViewEast, EAST_STREAM_URL)

        // Initialize TextViews for active preset display
        tvActiveWestPresetDisplay = findViewById(R.id.tv_active_west_preset_display)
        tvActiveEastPresetDisplay = findViewById(R.id.tv_active_east_preset_display)
        // Optionally, set initial text for these displays if needed
        tvActiveWestPresetDisplay.text = "" // Or "No Preset Selected"
        tvActiveEastPresetDisplay.text = "" // Or "No Preset Selected"

        // Initialize buttons
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

        btnOptions = findViewById(R.id.btn_options) // Added
        btnOptions.setOnClickListener { // Added
            val intent = Intent(this, SettingsActivity::class.java) // Added
            startActivity(intent) // Added
        } // Added

        // Query presets at startup and configure buttons
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
        }
        webView.loadUrl(url)
    }

    private fun queryPresets(onComplete: () -> Unit) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val westResponse = fetchQuery(WEST_QUERY_URL)
            parsePresets(westResponse, westPresets, CAM_WEST, false)
            val eastResponse = fetchQuery(EAST_QUERY_URL)
            parsePresets(eastResponse, eastPresets, CAM_EAST, false)
            runOnUiThread {
                if (westPresets.isEmpty() && eastPresets.isEmpty()) {
                    Toast.makeText(this, "Failed to sync presets—using defaults", Toast.LENGTH_LONG).show()
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

    private fun parsePresets(response: String, presetsMap: MutableMap<Int, String>, camIp: String, preferNumbers: Boolean) {
        if (response.isBlank()) {
            Log.w("PRESETS", "Empty response for $camIp, cannot parse presets.")
            return
        }
        Log.d("PRESETS", "Response for $camIp: $response")
        presetsMap.clear()
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
        if (presetsMap.isEmpty()){
            Log.w("PRESETS", "No presets parsed from response for $camIp. Check parsing logic.")
        }
        runOnUiThread {
            updateButtonsAndUrls(presetsMap, camIp, preferNumbers)
        }
    }

    private fun updateButtonsAndUrls(presetsMap: Map<Int, String>, camIp: String, preferNumbers: Boolean) {
        val cameraPresetEntries = presetsMap.entries.toList().sortedBy { it.key }
        val homePresetEntry = cameraPresetEntries.find { it.value.equals("Home", ignoreCase = true) }
        val displayablePresets = mutableListOf<Map.Entry<Int, String>>()
        if (homePresetEntry != null) {
            displayablePresets.add(homePresetEntry)
        }
        cameraPresetEntries.forEach { entry ->
            if (entry.key != homePresetEntry?.key && displayablePresets.size < 7) {
                displayablePresets.add(entry)
            }
        }
        if (homePresetEntry == null) { // If '''Home''' wasn't found, just fill with first available
            displayablePresets.clear()
            cameraPresetEntries.forEach { entry ->
                if (displayablePresets.size < 7) {
                    displayablePresets.add(entry)
                }
            }
        }

        val uiSlots = if (camIp == CAM_WEST) {
            listOf(
                Pair(btnWestChoir, { url: String -> WEST_CHOIR_URL = url }),
                Pair(btnWestPulpit, { url: String -> WEST_PULPIT_URL = url }),
                Pair(btnWestHome, { url: String -> WEST_HOME_URL = url }),
                Pair(btnWestPanorama, { url: String -> WEST_PANORAMA_URL = url }),
                Pair(btnWestPreset5, { url: String -> WEST_PRESET5_URL = url }),
                Pair(btnWestPreset6, { url: String -> WEST_PRESET6_URL = url }),
                Pair(btnWestPreset7, { url: String -> WEST_PRESET7_URL = url })
            )
        } else { // CAM_EAST
            listOf(
                Pair(btnEastChoir, { url: String -> EAST_CHOIR_URL = url }),
                Pair(btnEastPulpit, { url: String -> EAST_PULPIT_URL = url }),
                Pair(btnEastHome, { url: String -> EAST_HOME_URL = url }),
                Pair(btnEastPanorama, { url: String -> EAST_PANORAMA_URL = url }),
                Pair(btnEastPreset5, { url: String -> EAST_PRESET5_URL = url }),
                Pair(btnEastPreset6, { url: String -> EAST_PRESET6_URL = url }),
                Pair(btnEastPreset7, { url: String -> EAST_PRESET7_URL = url })
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
                val url = if (!preferNumbers && actualCameraPresetName.isNotBlank()) {
                    try {
                        "http://$USER:$PASS@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetname=${URLEncoder.encode(actualCameraPresetName, "UTF-8")}"
                    } catch (e: UnsupportedEncodingException) {
                        Log.e("URL_ENCODE", "Failed to encode preset name: $actualCameraPresetName", e)
                        "http://$USER:$PASS@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetno=$actualCameraPresetNumber" // Fallback
                    }
                } else {
                    "http://$USER:$PASS@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetno=$actualCameraPresetNumber"
                }
                setUrlLambda(url)
            } else {
                button.text = "Preset ${i + 1}" // Default text if not enough presets
                button.visibility = View.GONE // Hide button if no preset for this slot
            }
        }
    }

    private fun setupButtonListeners() {
        // West Camera Buttons
        btnWestChoir.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = WEST_CHOIR_URL
            sendRequest(WEST_CHOIR_URL, buttonText)
        }
        btnWestPulpit.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = WEST_PULPIT_URL
            sendRequest(WEST_PULPIT_URL, buttonText)
        }
        btnWestHome.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = WEST_HOME_URL
            sendRequest(WEST_HOME_URL, buttonText)
        }
        btnWestPanorama.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = WEST_PANORAMA_URL
            sendRequest(WEST_PANORAMA_URL, buttonText)
        }
        btnWestPreset5.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = WEST_PRESET5_URL
            sendRequest(WEST_PRESET5_URL, buttonText)
        }
        btnWestPreset6.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = WEST_PRESET6_URL
            sendRequest(WEST_PRESET6_URL, buttonText)
        }
        btnWestPreset7.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveWestPresetDisplay.text = buttonText
            tvUrlWest.text = WEST_PRESET7_URL
            sendRequest(WEST_PRESET7_URL, buttonText)
        }
        btnWestRefresh.setOnClickListener {
            webViewWest.reload()
            Toast.makeText(this, "Refreshing West Camera...", Toast.LENGTH_SHORT).show()
        }

        // East Camera Buttons
        btnEastChoir.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = EAST_CHOIR_URL
            sendRequest(EAST_CHOIR_URL, buttonText)
        }
        btnEastPulpit.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = EAST_PULPIT_URL
            sendRequest(EAST_PULPIT_URL, buttonText)
        }
        btnEastHome.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = EAST_HOME_URL
            sendRequest(EAST_HOME_URL, buttonText)
        }
        btnEastPanorama.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = EAST_PANORAMA_URL
            sendRequest(EAST_PANORAMA_URL, buttonText)
        }
        btnEastPreset5.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = EAST_PRESET5_URL
            sendRequest(EAST_PRESET5_URL, buttonText)
        }
        btnEastPreset6.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = EAST_PRESET6_URL
            sendRequest(EAST_PRESET6_URL, buttonText)
        }
        btnEastPreset7.setOnClickListener {
            val buttonText = (it as Button).text.toString()
            tvActiveEastPresetDisplay.text = buttonText
            tvUrlEast.text = EAST_PRESET7_URL
            sendRequest(EAST_PRESET7_URL, buttonText)
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
                urlConnection.connectTimeout = 5000 // 5 seconds
                urlConnection.readTimeout = 5000    // 5 seconds

                val responseCode = urlConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Optionally, you can read the response if needed:
                    // val responseBody = urlConnection.inputStream.bufferedReader().use { it.readText() }
                    // Log.d("REQUEST_SUCCESS", "Successfully activated $presetName. Response: $responseBody")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Activated preset: $presetName", Toast.LENGTH_SHORT).show()
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
