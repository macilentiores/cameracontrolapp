package com.example.cameracontrolapp

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private var WEST_CHOIR_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=1" // Default
    private var WEST_PULPIT_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=2"
    private var WEST_HOME_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=3"
    private var WEST_PANORAMA_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=4"
    private var WEST_PRESET5_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=5"
    private var WEST_PRESET6_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=6"
    private var WEST_PRESET7_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=7"

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

    // Buttons (declare as lateinits for dynamic access)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        tvUrlWest = findViewById(R.id.tv_url_west)
        tvUrlEast = findViewById(R.id.tv_url_east)
        tvUrlWest.text = "West Camera URL will appear here"
        tvUrlEast.text = "East Camera URL will appear here"

        webViewWest = findViewById(R.id.webview_west)
        webViewEast = findViewById(R.id.webview_east)

        setupWebView(webViewWest, WEST_STREAM_URL)
        setupWebView(webViewEast, EAST_STREAM_URL)

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

    // Function to query presets for both cameras asynchronously
    private fun queryPresets(onComplete: () -> Unit) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            // Query West
            val westResponse = fetchQuery(WEST_QUERY_URL)
            parsePresets(westResponse, westPresets, CAM_WEST, true) // true for West (prefer numbers)

            // Query East
            val eastResponse = fetchQuery(EAST_QUERY_URL)
            parsePresets(eastResponse, eastPresets, CAM_EAST, false) // false for East (prefer names)

            runOnUiThread {
                if (westPresets.isEmpty() && eastPresets.isEmpty()) {
                    Toast.makeText(this, "Failed to sync presets—using defaults", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Presets synced successfully", Toast.LENGTH_SHORT).show()
                }
                onComplete() // Callback to set listeners
            }
        }
    }

    // Helper to fetch query response body
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

    // Parse response and update URLs/maps
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
            Log.w("PRESETS", "No presets parsed from response for $camIp. Check parsing logic against actual camera output. Does it start with 'presetposno'?")
        }


        runOnUiThread {
            updateButtonsAndUrls(presetsMap, camIp, preferNumbers)
        }
    }

    // Update button texts and URLs based on parsed map
    private fun updateButtonsAndUrls(presetsMap: Map<Int, String>, camIp: String, preferNumbers: Boolean) {
        // 1. Get camera presets, sorted by camera's preset number
        val cameraPresetEntries = presetsMap.entries.toList().sortedBy { it.key }

        // 2. Find "Home" preset (case-insensitive)
        val homePresetEntry = cameraPresetEntries.find { it.value.equals("Home", ignoreCase = true) }

        // 3. Create the list of presets to display, prioritizing "Home"
        val displayablePresets = mutableListOf<Map.Entry<Int, String>>()
        if (homePresetEntry != null) {
            displayablePresets.add(homePresetEntry)
        }
        // Add remaining presets, ensuring "Home" isn't added twice if it was already first,
        // and respecting the max number of buttons (7).
        cameraPresetEntries.forEach { entry ->
            if (entry.key != homePresetEntry?.key && displayablePresets.size < 7) {
                displayablePresets.add(entry)
            }
        }
        // If "Home" wasn't found, and the list is shorter than 7, fill it up to 7 or
        // until all camera presets are used.
        if (homePresetEntry == null) {
            displayablePresets.clear() // Start fresh if no home was found
            cameraPresetEntries.forEach { entry ->
                if (displayablePresets.size < 7) {
                    displayablePresets.add(entry)
                }
            }
        }


        // 4. Define UI slots (Button and its corresponding URL variable setter)
        // These are in the fixed visual order of the buttons in the layout.
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

        // 5. Populate UI slots with displayable presets
        for (i in uiSlots.indices) { // Iterate 0 to 6 for the 7 UI button slots
            val button = uiSlots[i].first
            val setUrlLambda = uiSlots[i].second

            if (i < displayablePresets.size) {
                val cameraPresetData = displayablePresets[i] // This is a Map.Entry<Int, String>
                val actualCameraPresetNumber = cameraPresetData.key
                val actualCameraPresetName = cameraPresetData.value

                button.text = actualCameraPresetName
                button.visibility = View.VISIBLE

                val url = if (!preferNumbers && actualCameraPresetName.isNotBlank()) {
                    try {
                        "http://$USER:$PASS@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetname=${URLEncoder.encode(actualCameraPresetName, "UTF-8")}"
                    } catch (e: UnsupportedEncodingException) {
                        Log.e("URL_ENCODE", "Failed to encode preset name: $actualCameraPresetName", e)
                        // Fallback to preset number if encoding fails (should be rare)
                        "http://$USER:$PASS@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetno=$actualCameraPresetNumber"
                    }
                } else {
                    "http://$USER:$PASS@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetno=$actualCameraPresetNumber"
                }
                setUrlLambda(url)
            } else {
                // No camera preset for this UI slot
                button.text = "Preset ${i + 1}" // Or a more generic "Empty" or keep XML default
                button.visibility = View.GONE
                // The URL variable associated with this slot (e.g., WEST_PRESET7_URL if it's the 7th slot)
                // will retain its default value, which is fine as the button is hidden.
            }
        }
    }

    // Set button listeners after presets are synced
    private fun setupButtonListeners() {
        // West Camera Buttons
        btnWestChoir.setOnClickListener {
            tvUrlWest.text = WEST_CHOIR_URL
            sendRequest(WEST_CHOIR_URL, btnWestChoir.text.toString())
        }
        btnWestPulpit.setOnClickListener {
            tvUrlWest.text = WEST_PULPIT_URL
            sendRequest(WEST_PULPIT_URL, btnWestPulpit.text.toString())
        }
        btnWestHome.setOnClickListener {
            tvUrlWest.text = WEST_HOME_URL
            sendRequest(WEST_HOME_URL, btnWestHome.text.toString())
        }
        btnWestPanorama.setOnClickListener {
            tvUrlWest.text = WEST_PANORAMA_URL
            sendRequest(WEST_PANORAMA_URL, btnWestPanorama.text.toString())
        }
        btnWestPreset5.setOnClickListener {
            tvUrlWest.text = WEST_PRESET5_URL
            sendRequest(WEST_PRESET5_URL, btnWestPreset5.text.toString())
        }
        btnWestPreset6.setOnClickListener {
            tvUrlWest.text = WEST_PRESET6_URL
            sendRequest(WEST_PRESET6_URL, btnWestPreset6.text.toString())
        }
        btnWestPreset7.setOnClickListener {
            tvUrlWest.text = WEST_PRESET7_URL
            sendRequest(WEST_PRESET7_URL, btnWestPreset7.text.toString())
        }
        btnWestRefresh.setOnClickListener {
            webViewWest.reload()
            Toast.makeText(this, "Refreshing West Camera...", Toast.LENGTH_SHORT).show()
        }

        // East Camera Buttons
        btnEastChoir.setOnClickListener {
            tvUrlEast.text = EAST_CHOIR_URL
            sendRequest(EAST_CHOIR_URL, btnEastChoir.text.toString())
        }
        btnEastPulpit.setOnClickListener {
            tvUrlEast.text = EAST_PULPIT_URL
            sendRequest(EAST_PULPIT_URL, btnEastPulpit.text.toString())
        }
        btnEastHome.setOnClickListener {
            tvUrlEast.text = EAST_HOME_URL
            sendRequest(EAST_HOME_URL, btnEastHome.text.toString())
        }
        btnEastPanorama.setOnClickListener {
            tvUrlEast.text = EAST_PANORAMA_URL
            sendRequest(EAST_PANORAMA_URL, btnEastPanorama.text.toString())
        }
        btnEastPreset5.setOnClickListener {
            tvUrlEast.text = EAST_PRESET5_URL
            sendRequest(EAST_PRESET5_URL, btnEastPreset5.text.toString())
        }
        btnEastPreset6.setOnClickListener {
            tvUrlEast.text = EAST_PRESET6_URL
            sendRequest(EAST_PRESET6_URL, btnEastPreset6.text.toString())
        }
        btnEastPreset7.setOnClickListener {
            tvUrlEast.text = EAST_PRESET7_URL
            sendRequest(EAST_PRESET7_URL, btnEastPreset7.text.toString())
        }
        btnEastRefresh.setOnClickListener {
            webViewEast.reload()
            Toast.makeText(this, "Refreshing East Camera...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendRequest(urlString: String, actionName: String) {
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
                val responseBody = if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    urlConnection.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                } else {
                    urlConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }
                Log.d("PTZ_RESPONSE", "Action: $actionName, URL: $urlString, Code: $responseCode, Body: $responseBody")

                runOnUiThread {
                    if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || (responseCode == HttpURLConnection.HTTP_OK && !responseBody.contains("Error", ignoreCase = true))) {
                        Toast.makeText(this, "$actionName: Success", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "$actionName: Failed (Code: $responseCode, Response: $responseBody)", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "$actionName: Error (${e.message})", Toast.LENGTH_LONG).show()
                }
            } finally {
                urlConnection?.disconnect()
            }
        }
    }
}
