package com.example.cameracontrolapp

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvAppVersion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        Log.d("SettingsActivity", "onCreate called - Displaying app version")

        tvAppVersion = findViewById(R.id.tv_app_version)
        tvAppVersion.text = "App Version: ccsyncv 1.0.1"
    }
}
