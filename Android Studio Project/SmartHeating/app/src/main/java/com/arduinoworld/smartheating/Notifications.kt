package com.arduinoworld.smartheating

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.Fragment

@Suppress("DEPRECATION")
@SuppressLint("CommitPrefEdits")
class Notifications : Fragment() {
    private var isHapticFeedbackEnabled = true
    lateinit var buttonDefaultNotifications : Button
    lateinit var notificationManager : NotificationManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        val editPreferences = sharedPreferences.edit()

        notificationManager = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val checkBoxNotificationsSound : CheckBox = requireActivity().findViewById(R.id.checkBoxNotificationsSound)
        val checkBoxTimerNotifications : CheckBox = requireActivity().findViewById(R.id.checkBoxTimerNotifications)
        val checkBoxTimeNotifications : CheckBox = requireActivity().findViewById(R.id.checkBoxTimeNotifications)
        val checkBoxTemperatureNotifications : CheckBox = requireActivity().findViewById(R.id.checkBoxTemperatureNotifications)
        buttonDefaultNotifications = requireActivity().findViewById(R.id.buttonDefaultNotifications)

        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)
        checkBoxNotificationsSound.isChecked = sharedPreferences.getBoolean("NotificationsSound", true)
        checkBoxTimerNotifications.isChecked = sharedPreferences.getBoolean("TimerNotifications", true)
        checkBoxTimeNotifications.isChecked = sharedPreferences.getBoolean("TimeNotifications", true)
        checkBoxTemperatureNotifications.isChecked = sharedPreferences.getBoolean("TemperatureNotifications", true)

        checkBoxNotificationsSound.setOnCheckedChangeListener { _, isChecked ->
            vibrate()
            editPreferences.putBoolean("NotificationsSound", isChecked).apply()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!sharedPreferences.getBoolean("NotificationGroupCreated", false)) {
                    notificationManager.createNotificationChannelGroup(NotificationChannelGroup("SmartHeating", "Умное Отопление"))
                    editPreferences.putBoolean("NotificationGroupCreated", true).apply()
                }
                var notificationChannelsCount = sharedPreferences.getInt("NotificationChannelsCount", 0)
                notificationManager.deleteNotificationChannel("TimerNotification$notificationChannelsCount")
                notificationManager.deleteNotificationChannel("TimeNotification$notificationChannelsCount")
                notificationManager.deleteNotificationChannel("TemperatureNotification$notificationChannelsCount")
                notificationChannelsCount += 1
                createNotificationChannel(isChecked, "TimerNotification$notificationChannelsCount",
                    "Уведомление по Таймеру", "Уведомление которое появляется при завершении работы таймера")
                createNotificationChannel(isChecked, "TimeNotification$notificationChannelsCount",
                    "Уведомление по Времени", "Уведомление которое появляется при включении/выключении котла, или бойлера по времени")
                createNotificationChannel(isChecked, "TemperatureNotification$notificationChannelsCount",
                "Уведомление по Температуре", "Уведомление которое появляется при включении/выключении котла по температуре")
                editPreferences.putBoolean("TimerNotificationChannelCreated", true)
                editPreferences.putBoolean("TimeNotificationChannelCreated", true)
                editPreferences.putBoolean("TemperatureNotificationChannelCreated", true)
                editPreferences.putInt("NotificationChannelsCount", notificationChannelsCount).apply()
            }
        }
        checkBoxTimerNotifications.setOnCheckedChangeListener { _, isChecked ->
            vibrate()
            editPreferences.putBoolean("TimerNotifications", isChecked).apply()
        }
        checkBoxTimeNotifications.setOnCheckedChangeListener { _, isChecked ->
            vibrate()
            editPreferences.putBoolean("TimeNotifications", isChecked).apply()
        }
        checkBoxTemperatureNotifications.setOnCheckedChangeListener { _, isChecked ->
            vibrate()
            editPreferences.putBoolean("TemperatureNotifications", isChecked).apply()
        }

        buttonDefaultNotifications.setOnClickListener {
            vibrate()
            if (checkBoxNotificationsSound.isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!sharedPreferences.getBoolean("NotificationGroupCreated", false)) {
                        notificationManager.createNotificationChannelGroup(NotificationChannelGroup("SmartHeating", "Умное Отопление"))
                        editPreferences.putBoolean("NotificationGroupCreated", true).apply()
                    }
                    var notificationChannelsCount = sharedPreferences.getInt("NotificationChannelsCount", 0)
                    notificationManager.deleteNotificationChannel("TimerNotification$notificationChannelsCount")
                    notificationManager.deleteNotificationChannel("TimeNotification$notificationChannelsCount")
                    notificationManager.deleteNotificationChannel("TemperatureNotification$notificationChannelsCount")
                    notificationChannelsCount += 1
                    createNotificationChannel(true, "TimerNotification$notificationChannelsCount",
                        "Уведомление по Таймеру", "Уведомление которое появляется при завершении работы таймера")
                    createNotificationChannel(true, "TimeNotification$notificationChannelsCount",
                        "Уведомление по Времени", "Уведомление которое появляется при включении/выключении котла, или бойлера по времени")
                    createNotificationChannel(true, "TemperatureNotification$notificationChannelsCount",
                        "Уведомление по Температуре", "Уведомление которое появляется при включении/выключении котла по температуре")
                    editPreferences.putBoolean("TimerNotificationChannelCreated", true)
                    editPreferences.putBoolean("TimeNotificationChannelCreated", true)
                    editPreferences.putBoolean("TemperatureNotificationChannelCreated", true)
                    editPreferences.putInt("NotificationChannelsCount", notificationChannelsCount)
                }
            }

            checkBoxNotificationsSound.isChecked = true
            checkBoxTimerNotifications.isChecked = true
            checkBoxTimeNotifications.isChecked = true
            checkBoxTemperatureNotifications.isChecked = true

            editPreferences.putBoolean("NotificationsSound", true)
            editPreferences.putBoolean("TimerNotifications", true)
            editPreferences.putBoolean("TimeNotifications", true)
            editPreferences.putBoolean("TemperatureNotifications", true).apply()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    private fun createNotificationChannel(isChecked : Boolean, channelId : String, channelName : String, channelDescription : String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lateinit var audioAttributes : AudioAttributes
            if (isChecked) {
                audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
            }
            val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.description = channelDescription
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(false)
            if (isChecked) {
                notificationChannel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + requireActivity().packageName + "/" + R.raw.notification), audioAttributes)
            } else {
                notificationChannel.setSound(null, null)
            }
            notificationChannel.group = "SmartHeating"
            notificationChannel.lockscreenVisibility = View.VISIBLE
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun vibrate() {
        if (isHapticFeedbackEnabled) {
            buttonDefaultNotifications.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        } else {
            val vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(20)
                }
            }
        }
    }
}