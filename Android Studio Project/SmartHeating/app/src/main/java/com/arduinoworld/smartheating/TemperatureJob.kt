package com.arduinoworld.smartheating

import android.annotation.SuppressLint
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@SuppressLint("CommitPrefEdits")
@Suppress("ControlFlowWithEmptyBody", "DEPRECATION")
class TemperatureJob : JobService() {
    lateinit var realtimeDatabase : DatabaseReference
    lateinit var temperatureControlValueEventListener : ValueEventListener

    private var userUID = ""

    override fun onStartJob(p0: JobParameters?): Boolean {
        val sharedPreferences = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        val editPreferences = sharedPreferences.edit()

        val firebaseAuth = FirebaseAuth.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance().reference.child("users")

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "SmartHeating:TemperatureNotificationLock")

        userUID = firebaseAuth.currentUser!!.uid

        val startHeatingTemperature = sharedPreferences.getInt("HomeTemperature", 0) - sharedPreferences.getInt("HeatingStartTemperature", 2)
        val stopHeatingTemperature = sharedPreferences.getInt("HomeTemperature", 0) + sharedPreferences.getInt("HeatingStopTemperature", 2)
        var heatingPhase = sharedPreferences.getBoolean("HeatingPhase", false)

        var changeVariable = sharedPreferences.getBoolean("ChangeVariable", false)
        var startHeating1 = sharedPreferences.getBoolean("StartHeating1", false)
        var startHeating2 = sharedPreferences.getBoolean("StartHeating2", false)

        temperatureControlValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temperature = snapshot.getValue(Int::class.java)!!
                var notificationText = ""

                if (!heatingPhase) {
                    if (temperature <= stopHeatingTemperature) {
                        changeVariable = !changeVariable
                        editPreferences.putBoolean("ChangeVariable", changeVariable)
                        if (changeVariable) {
                            startHeating1 = true
                            editPreferences.putBoolean("StartHeating1", startHeating1).apply()
                        } else {
                            startHeating2 = true
                            editPreferences.putBoolean("StartHeating2", startHeating2).apply()
                        }
                        notificationText = "Котёл запущен по температуре!\nТемпература: $temperature°С"
                    } else {
                        changeVariable = !changeVariable
                        editPreferences.putBoolean("ChangeVariable", changeVariable)
                        if (changeVariable) {
                            startHeating1 = false
                            editPreferences.putBoolean("StartHeating1", startHeating1).apply()
                        } else {
                            startHeating2 = false
                            editPreferences.putBoolean("StartHeating2", startHeating2).apply()
                        }
                        notificationText = "Котёл остановлен по температуре!\nТемпература: $temperature°С"
                        heatingPhase = true
                        editPreferences.putBoolean("HeatingPhase", true).apply()
                    }
                }
                if (heatingPhase) {
                    if (temperature < startHeatingTemperature) {
                        changeVariable = !changeVariable
                        editPreferences.putBoolean("ChangeVariable", changeVariable)
                        if (changeVariable) {
                            startHeating1 = true
                            editPreferences.putBoolean("StartHeating1", startHeating1).apply()
                        } else {
                            startHeating2 = true
                            editPreferences.putBoolean("StartHeating2", startHeating2).apply()
                        }
                        notificationText = "Котёл запущен по температуре!\nТемпература: $temperature°С"
                        heatingPhase = false
                        editPreferences.putBoolean("HeatingPhase", false).apply()
                    }
                }

                val builder = NotificationCompat.Builder(applicationContext, "TemperatureNotification")
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("Управление по Температуре")
                        .setContentText(notificationText)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                with(NotificationManagerCompat.from(applicationContext)) {
                    notify(6852, builder.build())
                }
                if (if (Build.VERSION.SDK_INT >= 20) !powerManager.isInteractive else !powerManager.isScreenOn) {
                    wakeLock.acquire(3000)
                    wakeLock.release()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "Не удалось \n получить температуру!", Toast.LENGTH_LONG).show()
            }
        }
        realtimeDatabase.child(userUID).child("temperature").addValueEventListener(temperatureControlValueEventListener)
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        realtimeDatabase.child(userUID).child("temperature").removeEventListener(temperatureControlValueEventListener)
        return true
    }
}