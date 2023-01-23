package com.example.saassiegment

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionUtils(var activity: AppCompatActivity) {

    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun askPermission(permissions: Array<String>, requestCode: Int){
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

}