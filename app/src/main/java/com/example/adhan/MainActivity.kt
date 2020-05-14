package com.example.adhan

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.azan.Azan
import com.azan.Method
import com.azan.astrologicalCalc.Location
import com.azan.astrologicalCalc.SimpleDate
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


private const val PERMISSION_REQUEST = 10
/* used this prayer time engine */
class MainActivity : AppCompatActivity() {

    lateinit var locationManager: LocationManager
    private var hasGps = false
    private var hasNetwork = false
    private var locationGps: android.location.Location? = null
    private var locationNetwork: android.location.Location? = null
    private var location: android.location.Location? = null
    private var permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /******* Init with some data of Jijel city ***********/
    var today = SimpleDate(GregorianCalendar())
    var adhanLocation = Location(36.8167387, 5.7490933, 1.0, 0)
    var azan = Azan(adhanLocation, Method.MUSLIM_LEAGUE)
    var prayerTimes = azan.getPrayerTimes(today)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*********************** Getting Location **********************/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermission(permissions)) {
                updateLocation()
            } else {
                requestPermissions(permissions, PERMISSION_REQUEST)
            }
        } else {
            updateLocation()
        }


        date_text.text = "" + today.day + " / " + today.month + " / " + today.year
        fajr_text.text = formatTime(prayerTimes.fajr().toString())
        sunrise_text.text = formatTime(prayerTimes.shuruq().toString())
        dohr_text.text = formatTime(prayerTimes.thuhr().toString())
        asr_text.text = formatTime(prayerTimes.assr().toString())
        maghrib_text.text = formatTime(prayerTimes.maghrib().toString())
        isha_text.text = formatTime(prayerTimes.ishaa().toString())


        /********* Create notification channel ************/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Adhan"
            val descriptionText = "Adhan"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("MyChannel", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        /******************* Creating the service ******************/
        val intent = Intent(this, AdhanService::class.java)
        val adhanTimes = AdhanTimes(
            formatTime(prayerTimes.fajr().toString()),
            formatTime(prayerTimes.shuruq().toString()),
            formatTime(prayerTimes.thuhr().toString()),
            formatTime(prayerTimes.assr().toString()),
            formatTime(prayerTimes.maghrib().toString()),
            formatTime(prayerTimes.ishaa().toString())
        )
        intent.putExtra("adhanTimes", adhanTimes)
        startService(intent)
    }

    private fun updateAdhanTimes(date: SimpleDate, lat: Double, lon: Double){
         adhanLocation = Location(lat, lon, 1.0, 0)
         azan = Azan(adhanLocation, Method.MUSLIM_LEAGUE)
         prayerTimes = azan.getPrayerTimes(date)
    }

    private fun checkPermission(permissionArray: Array<String>): Boolean {
        var allSuccess = true
        for (i in permissionArray.indices) {
            if (checkCallingOrSelfPermission(permissionArray[i]) == PackageManager.PERMISSION_DENIED)
                allSuccess = false
        }
        return allSuccess
    }

    @SuppressLint("MissingPermission")
    private fun updateLocation() {
        var location: android.location.Location? = location
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (hasGps || hasNetwork) {

            if (hasGps) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    0F,
                    object : LocationListener {

                        override fun onLocationChanged(location: android.location.Location?) {
                            if (location != null) {
                                locationGps = location
                            }
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String?) {}
                        override fun onProviderDisabled(provider: String?) {}
                    })

                val localGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (localGpsLocation != null)
                    locationGps = localGpsLocation
            }
            if (hasNetwork) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    0F,
                    object : LocationListener {
                        override fun onLocationChanged(location: android.location.Location?) {
                            if (location != null) {
                                locationNetwork = location
                            }
                        }
                        override fun onStatusChanged(provider: String?, status: Int,extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String?) {}
                        override fun onProviderDisabled(provider: String?) {}
                    })

                val localNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (localNetworkLocation != null)
                    locationNetwork = localNetworkLocation
            }

            if (locationGps != null && locationNetwork != null) {
                if (locationGps!!.accuracy > locationNetwork!!.accuracy) {
                    location = locationNetwork
                    updateAdhanTimes(SimpleDate(GregorianCalendar()), location!!.latitude, location!!.longitude)
                } else {
                    location = locationGps
                    updateAdhanTimes(SimpleDate(GregorianCalendar()), location!!.latitude, location!!.longitude)
                }
            }
        } else {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            var allSuccess = true
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    allSuccess = false
                    val requestAgain = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(permissions[i])
                    if (requestAgain) {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Go to settings and enable the permission", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (allSuccess)
                updateLocation()
        }
    }

    private fun formatTime(time: String): String{
        val timeArray = time.split(":")
        return timeArray[0]+":"+timeArray[1]
    }
}
