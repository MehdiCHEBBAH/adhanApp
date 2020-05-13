package com.example.adhan


import com.azan.Time
import java.io.Serializable

class AdhanTimes(val fajr: String, val shuruq: String, val dohr: String, val asr: String, val maghrib: String, val isha: String) : Serializable{
}