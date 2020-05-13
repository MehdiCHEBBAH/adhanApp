package com.example.adhan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AutoStart : BroadcastReceiver() {
    val restartService = "restart.adhan"

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("restarted", "phone restarted successfully")
        if (intent.action.equals(restartService) || intent.action.equals(Intent.ACTION_BOOT_COMPLETED)){
            context.startService(Intent(context, AdhanService::class.java))
        }
    }
}