package com.example.saassiegment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MyBroadCastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val timestamp = intent?.getStringExtra("TIMESTAMP")
//        println("updated waqt $timestamp")
        val name = intent?.getStringExtra("mujeeb")
        println("hello $name")

    }
}