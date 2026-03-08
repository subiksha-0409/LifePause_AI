package com.example.lifepauseai

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
class MainActivity : ComponentActivity(), SensorEventListener {

    private var lastLatitude = 0.0
    private var lastLongitude = 0.0
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var smsAlreadySent = false
    private var showEmergencyDialog by mutableStateOf(false)
    private var countdownSeconds by mutableStateOf(10)
    private var countdownHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    private var previousSpeed = 0.0
    private var speedSpikeDetected = false
    private lateinit var inactivityHandler: android.os.Handler
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isMonitoring = false
    private var lastMovementTime = System.currentTimeMillis()
    private var stillnessDetected = false
    private lateinit var locationCallback: com.google.android.gms.location.LocationCallback
    private lateinit var locationRequest: com.google.android.gms.location.LocationRequest
    private var dangerScore = 0
    private val DANGER_THRESHOLD = 5
    private val STILLNESS_THRESHOLD_LOW = 9.5
    private val STILLNESS_THRESHOLD_HIGH = 10.5
    private val STILLNESS_TIME_LIMIT = 10000L // 10 seconds (testing only)
    private var lastScreenTouchTime = System.currentTimeMillis()
    private var screenInactivityDetected = false
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private val SCREEN_INACTIVITY_LIMIT = 15000L // 15 seconds (testing)
    private val smsSentReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            when (resultCode) {
                RESULT_OK -> {
                    Log.d("LifePause", "SMS successfully sent")
                }
                android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                    Log.d("LifePause", "SMS failed: Generic failure")
                }
                android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE -> {
                    Log.d("LifePause", "SMS failed: No service")
                }
                android.telephony.SmsManager.RESULT_ERROR_NULL_PDU -> {
                    Log.d("LifePause", "SMS failed: Null PDU")
                }
                android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF -> {
                    Log.d("LifePause", "SMS failed: Radio off")
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        checkSmsPermission()
        super.onCreate(savedInstanceState)
        registerReceiver(
            smsSentReceiver,
            android.content.IntentFilter("SMS_SENT"),
            Context.RECEIVER_NOT_EXPORTED
        )
        inactivityHandler = android.os.Handler(mainLooper)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()
        setContent {
            MaterialTheme {
                LifePauseApp()
            }
        }
    }
    private fun startEmergencyCountdown() {

        countdownSeconds = 10
        Log.d("LifePause", "Countdown started")

        countdownRunnable?.let {
            countdownHandler.removeCallbacks(it)
        }

        countdownRunnable = object : Runnable {
            override fun run() {

                Log.d("LifePause", "Countdown: $countdownSeconds")

                if (countdownSeconds > 0) {
                    countdownSeconds--
                    countdownHandler.postDelayed(this, 1000)
                } else {

                    Log.d("LifePause", "Countdown finished")

                    showEmergencyDialog = false

                    if (!smsAlreadySent) {
                        sendEmergencySms(lastLatitude, lastLongitude)
                        startAlarm()
                        smsAlreadySent = true
                    }
                }
            }
        }

        countdownHandler.post(countdownRunnable!!)
    }
    private fun sendEmergencySms(latitude: Double, longitude: Double) {

        val smsManager = android.telephony.SmsManager.getDefault()

        val message = "EMERGENCY ALERT! Possible danger detected. Location: https://maps.google.com/?q=$latitude,$longitude"

        val contacts = listOf(
            "8122592905"

        )

        for (number in contacts) {
            smsManager.sendTextMessage(number, null, message, null, null)
        }

        Log.d("LifePause", "Emergency SMS sent to all contacts")
    }
    private fun startAlarm() {

        if (mediaPlayer == null) {
            mediaPlayer = android.media.MediaPlayer.create(
                this,
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            )
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
    }
    private fun stopAlarm() {

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    private fun checkSmsPermission() {
        val permissions = arrayOf(
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS
        )
        requestPermissions(permissions, 2001)
    }
    private fun startInactivityMonitor() {
        inactivityHandler.post(object : Runnable {
            override fun run() {
                Log.d("LifePause", "Inactivity Monitor Running")
                val currentTime = System.currentTimeMillis()
                Log.d("LifePause", "Time since last touch: ${currentTime - lastScreenTouchTime}")
                if (isMonitoring &&
                    !screenInactivityDetected &&
                    currentTime - lastScreenTouchTime > SCREEN_INACTIVITY_LIMIT
                ) {
                    Log.d("LifePause", "Screen Inactivity Detected")
                    screenInactivityDetected = true
                    dangerScore += 2
                    Log.d("LifePause", "Danger Score: $dangerScore")
                }
                inactivityHandler.postDelayed(this, 1000)
            }
        })
    }
    private fun getLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d("LifePause", "Latitude: ${location.latitude}")
                        Log.d("LifePause", "Longitude: ${location.longitude}")
                    } else {
                        Log.d("LifePause", "Location is null")
                    }
                }
        }
    }
    private fun checkLocationPermission() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }
    private fun startLocationUpdates() {
        // 🔥 RESET VARIABLES HERE
        previousSpeed = 0.0
        speedSpikeDetected = false
        dangerScore = 0
        Log.d("LifePause", "Reset spike variables")
        Log.d("LifePause", "Starting Location Updates")
        locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).build()
        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                Log.d("LifePause", "Location Callback Triggered")
                for (location in locationResult.locations) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    lastLatitude = latitude
                    lastLongitude = longitude
                    val speedKmph = 45.0
                    Log.d("LifePause", "Latitude: $latitude")
                    Log.d("LifePause", "Longitude: $longitude")
                    Log.d("LifePause", "Speed: $speedKmph km/h")
                    Log.d("LifePause", "speedSpikeDetected = $speedSpikeDetected")
                    if (!speedSpikeDetected &&
                        speedKmph > 30 &&
                        screenInactivityDetected
                    ) {
                        Log.d("LifePause", "Sudden High Speed Detected")
                        dangerScore += 4
                        Log.d("LifePause", "Danger Score: $dangerScore")
                        if (dangerScore >= 6) {
                            Log.d("LifePause", "Emergency dialog triggered")
                            showEmergencyDialog = true
                            startEmergencyCountdown()

                        }
                        speedSpikeDetected = true
                    }
                    previousSpeed = speedKmph
                }
            }
        }
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
        }
    }
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        lastScreenTouchTime = System.currentTimeMillis()
        screenInactivityDetected = false
        dangerScore = maxOf(0, dangerScore - 1)
        Log.d("LifePause", "Danger Score: $dangerScore")
        Log.d("LifePause", "Touch Event Detected")
        return super.dispatchTouchEvent(ev)
    }
    @Composable
    fun LifePauseApp() {
        var monitoringState by remember { mutableStateOf(false) }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (monitoringState) Color(0xFFE8F5E9) else Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LifePause AI",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (monitoringState) "Monitoring Active" else "Monitoring Inactive"
                )
                Spacer(modifier = Modifier.height(30.dp))
                Button(
                    onClick = {
                        monitoringState = !monitoringState
                        isMonitoring = monitoringState
                        if (isMonitoring) {
                            Log.d("LifePause", "Monitoring Started")
                            // 🔥 RESET EVERYTHING PROPERLY
                            smsAlreadySent = false
                            previousSpeed = 0.0
                            speedSpikeDetected = false
                            dangerScore = 0
                            screenInactivityDetected = false
                            lastScreenTouchTime = System.currentTimeMillis()
                            startSensor()
                            startLocationUpdates()
                            startInactivityMonitor()
                        } else {
                            stopSensor()
                            stopLocationUpdates()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (monitoringState) Color.Red else Color.Green
                    )
                ) {
                    Text(
                        text = if (monitoringState) "Stop Monitoring" else "Start Monitoring"
                    )
                }
            }
        }
        if (showEmergencyDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Are You Safe?") },
                text = { Text("Respond within $countdownSeconds seconds") },
                confirmButton = {
                    Button(
                        onClick = {
                            showEmergencyDialog = false
                            stopAlarm()
                            dangerScore = 0
                            speedSpikeDetected = false
                            previousSpeed = 0.0
                            lastScreenTouchTime = System.currentTimeMillis()

                            countdownRunnable?.let {
                                countdownHandler.removeCallbacks(it)
                            }
                            Log.d("LifePause", "User Confirmed Safe")
                        }
                    ) {
                        Text("I am Safe")
                    }
                }
            )
        }
    }
    private fun startSensor() {
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("LifePause", "Accelerometer Started")
        }
    }
    private fun stopSensor() {
        sensorManager.unregisterListener(this)
        Log.d("LifePause", "Accelerometer Stopped")
    }
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && isMonitoring) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val movement = sqrt((x * x + y * y + z * z).toDouble())
            Log.d("LifePause", "Movement Value: $movement")
            val currentTime = System.currentTimeMillis()
            // Movement detected
            if (movement < STILLNESS_THRESHOLD_LOW || movement > STILLNESS_THRESHOLD_HIGH) {
                lastMovementTime = currentTime
                stillnessDetected = false
                dangerScore = maxOf(0, dangerScore - 1)
                Log.d("LifePause", "Danger Score: $dangerScore")
            } else {
                if (!stillnessDetected && currentTime - lastMovementTime > STILLNESS_TIME_LIMIT) {
                    Log.d("LifePause", "Stillness Detected")
                    stillnessDetected = true
                    dangerScore += 2
                    Log.d("LifePause", "Danger Score: $dangerScore")
                }
            }
            // Screen inactivity check (MUST be inside monitoring block)
            if (!screenInactivityDetected && currentTime - lastScreenTouchTime > SCREEN_INACTIVITY_LIMIT) {
                Log.d("LifePause", "Screen Inactivity Detected")
                screenInactivityDetected = true
                dangerScore += 2
                Log.d("LifePause", "Danger Score: $dangerScore")
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onResume() {
        super.onResume()

        if (mediaPlayer != null) {
            stopAlarm()
            Log.d("LifePause", "Alarm stopped because app reopened")
        }
    }
}
