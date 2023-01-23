package com.example.saassiegment

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    //    val connection = findViewById<TextView>(R.id.internetConnectivity)
    // in the below line, we are creating variables.
    private val REQUEST_CODE = 101
    private lateinit var imei: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val cld = ConnectionLiveData(application)
        isConnected(cld)
        batteryStatus()

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

}