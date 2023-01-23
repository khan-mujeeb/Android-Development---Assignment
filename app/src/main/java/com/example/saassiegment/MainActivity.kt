package com.example.saassiegment

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    //    val connection = findViewById<TextView>(R.id.internetConnectivity)
    // creating variables.
    private val REQUEST_CODE = 101
    private val PERMISSION_REQUEST_ACCESS_LOCATION = 101
    private lateinit var imei: String
    lateinit var permissionUtils: PermissionUtils
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionUtils = PermissionUtils(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val cld = ConnectionLiveData(application)
        isConnected(cld)
        batteryStatus()
        getLocation()

        var sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        val currentDate = sdf.format(Date())
        println("date is $currentDate")

    }

    private fun batteryStatus() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val isCharging: Boolean =
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        println("battery is $isCharging")

        val batteryPct: Float = (level / scale.toFloat()) * 100
        println("battery is $batteryPct")
    }

    private fun isConnected(cld: ConnectionLiveData) {
        cld.observe(this) { isConnected ->

            if (isConnected) {
                println("connected")
//                connection.text = "connected"
            } else {
                println("disconnected")
//                connection.text = "disconnected"
            }
        }
    }

    private fun getLocation() {
        // permission granted
        if (permissionUtils.checkPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) && permissionUtils.checkPermission(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // if permission is enabled and location is disabled
            if (isLocationEnabled()) {
                // get location coordinates
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    if (task != null) {
                        val location = task.result
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses: MutableList<Address>? = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude, 1)

                        addresses?.let {
                            if (it.isNotEmpty()) {
                                val city: String = addresses[0].locality
                                println("mera address $city")
                            }
                        }

                    }
                }
            } else {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
        // permission not granted
        else {
            // request permission here
            permissionUtils.askPermission(
                arrayOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ), PERMISSION_REQUEST_ACCESS_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

}