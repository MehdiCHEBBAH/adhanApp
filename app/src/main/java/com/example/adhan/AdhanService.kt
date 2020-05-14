package com.example.adhan

import android.app.IntentService
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.fixedRateTimer

class AdhanService: Service() {

    private fun playNotification(context: Context, adhan: String){
        var builder = NotificationCompat.Builder(context, "MyChannel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(adhan)
            .setContentText("حان الآن موعد أذان " + adhan)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(Uri.parse("android.resource://com.example.adhan/" + R.raw.adhan))

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify((Math.random()*1000).toInt(), builder.build())
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val RESTART_SERVICE = "restart.adhan"
        val i = Intent()
        i.action = RESTART_SERVICE
        i.setClass(this, AutoStart::class.java)
        this.sendBroadcast(i)
        super.onTaskRemoved(rootIntent)
    }

    private fun formatTime(time: String): String{
        val timeArray = time.split(":")
        return timeArray[0]+":"+timeArray[1]
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val adhanTimes = intent?.extras?.get("adhanTimes") as AdhanTimes
        val sdf = SimpleDateFormat("HH:mm")
        var timeNow : String
        val context = this

        fixedRateTimer("timer",false,0,1000*60){
            timeNow = sdf.format(Date())
            Log.i("", timeNow)

            if (timeNow == formatTime(adhanTimes.fajr)){
                playNotification(context, "الصبح")
            }else if (timeNow == formatTime(adhanTimes.dohr)){
                playNotification(context, "الظهر")
            }else if (timeNow == formatTime(adhanTimes.asr)){
                playNotification(context, "العصر")
            }else if (timeNow == formatTime(adhanTimes.maghrib)){
                playNotification(context, "المغرب")
            }else if (timeNow == formatTime(adhanTimes.isha)){
                playNotification(context, "العشاء")
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}