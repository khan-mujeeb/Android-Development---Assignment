package com.example.saassiegment

import android.Manifest.permission.*
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.saassiegment.databinding.ActivityMainBinding
import com.example.saassiegment.model.SystemInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    // creating variables.
    private var pro: Int = 5
    private val CAMERA_REQUEST_CODE = 102
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    lateinit var permissionUtils: PermissionUtils
    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_ACCESS_LOCATION = 101
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        permissionUtils = PermissionUtils(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // display system information
        displayData()


        var timer = Timer()
        var task: TimerTask? = null
        binding.openCamera.setOnClickListener {
            choosePhotoFromCamera()
        }

        binding.refreshBtn.setOnClickListener {
            displayData()
        }
        binding.upload.setOnClickListener{
            sendToDb()
        }

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                task?.cancel() //cancel the previous task
                timer.purge() //remove any scheduled tasks
                timer = Timer() //create new timer object
                task = object : TimerTask() { //create new task
                    override fun run() {
                        (this@MainActivity as Activity).runOnUiThread {
                            displayData()
                        }
                    }
                }
                timer.schedule(task, 0, progress * 1000.toLong()) //schedule new task
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
            // rest of the methods
        })

    }

    fun displayData() {
        isConnected()
        batteryStatus()
        getLocation()
        getDateTime()
    }

    // date and time
    fun getDateTime() {
        var sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        val currentDate = sdf.format(Date())
        binding.dateTime.text = currentDate
        timeStamp = currentDate.toString()


    }

    // battery status
    private fun batteryStatus() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val isCharging: Boolean =
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val batteryPct: Float = (level / scale.toFloat()) * 100
        binding.batterycharing.text = isCharging.toString()
        binding.battery.text = "$batteryPct%"

        charging = isCharging.toString()
        battery = "$batteryPct%"

    }

    // Internet Connectivity
    private fun isConnected() {
        val cld = ConnectionLiveData(application)
        cld.observe(this) { isConnected ->

            if (isConnected) {
                binding.internetConnectivity.text = "connected"

            } else {
                binding.internetConnectivity.text = "disconnected"
            }
        }
    }

    private fun sendToDb(){
        val randomkey = database.reference.push().key.toString()
        var data = SystemInfo(
            imei,
            charging,
            battery,
            lastLocaton,
            timeStamp
        )
        database.reference.child("system info")
            .child(randomkey)
            .setValue(data)
            .addOnSuccessListener {
                Toast.makeText(this,"data uploaded to firebase",Toast.LENGTH_SHORT).show()
            }
    }

    // locations
    private fun getLocation() {
        // permission granted
        if (permissionUtils.checkPermission(ACCESS_COARSE_LOCATION) && permissionUtils.checkPermission(
                ACCESS_FINE_LOCATION
            )
        ) {
            // if permission is enabled and location is disabled
            if (isLocationEnabled()) {
                // get location coordinates
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    if (task != null) {
                        val location = task.result
                        if (location != null) {
                            val geocoder = Geocoder(this, Locale.getDefault())
                            val addresses: MutableList<Address>? = geocoder.getFromLocation(
                                location.latitude,
                                location.longitude, 1
                            )
                            addresses?.let {
                                if (it.isNotEmpty()) {
                                    val city: String = addresses[0].locality
                                    binding.location.text = city
                                    lastLocaton = city
                                }
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
                    ACCESS_COARSE_LOCATION,
                    ACCESS_FINE_LOCATION
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

    // Camera
    private fun choosePhotoFromCamera() {
        Dexter.withActivity(this).withPermissions(
            CAMERA,
            WRITE_EXTERNAL_STORAGE,
            READ_EXTERNAL_STORAGE
        ).withListener(
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if (p0!!.areAllPermissionsGranted()) {
                        Toast.makeText(this@MainActivity, "Permission Granted", Toast.LENGTH_SHORT)
                            .show()
                        openCamera()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    showDialogForPermissions()
                }

            }).onSameThread().check()
    }

    private fun showDialogForPermissions() {
        AlertDialog.Builder(this).setMessage(
            "Allow permission to use this feature"
        ).setPositiveButton("Go to Settings") { _, _ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }.show()
    }

    private fun openCamera() {
        val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (callCameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(callCameraIntent, CAMERA_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (-1 == Activity.RESULT_OK && data != null) {
            val originalBitmap = data!!.extras!!.get("data") as Bitmap
            val newWidth = 200
            val newHeight = 300
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            binding.photo.setImageBitmap(scaledBitmap)
            Toast.makeText(this, "Failed to Capture", Toast.LENGTH_SHORT)
        } else {
            Toast.makeText(this, "Failed to Capture", Toast.LENGTH_SHORT)
        }
    }

    companion object {
        var lastLocaton = ""
        var timeStamp = ""
        var battery = ""
        var charging = ""
        var imei = ""
    }
}