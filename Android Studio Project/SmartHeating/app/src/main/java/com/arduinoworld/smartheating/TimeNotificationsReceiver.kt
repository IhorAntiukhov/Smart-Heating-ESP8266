package com.arduinoworld.smartheating

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlin.collections.ArrayList

@Suppress("DEPRECATION", "CommitPrefEdits")
class TimeNotificationsReceiver : BroadcastReceiver() {
    private var heatingTimestampsArrayList = ArrayList<String>()
    private var heatingTimestampTypesArrayList = ArrayList<Boolean>()
    private var boilerTimestampsArrayList = ArrayList<String>()
    private var boilerTimestampTypesArrayList = ArrayList<Boolean>()

    private var notificationID = 0
    private var currentTime = ""
    private var notificationText = ""
    private var heatingTimestampsArrayListJson = ""
    private var boilerTimestampsArrayListJson = ""
    private var heatingTimestampTypesArrayListJson = ""
    private var boilerTimestampTypesArrayListJson = ""

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPreferences = context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)

        heatingTimestampsArrayListJson = sharedPreferences.getString("HeatingTimestampsArrayList", "").toString()
        boilerTimestampsArrayListJson = sharedPreferences.getString("BoilerTimestampsArrayList", "").toString()
        heatingTimestampTypesArrayListJson = sharedPreferences.getString("HeatingTimestampTypesArrayList", "").toString()
        boilerTimestampTypesArrayListJson = sharedPreferences.getString("BoilerTimestampTypesArrayList", "").toString()

        val gson = Gson()

        if (heatingTimestampTypesArrayListJson != "") {
            heatingTimestampsArrayList = gson.fromJson(heatingTimestampsArrayListJson, object : TypeToken<ArrayList<String?>?>() {}.type)
            heatingTimestampTypesArrayList = gson.fromJson(heatingTimestampTypesArrayListJson, object : TypeToken<ArrayList<Boolean?>?>() {}.type)
        }
        if (boilerTimestampTypesArrayListJson != "") {
            boilerTimestampsArrayList = gson.fromJson(boilerTimestampsArrayListJson, object : TypeToken<ArrayList<String?>?>() {}.type)
            boilerTimestampTypesArrayList = gson.fromJson(boilerTimestampTypesArrayListJson, object : TypeToken<ArrayList<Boolean?>?>() {}.type)
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "SmartHeating:TimeNotificationLock")

        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 10 && Calendar.getInstance().get(Calendar.MINUTE) < 10) {
            currentTime = "0${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}:0${Calendar.getInstance().get(Calendar.MINUTE)}"
        }
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 10 && Calendar.getInstance().get(Calendar.MINUTE) >= 10) {
            currentTime = "0${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}:${Calendar.getInstance().get(Calendar.MINUTE)}"
        }
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 10 && Calendar.getInstance().get(Calendar.MINUTE) < 10) {
            currentTime = "${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}:0${Calendar.getInstance().get(Calendar.MINUTE)}"
        }
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 10 && Calendar.getInstance().get(Calendar.MINUTE) >= 10) {
            currentTime = "${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}:${Calendar.getInstance().get(Calendar.MINUTE)}"
        }

        if (heatingTimestampsArrayList.contains(currentTime) && boilerTimestampsArrayList.contains(currentTime)) {
            if (heatingTimestampTypesArrayList[heatingTimestampsArrayList.indexOf(currentTime)] && boilerTimestampTypesArrayList[boilerTimestampsArrayList.indexOf(currentTime)]) {
                notificationText = "Котёл и бойлер включились!"
            } else if (heatingTimestampTypesArrayList[heatingTimestampsArrayList.indexOf(currentTime)] && !boilerTimestampTypesArrayList[boilerTimestampsArrayList.indexOf(currentTime)]) {
                notificationText = "Котёл включился, а бойлер выключился!"
            } else if (!heatingTimestampTypesArrayList[heatingTimestampsArrayList.indexOf(currentTime)] && boilerTimestampTypesArrayList[boilerTimestampsArrayList.indexOf(currentTime)]) {
                notificationText = "Котёл выключился, а бойлер включился!"
            }
            notificationID = 3245
        } else if (heatingTimestampsArrayList.contains(currentTime) && !boilerTimestampsArrayList.contains(currentTime)) {
            notificationText = if (heatingTimestampTypesArrayList[heatingTimestampsArrayList.indexOf(currentTime)]) {
                "Котёл включился!"
            } else {
                "Котёл выключился!"
            }
            notificationID = 4168
        } else if (!heatingTimestampsArrayList.contains(currentTime) && boilerTimestampsArrayList.contains(currentTime)) {
            notificationText = if (boilerTimestampTypesArrayList[boilerTimestampsArrayList.indexOf(currentTime)]) {
                "Бойлер включился!"
            } else {
                "Бойлер выключился!"
            }
            notificationID = 5894
        }

        val builder = NotificationCompat.Builder(context, "TimeNotification${sharedPreferences.getInt("NotificationChannelsCount", 0)}")
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Умное Отопление")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
        with(NotificationManagerCompat.from(context)) {
            notify(notificationID, builder.build())
        }
        if (if (Build.VERSION.SDK_INT >= 20) !powerManager.isInteractive else !powerManager.isScreenOn) {
            wakeLock.acquire(3000)
            wakeLock.release()
        }
    }
}