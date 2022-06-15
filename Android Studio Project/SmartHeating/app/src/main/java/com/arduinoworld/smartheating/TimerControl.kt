package com.arduinoworld.smartheating

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.*
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


@Suppress("DEPRECATION", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@SuppressLint("DEPRECATION", "ClickableViewAccessibility", "SetTextI18n", "CommitPrefEdits", "SimpleDateFormat")
class TimerControl : Fragment() {
    private var isHapticFeedbackEnabled = true
    private var isOverCurrentProtectionEnabled = true
    private var isNotificationChannelCreated = false
    private var isNotificationGroupCreated = false
    private var isTimerNotificationsEnabled = true
    private var notificationChannelsCount = 0
    private var heatingOrBoiler = true
    private var isTimerFinished = false
    private var isHeatingTimerStarted = false
    private var startHeatingTimer = false
    private var isBoilerTimerStarted = false
    private var startBoilerTimer = false
    private var decreaseBlocked = true
    private var isUserLogged = false

    private var fragmentDestroyHeatingTimeLeft : Long = 0
    private var fragmentDestroyBoilerTimeLeft : Long = 0
    private var heatingTimeLeftInMillis : Long = 0
    private var boilerTimeLeftInMillis : Long = 0
    private var heatingTimerTime = 1
    private var boilerTimerTime = 1
    private var currentHeatingMode = 0
    private var currentBoilerMode = 0
    private var heatingElementsCount = 1
    private var maxHeatingElementsCount = 2
    private var userUID = ""

    lateinit var heatingTimer : CountDownTimer
    lateinit var boilerTimer : CountDownTimer
    lateinit var buttonStartTimer : Button
    lateinit var buttonStopTimer : Button
    lateinit var sliderTimerTime : SeekBar
    lateinit var layoutTimerTime : RelativeLayout
    lateinit var labelHeatingTimeLeft : TextView
    lateinit var labelBoilerTimeLeft : TextView
    lateinit var inputHeatingElementsCount : AutoCompleteTextView
    lateinit var powerManager : PowerManager
    lateinit var wakeLock : PowerManager.WakeLock
    lateinit var fragmentOpenTime : Date
    lateinit var fragmentDestroyTime : Date
    lateinit var dateFormat : SimpleDateFormat
    lateinit var editPreferences : SharedPreferences.Editor

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        editPreferences = sharedPreferences.edit()

        val firebaseAuth = FirebaseAuth.getInstance()
        val realtimeDatabase = FirebaseDatabase.getInstance().reference.child("users")

        powerManager = requireActivity().getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "SmartHeating:TimerNotificationLock")

        val buttonIncrease : ImageButton = requireActivity().findViewById(R.id.buttonIncrease)
        val buttonDecrease : ImageButton = requireActivity().findViewById(R.id.buttonDecrease)
        val labelTimerTime : TextView = requireActivity().findViewById(R.id.labelTimerTime)
        val inputHeatingOrBoiler : AutoCompleteTextView = requireActivity().findViewById(R.id.inputTimerHeatingOrBoiler)
        val inputLayoutHeatingElementsCount : TextInputLayout = requireActivity().findViewById(R.id.inputLayoutHeatingElementsTimer)

        buttonStartTimer = requireActivity().findViewById(R.id.buttonStartTimer)
        buttonStopTimer = requireActivity().findViewById(R.id.buttonStopTimer)
        sliderTimerTime = requireActivity().findViewById(R.id.sliderTimerTime)
        layoutTimerTime = requireActivity().findViewById(R.id.layoutTimerTime)
        labelHeatingTimeLeft = requireActivity().findViewById(R.id.labelHeatingTimeLeft)
        labelBoilerTimeLeft = requireActivity().findViewById(R.id.labelBoilerTimeLeft)
        inputHeatingElementsCount = requireActivity().findViewById(R.id.inputHeatingElementsTimer)

        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)
        isOverCurrentProtectionEnabled = sharedPreferences.getBoolean("OverCurrentProtectionEnabled", true)
        isNotificationChannelCreated = sharedPreferences.getBoolean("TimerNotificationChannelCreated", false)
        isNotificationGroupCreated = sharedPreferences.getBoolean("NotificationGroupCreated", false)
        isTimerNotificationsEnabled = sharedPreferences.getBoolean("TimerNotifications", true)
        notificationChannelsCount = sharedPreferences.getInt("NotificationChannelsCount", 0)
        heatingOrBoiler = sharedPreferences.getBoolean("TimerHeatingOrBoiler", true)
        isHeatingTimerStarted = sharedPreferences.getBoolean("HeatingTimerStarted", false)
        isBoilerTimerStarted = sharedPreferences.getBoolean("BoilerTimerStarted", false)
        isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        heatingTimerTime = sharedPreferences.getInt("HeatingTimerTime", 1)
        boilerTimerTime = sharedPreferences.getInt("BoilerTimerTime", 1)
        heatingElementsCount = sharedPreferences.getInt("HeatingElementsTimer", 1)
        maxHeatingElementsCount = sharedPreferences.getInt("MaxHeatingElements", 2)
        currentHeatingMode = sharedPreferences.getInt("CurrentHeatingMode", 0)
        currentBoilerMode = sharedPreferences.getInt("CurrentBoilerMode", 0)
        dateFormat = SimpleDateFormat("dd/M/yyyy hh:mm:ss")

        if (isUserLogged) {
            userUID = firebaseAuth.currentUser!!.uid
        }

        if (maxHeatingElementsCount == 1) {
            inputLayoutHeatingElementsCount.visibility = View.GONE
        } else {
            val heatingElementsCountList = ArrayList<String>()
            for (i in 1..maxHeatingElementsCount) {
                when {
                    i == 1 -> {
                        heatingElementsCountList.add("Включить 1 тэн котла")
                    }
                    i in 2..4 -> {
                        heatingElementsCountList.add("Включить $i тэна котла")
                    }
                    i >= 5 -> {
                        heatingElementsCountList.add("Включить $i тэнов котла")
                    }
                }
            }
            val heatingElementsCountArrayAdapter = ArrayAdapter(requireActivity().applicationContext, R.layout.dropdown_menu_item, heatingElementsCountList)
            inputHeatingElementsCount.setAdapter(heatingElementsCountArrayAdapter)
            inputHeatingElementsCount.threshold = 1
        }

        val heatingOrBoilerList = listOf("Управление Котлом", "Управление Бойлером")
        val heatingOrBoilerArrayAdapter = ArrayAdapter(requireActivity().applicationContext, R.layout.dropdown_menu_item, heatingOrBoilerList)
        inputHeatingOrBoiler.setAdapter(heatingOrBoilerArrayAdapter)
        inputHeatingOrBoiler.threshold = 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            when {
                heatingElementsCount == 1 -> {
                    inputHeatingElementsCount.setText("Включить 1 тэн котла", false)
                }
                heatingElementsCount in 2..4 -> {
                    inputHeatingElementsCount.setText("Включить $heatingElementsCount тэна котла", false)
                }
                heatingElementsCount >= 5 -> {
                    inputHeatingElementsCount.setText("Включить $heatingElementsCount тэнов котла", false)
                }
            }
        }

        if (heatingOrBoiler) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                inputHeatingOrBoiler.setText("Управление Котлом", false)
            }
            sliderTimerTime.max = 299
            sliderTimerTime.progress = heatingTimerTime - 1
            labelTimerTime.text = "$heatingTimerTime мин"
            if (heatingTimerTime == 1) {
                decreaseBlocked = true
                buttonDecrease.setImageResource(R.drawable.arrow_down_blocked_icon)
            } else {
                decreaseBlocked = false
                buttonDecrease.setImageResource(R.drawable.arrow_down_icon)
            }
            if (isHeatingTimerStarted) {
                buttonStartTimer.visibility = View.GONE
                buttonStopTimer.visibility = View.VISIBLE
                layoutTimerTime.visibility = View.GONE
                sliderTimerTime.visibility = View.GONE
                labelHeatingTimeLeft.visibility = View.VISIBLE
                fragmentDestroyHeatingTimeLeft = sharedPreferences.getLong("HeatingTimeLeft", 0)
                fragmentDestroyTime = dateFormat.parse(sharedPreferences.getString("FragmentDestroyTime", ""))
                fragmentOpenTime = dateFormat.parse(dateFormat.format(Calendar.getInstance().time))
                heatingTimeLeftInMillis = fragmentDestroyHeatingTimeLeft - (kotlin.math.abs(fragmentOpenTime.time - fragmentDestroyTime.time))
                if (heatingTimeLeftInMillis < 0) isTimerFinished = true
                startHeatingTimer()
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                inputHeatingOrBoiler.setText("Управление Бойлером", false)
            }
            sliderTimerTime.max = 299
            sliderTimerTime.progress = boilerTimerTime - 1
            labelTimerTime.text = "$boilerTimerTime мин"
            inputLayoutHeatingElementsCount.visibility = View.GONE
            if (boilerTimerTime == 1) {
                decreaseBlocked = true
                buttonDecrease.setImageResource(R.drawable.arrow_down_blocked_icon)
            } else {
                decreaseBlocked = false
                buttonDecrease.setImageResource(R.drawable.arrow_down_icon)
            }
            if (isBoilerTimerStarted) {
                buttonStartTimer.visibility = View.GONE
                buttonStopTimer.visibility = View.VISIBLE
                layoutTimerTime.visibility = View.GONE
                sliderTimerTime.visibility = View.GONE
                labelBoilerTimeLeft.visibility = View.VISIBLE
                fragmentDestroyBoilerTimeLeft = sharedPreferences.getLong("BoilerTimeLeft", 0)
                fragmentDestroyTime = dateFormat.parse(sharedPreferences.getString("FragmentDestroyTime", ""))
                fragmentOpenTime = dateFormat.parse(dateFormat.format(Calendar.getInstance().time))
                boilerTimeLeftInMillis = fragmentDestroyBoilerTimeLeft - (kotlin.math.abs(fragmentOpenTime.time - fragmentDestroyTime.time))
                if (boilerTimeLeftInMillis < 0) isTimerFinished = true
                startBoilerTimer()
            }
        }

        if (heatingOrBoiler && isBoilerTimerStarted) {
            startBoilerTimer = true
        } else if (!heatingOrBoiler && isHeatingTimerStarted) {
            startHeatingTimer = true
        }

        inputHeatingOrBoiler.setOnItemClickListener { _, _, position, _ ->
            vibrate()
            if (position == 0) {
                heatingOrBoiler = true
                labelBoilerTimeLeft.visibility = View.GONE
                if (maxHeatingElementsCount != 1) {
                    inputLayoutHeatingElementsCount.visibility = View.VISIBLE
                }
                sliderTimerTime.max = 299
                sliderTimerTime.progress = heatingTimerTime - 1
                labelTimerTime.text = "$heatingTimerTime мин"
                if (isHeatingTimerStarted) {
                    buttonStartTimer.visibility = View.GONE
                    buttonStopTimer.visibility = View.VISIBLE
                    layoutTimerTime.visibility = View.GONE
                    sliderTimerTime.visibility = View.GONE
                    labelHeatingTimeLeft.visibility = View.VISIBLE
                    if (startHeatingTimer) {
                        fragmentDestroyHeatingTimeLeft = sharedPreferences.getLong("HeatingTimeLeft", 0)
                        fragmentDestroyTime = dateFormat.parse(sharedPreferences.getString("FragmentDestroyTime", ""))
                        fragmentOpenTime = dateFormat.parse(dateFormat.format(Calendar.getInstance().time))
                        heatingTimeLeftInMillis = fragmentDestroyHeatingTimeLeft - (kotlin.math.abs(fragmentOpenTime.time - fragmentDestroyTime.time))
                        if (heatingTimeLeftInMillis < 0) isTimerFinished = true
                        startHeatingTimer = false
                        startHeatingTimer()
                    }
                } else {
                    buttonStartTimer.visibility = View.VISIBLE
                    buttonStopTimer.visibility = View.GONE
                    layoutTimerTime.visibility = View.VISIBLE
                    sliderTimerTime.visibility = View.VISIBLE
                }
            } else {
                heatingOrBoiler = false
                labelHeatingTimeLeft.visibility = View.GONE
                inputLayoutHeatingElementsCount.visibility = View.GONE
                sliderTimerTime.max = 299
                sliderTimerTime.progress = boilerTimerTime - 1
                labelTimerTime.text = "$boilerTimerTime мин"
                if (isBoilerTimerStarted) {
                    buttonStartTimer.visibility = View.GONE
                    buttonStopTimer.visibility = View.VISIBLE
                    layoutTimerTime.visibility = View.GONE
                    sliderTimerTime.visibility = View.GONE
                    labelBoilerTimeLeft.visibility = View.VISIBLE
                    if (startBoilerTimer) {
                        fragmentDestroyBoilerTimeLeft = sharedPreferences.getLong("BoilerTimeLeft", 0)
                        fragmentDestroyTime = dateFormat.parse(sharedPreferences.getString("FragmentDestroyTime", ""))
                        fragmentOpenTime = dateFormat.parse(dateFormat.format(Calendar.getInstance().time))
                        boilerTimeLeftInMillis = fragmentDestroyBoilerTimeLeft - (kotlin.math.abs(fragmentOpenTime.time - fragmentDestroyTime.time))
                        if (boilerTimeLeftInMillis < 0) isTimerFinished = true
                        startBoilerTimer = false
                        startBoilerTimer()
                    }
                } else {
                    buttonStartTimer.visibility = View.VISIBLE
                    buttonStopTimer.visibility = View.GONE
                    layoutTimerTime.visibility = View.VISIBLE
                    sliderTimerTime.visibility = View.VISIBLE
                }
            }
            editPreferences.putBoolean("TimerHeatingOrBoiler", heatingOrBoiler).apply()
        }

        inputHeatingElementsCount.setOnItemClickListener { _, _, position, _ ->
            vibrate()
            if (!isHeatingTimerStarted) {
                heatingElementsCount = position + 1
                editPreferences.putInt("HeatingElementsTimer", heatingElementsCount).apply()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    when {
                        heatingElementsCount == 1 -> {
                            inputHeatingElementsCount.setText("Включить 1 тэн котла", false)
                        }
                        heatingElementsCount in 2..4 -> {
                            inputHeatingElementsCount.setText("Включить $heatingElementsCount тэна котла", false)
                        }
                        heatingElementsCount >= 5 -> {
                            inputHeatingElementsCount.setText("Включить $heatingElementsCount тэнов котла", false)
                        }
                    }
                }
                Toast.makeText(activity, "Вы не можете изменять \n количество тэнов, пока \n запущен таймер!", Toast.LENGTH_LONG).show()
            }
        }

        buttonIncrease.setOnClickListener {
            vibrate()
            if (heatingOrBoiler) {
                heatingTimerTime += 1
                sliderTimerTime.max = 299
                sliderTimerTime.progress = heatingTimerTime - 1
                labelTimerTime.text = "$heatingTimerTime мин"
                if (heatingTimerTime > 1) {
                    decreaseBlocked = false
                    buttonDecrease.setImageResource(R.drawable.arrow_down_icon)
                }
                editPreferences.putInt("HeatingTimeLeft", heatingTimerTime).apply()
            } else {
                boilerTimerTime += 1
                sliderTimerTime.max = 299
                sliderTimerTime.progress = boilerTimerTime - 1
                labelTimerTime.text = "$boilerTimerTime мин"
                if (boilerTimerTime > 1) {
                    decreaseBlocked = false
                    buttonDecrease.setImageResource(R.drawable.arrow_down_icon)
                }
                editPreferences.putInt("BoilerTimeLeft", boilerTimerTime).apply()
            }
        }

        buttonDecrease.setOnClickListener {
            vibrate()
            if (!decreaseBlocked) {
                if (heatingOrBoiler) {
                    heatingTimerTime -= 1
                    sliderTimerTime.max = 299
                    sliderTimerTime.progress = heatingTimerTime - 1
                    labelTimerTime.text = "$heatingTimerTime мин"
                    if (heatingTimerTime > 1) {
                        decreaseBlocked = false
                        buttonDecrease.setImageResource(R.drawable.arrow_down_icon)
                    }
                    editPreferences.putInt("HeatingTimeLeft", heatingTimerTime).apply()
                } else {
                    boilerTimerTime -= 1
                    sliderTimerTime.max = 299
                    sliderTimerTime.progress = boilerTimerTime - 1
                    labelTimerTime.text = "$boilerTimerTime мин"
                    if (boilerTimerTime > 1) {
                        decreaseBlocked = false
                        buttonDecrease.setImageResource(R.drawable.arrow_down_icon)
                    }
                    editPreferences.putInt("BoilerTimeLeft", boilerTimerTime).apply()
                }
            } else {
                Toast.makeText(activity, "Вы установили минимальное \n время работы таймера!", Toast.LENGTH_LONG).show()
            }
        }

        buttonStartTimer.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    if (heatingOrBoiler) {
                        if (currentHeatingMode == 0) {
                            if (isOverCurrentProtectionEnabled && heatingElementsCount == maxHeatingElementsCount && (sharedPreferences.getBoolean("BoilerStarted", false) || isBoilerTimerStarted || currentBoilerMode == 2 || currentBoilerMode == 3)) {
                                overCurrentProtection()
                            } else {
                                buttonStartTimer.visibility = View.GONE
                                buttonStopTimer.visibility = View.VISIBLE
                                layoutTimerTime.visibility = View.GONE
                                sliderTimerTime.visibility = View.GONE
                                labelHeatingTimeLeft.visibility = View.VISIBLE
                                isHeatingTimerStarted = true
                                heatingTimeLeftInMillis = (heatingTimerTime * 60000).toLong()
                                editPreferences.putBoolean("HeatingTimerStarted", true)
                                editPreferences.putInt("HeatingTimerTime", heatingTimerTime)
                                editPreferences.putInt("CurrentHeatingMode", 1).apply()
                                startHeatingTimer()
                                realtimeDatabase.child(userUID).child("heatingElements").setValue(heatingElementsCount)
                                Handler().postDelayed({
                                    realtimeDatabase.child(userUID).child("heatingTimerTime").setValue(heatingTimerTime)
                                }, 200)
                            }
                        } else {
                            printHeatingMode()
                        }
                    } else {
                        if (currentBoilerMode == 0) {
                            if (isOverCurrentProtectionEnabled && heatingElementsCount == maxHeatingElementsCount &&
                                ((sharedPreferences.getBoolean("HeatingStarted", false) && sharedPreferences.getInt("HeatingElements", 1) == maxHeatingElementsCount) || isHeatingTimerStarted ||
                                (currentHeatingMode == 2 && sharedPreferences.getBoolean("HeatingElementsTimeContains2", false)) ||
                                (currentBoilerMode == 3 && sharedPreferences.getInt("HeatingElementsTemperature", 1) == maxHeatingElementsCount))) {
                                overCurrentProtection()
                            } else {
                                buttonStartTimer.visibility = View.GONE
                                buttonStopTimer.visibility = View.VISIBLE
                                layoutTimerTime.visibility = View.GONE
                                sliderTimerTime.visibility = View.GONE
                                labelBoilerTimeLeft.visibility = View.VISIBLE
                                isBoilerTimerStarted = true
                                boilerTimeLeftInMillis = (boilerTimerTime * 60000).toLong()
                                editPreferences.putBoolean("BoilerTimerStarted", true)
                                editPreferences.putInt("BoilerTimerTime", boilerTimerTime)
                                editPreferences.putInt("CurrentBoilerMode", 1).apply()
                                realtimeDatabase.child(userUID).child("boilerTimerTime").setValue(boilerTimerTime)
                                startBoilerTimer()
                            }
                        } else {
                            printBoilerMode()
                        }
                    }
                } else {
                    Toast.makeText(activity, "Вы не вошли в пользователя!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonStopTimer.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    if (heatingOrBoiler) {
                        heatingTimer.cancel()
                        buttonStartTimer.visibility = View.VISIBLE
                        buttonStopTimer.visibility = View.GONE
                        layoutTimerTime.visibility = View.VISIBLE
                        sliderTimerTime.visibility = View.VISIBLE
                        labelHeatingTimeLeft.visibility = View.GONE
                        isHeatingTimerStarted = false
                        currentHeatingMode = 0
                        editPreferences.putBoolean("HeatingTimerStarted", false)
                        editPreferences.putInt("CurrentHeatingMode", 0).apply()
                        realtimeDatabase.child(userUID).child("heatingTimerTime").setValue(0)
                    } else {
                        boilerTimer.cancel()
                        buttonStartTimer.visibility = View.VISIBLE
                        buttonStopTimer.visibility = View.GONE
                        layoutTimerTime.visibility = View.VISIBLE
                        sliderTimerTime.visibility = View.VISIBLE
                        labelBoilerTimeLeft.visibility = View.GONE
                        isBoilerTimerStarted = false
                        currentBoilerMode = 0
                        editPreferences.putBoolean("BoilerTimerStarted", false)
                        editPreferences.putInt("CurrentBoilerMode", 0).apply()
                        realtimeDatabase.child(userUID).child("boilerTimerTime").setValue(0)
                    }
                } else {
                    Toast.makeText(activity, "Вы не вошли в пользователя!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        sliderTimerTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, position: Int, p2: Boolean) {
                if (heatingOrBoiler) {
                    heatingTimerTime = position + 1
                    labelTimerTime.text = "$heatingTimerTime мин"
                    if (heatingTimerTime == 1) {
                        decreaseBlocked = true
                        buttonDecrease.setImageResource(R.drawable.arrow_down_blocked_icon)
                    } else {
                        decreaseBlocked = false
                        buttonDecrease.setImageResource(R.drawable.arrow_down_icon)
                    }
                } else {
                    boilerTimerTime = position + 1
                    labelTimerTime.text = "$boilerTimerTime мин"
                    if (boilerTimerTime == 1) {
                        decreaseBlocked = true
                        buttonDecrease.setImageResource(R.drawable.arrow_down_blocked_icon)
                    } else {
                        decreaseBlocked = false
                        buttonDecrease.setImageResource(R.drawable.arrow_down_icon)
                    }
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                vibrate()
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                if (heatingOrBoiler) {
                    editPreferences.putInt("HeatingTimerTime", heatingTimerTime).apply()
                } else {
                    editPreferences.putInt("BoilerTimerTime", heatingTimerTime).apply()
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timer_control, container, false)
    }

    private fun startHeatingTimer() {
        heatingTimer = object : CountDownTimer(heatingTimeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                heatingTimeLeftInMillis = millisUntilFinished
                if (heatingTimerTime <= 60) {
                    val minutes = (heatingTimeLeftInMillis / 1000).toInt() / 60
                    val seconds = (heatingTimeLeftInMillis / 1000).toInt() % 60
                    labelHeatingTimeLeft.text = "00:${java.lang.String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)}"
                } else {
                    val hours = (heatingTimeLeftInMillis / 1000).toInt() / 3600
                    val minutes = (heatingTimeLeftInMillis / 1000 % 3600).toInt() / 60
                    val seconds = (heatingTimeLeftInMillis / 1000).toInt() % 60
                    labelHeatingTimeLeft.text = "0${java.lang.String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)}"
                }
            }

            override fun onFinish() {
                Toast.makeText(activity, "Время таймера \n котла истекло!", Toast.LENGTH_SHORT).show()
                buttonStartTimer.visibility = View.VISIBLE
                buttonStopTimer.visibility = View.GONE
                layoutTimerTime.visibility = View.VISIBLE
                sliderTimerTime.visibility = View.VISIBLE
                labelHeatingTimeLeft.visibility = View.GONE
                isHeatingTimerStarted = false
                currentHeatingMode = 0
                editPreferences.putBoolean("HeatingTimerStarted", false)
                editPreferences.putInt("CurrentHeatingMode", 0).apply()
                createNotificationChannelOrGroup()
                if (isTimerNotificationsEnabled) {
                    val builder = NotificationCompat.Builder(requireActivity(), "TimerNotification$notificationChannelsCount")
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("Умное Отопление")
                            .setContentText("Время таймера котла истекло!")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                    with(NotificationManagerCompat.from(requireActivity())) {
                        notify(1498, builder.build())
                    }
                    if (if (Build.VERSION.SDK_INT >= 20) !powerManager.isInteractive else !powerManager.isScreenOn) {
                        wakeLock.acquire(3000)
                        wakeLock.release()
                    }
                }
            }
        }.start()
    }

    private fun startBoilerTimer() {
        boilerTimer = object : CountDownTimer(boilerTimeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                boilerTimeLeftInMillis = millisUntilFinished
                if (boilerTimerTime <= 60) {
                    val minutes = (boilerTimeLeftInMillis / 1000).toInt() / 60
                    val seconds = (boilerTimeLeftInMillis / 1000).toInt() % 60
                    labelBoilerTimeLeft.text = "00:${java.lang.String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)}"
                } else {
                    val hours = (boilerTimeLeftInMillis / 1000).toInt() / 3600
                    val minutes = (boilerTimeLeftInMillis / 1000 % 3600).toInt() / 60
                    val seconds = (boilerTimeLeftInMillis / 1000).toInt() % 60
                    labelBoilerTimeLeft.text = "0${java.lang.String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)}"
                }
            }

            override fun onFinish() {
                Toast.makeText(activity, "Время таймера \n бойлера истекло!", Toast.LENGTH_SHORT).show()
                buttonStartTimer.visibility = View.VISIBLE
                buttonStopTimer.visibility = View.GONE
                layoutTimerTime.visibility = View.VISIBLE
                sliderTimerTime.visibility = View.VISIBLE
                labelBoilerTimeLeft.visibility = View.GONE
                isBoilerTimerStarted = false
                currentBoilerMode = 0
                editPreferences.putBoolean("BoilerTimerStarted", false)
                editPreferences.putInt("CurrentBoilerMode", 0).apply()
                createNotificationChannelOrGroup()
                if (isTimerNotificationsEnabled) {
                    val builder = NotificationCompat.Builder(requireActivity(), "TimerNotification$notificationChannelsCount")
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("Умное Отопление")
                            .setContentText("Время таймера бойлера истекло!")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                    with(NotificationManagerCompat.from(requireActivity())) {
                        notify(2876, builder.build())
                    }
                    if (if (Build.VERSION.SDK_INT >= 20) !powerManager.isInteractive else !powerManager.isScreenOn) {
                        wakeLock.acquire(3000)
                        wakeLock.release()
                    }
                }
            }
        }.start()
    }

    override fun onPause() {
        super.onPause()

        if (isHeatingTimerStarted) {
            editPreferences.putLong("HeatingTimeLeft", heatingTimeLeftInMillis)
        }
        if (isBoilerTimerStarted) {
            editPreferences.putLong("BoilerTimeLeft", boilerTimeLeftInMillis)
        }
        val dateFormat = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        editPreferences.putString("FragmentDestroyTime", dateFormat.format(Calendar.getInstance().time)).apply()
    }

    private fun createNotificationChannelOrGroup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!isNotificationGroupCreated) {
                notificationManager.createNotificationChannelGroup(NotificationChannelGroup("SmartHeating", "Умное Отопление"))
                editPreferences.putBoolean("NotificationGroupCreated", true).apply()
            }
            if (!isNotificationChannelCreated) {
                val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                val notificationChannel = NotificationChannel("TimerNotification$notificationChannelsCount", "Уведомление по Таймеру", NotificationManager.IMPORTANCE_DEFAULT)
                notificationChannel.description = "Уведомление которое появляется при завершении работы таймера"
                notificationChannel.enableLights(true)
                notificationChannel.enableVibration(false)
                notificationChannel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + requireActivity().packageName + "/" + R.raw.notification), audioAttributes)
                notificationChannel.group = "SmartHeating"
                notificationChannel.lockscreenVisibility = View.VISIBLE
                notificationManager.createNotificationChannel(notificationChannel)
                editPreferences.putBoolean("TimerNotificationChannelCreated", true).apply()
            }
        }
    }

    private fun overCurrentProtection() {
        val alertDialogOverCurrentProtectionBuilder = AlertDialog.Builder(requireActivity())
        alertDialogOverCurrentProtectionBuilder.setTitle("Защита от перегрузки сети")
        alertDialogOverCurrentProtectionBuilder.setMessage("Вы не можете одновременно включать котёл на максимальную " +
                "мощность и бойлер! Это может привести к перегрузке электросети. Вы можете отключить защиту " +
                "от перегрузки в настройках.")
        alertDialogOverCurrentProtectionBuilder.setPositiveButton("Настройки") { _, _ ->
            vibrate()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                (activity as AppCompatActivity).supportActionBar?.title = "Настройки"
            }
            requireActivity().navigationView.setCheckedItem(R.id.buttonSettings)
            val fragmentManager = requireActivity().supportFragmentManager.beginTransaction()
            fragmentManager.replace(R.id.frameLayout, Settings()).commit()
        }
        alertDialogOverCurrentProtectionBuilder.setNegativeButton("Отмена") { _, _ ->
            vibrate()
        }
        val alertDialogOverCurrentProtection = alertDialogOverCurrentProtectionBuilder.create()
        alertDialogOverCurrentProtection.show()
    }

    private fun printHeatingMode() {
        when (currentHeatingMode) {
            2 -> {
                Toast.makeText(activity, "Сейчас запущен режим \n включения и выключения \n по времени!", Toast.LENGTH_LONG).show()
            }
            3 -> {
                Toast.makeText(activity, "Сейчас запущен режим \n включения и выключения \n по температуре!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun printBoilerMode() {
        when (currentBoilerMode) {
            2 -> {
                Toast.makeText(activity, "Сейчас запущен режим \n включения и выключения \n по времени!", Toast.LENGTH_LONG).show()
            }
            3 -> {
                Toast.makeText(activity, "Сейчас запущен режим \n включения и выключения \n по температуре!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isNetworkConnected() : Boolean {
        val connectivityManager = requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state == NetworkInfo.State.CONNECTED
    }

    private fun vibrate() {
        if (isHapticFeedbackEnabled) {
            buttonStartTimer.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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