package com.arduinoworld.smartheating

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Context.JOB_SCHEDULER_SERVICE
import android.media.AudioAttributes
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.*
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*


@Suppress("DEPRECATION")
@SuppressLint("CommitPrefEdits", "SetTextI18n")
class TemperatureControl : Fragment() {
    private var isHapticFeedbackEnabled = true
    private var isOverCurrentProtectionEnabled = true
    private var isNotificationsEnabled = true
    private var isNotificationChannelCreated = false
    private var isNotificationGroupCreated = false
    private var isTemperatureNotificationsEnabled = true
    private var notificationChannelsCount = 0
    private var isUserLogged = false
    private var isTemperatureModeStarted = false
    private var heatingElementsCount = 1
    private var currentHeatingMode = 0
    private var currentBoilerMode = 0
    private var maxHeatingElementsCount = 2
    private var heatingStartTemperature = 1
    private var heatingStopTemperature = 2
    private var homeTemperature = 0
    private var temperature = 0
    private var userUID = ""

    lateinit var buttonStartTemperatureMode : Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        val editPreferences = sharedPreferences.edit()

        val firebaseAuth = FirebaseAuth.getInstance()
        val realtimeDatabase = FirebaseDatabase.getInstance().reference.child("users")

        val layoutTemperature : RelativeLayout = requireActivity().findViewById(R.id.layoutTemperature)
        val progressBarTemperature : CircularProgressIndicator = requireActivity().findViewById(R.id.progressBarTemperature)
        val labelTemperature : TextView = requireActivity().findViewById(R.id.labelTemperature)
        val labelTemperatureRange : TextView = requireActivity().findViewById(R.id.labelTemperatureRange)
        val inputTemperature : EditText = requireActivity().findViewById(R.id.inputTemperature)
        val buttonStopTemperatureMode : Button = requireActivity().findViewById(R.id.buttonStopTemperatureMode)
        val inputLayoutHeatingElementsCount : TextInputLayout = requireActivity().findViewById(R.id.inputLayoutHeatingElementsTemperature)
        val inputHeatingElementsCount : AutoCompleteTextView = requireActivity().findViewById(R.id.inputHeatingElementsTemperature)
        buttonStartTemperatureMode = requireActivity().findViewById(R.id.buttonStartTemperatureMode)

        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)
        isOverCurrentProtectionEnabled = sharedPreferences.getBoolean("OverCurrentProtectionEnabled", true)
        isNotificationsEnabled = sharedPreferences.getBoolean("NotificationsEnabled", true)
        isNotificationChannelCreated = sharedPreferences.getBoolean("TemperatureNotificationChannelCreated", false)
        isNotificationGroupCreated = sharedPreferences.getBoolean("NotificationGroupCreated", false)
        isTemperatureNotificationsEnabled = sharedPreferences.getBoolean("TemperatureNotifications", true)
        notificationChannelsCount = sharedPreferences.getInt("NotificationChannelsCount", 0)
        isTemperatureModeStarted = sharedPreferences.getBoolean("TemperatureModeStarted", false)
        isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        heatingElementsCount = sharedPreferences.getInt("HeatingElementsTemperature", 1)
        maxHeatingElementsCount = sharedPreferences.getInt("MaxHeatingElements", 2)
        heatingStartTemperature = sharedPreferences.getInt("HeatingStartTemperature", 1)
        heatingStopTemperature = sharedPreferences.getInt("HeatingStopTemperature", 2)
        homeTemperature = sharedPreferences.getInt("HomeTemperature", 0)
        currentHeatingMode = sharedPreferences.getInt("CurrentHeatingMode", 0)
        currentBoilerMode = sharedPreferences.getInt("CurrentBoilerMode", 0)

        inputTemperature.clearFocus()

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

        if (isTemperatureModeStarted) {
            inputTemperature.setText(homeTemperature.toString())
            buttonStartTemperatureMode.visibility = View.GONE
            buttonStopTemperatureMode.visibility = View.VISIBLE
            labelTemperatureRange.visibility = View.VISIBLE
            labelTemperatureRange.text = "Диапазон t: ${homeTemperature - heatingStartTemperature} - " +
                    "${homeTemperature + heatingStopTemperature}°C\nКотёл: ${isHeatingStarted()}"
        }

        if (isNetworkConnected()) {
            if (isUserLogged) {
                userUID = firebaseAuth.currentUser!!.uid
                realtimeDatabase.child(userUID).child("temperature").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        layoutTemperature.visibility = View.VISIBLE
                        temperature = snapshot.getValue(Int::class.java)!!
                        labelTemperature.text = "$temperature°C"
                        progressBarTemperature.progress = temperature
                    }
                    override fun onCancelled(error: DatabaseError) {
                        if (sharedPreferences.getBoolean("UserLogged", false)) {
                            Toast.makeText(activity, "Не удалось получить температуру!", Toast.LENGTH_LONG).show()
                        }
                    }
                })
            }
        }

        inputHeatingElementsCount.setOnItemClickListener { _, _, position, _ ->
            vibrate()
            if (!isTemperatureModeStarted) {
                heatingElementsCount = position + 1
                editPreferences.putInt("HeatingElementsTemperature", heatingElementsCount).apply()
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
                Toast.makeText(activity, "Вы не можете изменять \n количество тэнов, пока \n запущен режим!", Toast.LENGTH_LONG).show()
            }
        }

        buttonStartTemperatureMode.setOnClickListener {
            vibrate()
            hideKeyboard()
            inputTemperature.clearFocus()
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    if (currentHeatingMode == 0) {
                        if (isOverCurrentProtectionEnabled && heatingElementsCount == maxHeatingElementsCount && (sharedPreferences.getBoolean("BoilerStarted", false) || currentBoilerMode == 1 || currentBoilerMode == 2)) {
                            overCurrentProtection()
                        } else {
                            if (inputTemperature.text.isNotEmpty()) {
                                homeTemperature = inputTemperature.text.toString().toInt()
                                buttonStartTemperatureMode.visibility = View.GONE
                                buttonStopTemperatureMode.visibility = View.VISIBLE
                                labelTemperatureRange.visibility = View.VISIBLE
                                labelTemperatureRange.text = "Диапазон t: ${homeTemperature - heatingStartTemperature} - " +
                                        "${homeTemperature + heatingStopTemperature}°C\nКотёл: ${isHeatingStarted()}"
                                editPreferences.putBoolean("TemperatureModeStarted", true)
                                editPreferences.putInt("CurrentHeatingMode", 3)
                                editPreferences.putInt("HomeTemperature", homeTemperature).apply()
                                realtimeDatabase.child(userUID).child("heatingElements").setValue(heatingElementsCount)
                                Handler().postDelayed({
                                    realtimeDatabase.child(userUID).child("temperatureRange").setValue((homeTemperature - heatingStartTemperature).toString() + " " + (homeTemperature + heatingStopTemperature).toString())
                                }, 200)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val notificationManager = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    if (!isNotificationGroupCreated) {
                                        notificationManager.createNotificationChannelGroup(NotificationChannelGroup("SmartHeating", "Умное Отопление"))
                                        editPreferences.putBoolean("NotificationGroupCreated", true).apply()
                                    }
                                    if (!isNotificationChannelCreated) {
                                        val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                                        val notificationChannel = NotificationChannel("TemperatureNotification$notificationChannelsCount", "Уведомление по Температуре", NotificationManager.IMPORTANCE_DEFAULT)
                                        notificationChannel.description = "Уведомление которое появляется при включении/выключении котла по температуре"
                                        notificationChannel.enableLights(true)
                                        notificationChannel.enableVibration(false)
                                        notificationChannel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + requireActivity().packageName + "/" + R.raw.notification), audioAttributes)
                                        notificationChannel.group = "SmartHeating"
                                        notificationChannel.lockscreenVisibility = View.VISIBLE
                                        notificationManager.createNotificationChannel(notificationChannel)
                                        editPreferences.putBoolean("TemperatureNotificationChannelCreated", true).apply()
                                    }
                                }
                                if (isTemperatureNotificationsEnabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        val builder = JobInfo.Builder(9428, ComponentName(requireActivity(), TemperatureJob::class.java))
                                        builder.setMinimumLatency((5 * 1000).toLong())
                                        builder.setOverrideDeadline((15 * 1000).toLong())
                                        val jobScheduler = requireActivity().getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
                                        jobScheduler.schedule(builder.build())
                                    }
                                }
                            } else {
                                Toast.makeText(activity, "Введите температуру в доме!", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        printHeatingMode()
                    }
                } else {
                    Toast.makeText(activity, "Вы не вошли в пользователя!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonStopTemperatureMode.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    homeTemperature = inputTemperature.text.toString().toInt()
                    buttonStartTemperatureMode.visibility = View.VISIBLE
                    buttonStopTemperatureMode.visibility = View.GONE
                    labelTemperatureRange.visibility = View.GONE
                    labelTemperatureRange.text = "Диапазон t: ${homeTemperature - heatingStartTemperature} - " +
                            "${homeTemperature + heatingStopTemperature}°C\nКотёл: ${isHeatingStarted()}"
                    currentHeatingMode
                    editPreferences.putInt("CurrentHeatingMode", 0)
                    editPreferences.putBoolean("TemperatureModeStarted", false).apply()
                    realtimeDatabase.child(userUID).child("temperatureRange").setValue("0")
                    if (isTemperatureNotificationsEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val jobScheduler = requireActivity().getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
                            jobScheduler.cancel(9428)
                        }
                    }
                } else {
                    Toast.makeText(activity, "Вы не вошли в пользователя!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_temperature_control, container, false)
    }

    private fun isHeatingStarted() : String {
        return if (temperature >= homeTemperature - heatingStartTemperature && temperature <= homeTemperature + heatingStopTemperature) {
            "включен"
        } else {
            "выключен"
        }
    }

    private fun printHeatingMode() {
        when (currentHeatingMode) {
            1 -> {
                Toast.makeText(activity, "Сейчас запущен таймер!", Toast.LENGTH_LONG).show()
            }
            2 -> {
                Toast.makeText(activity, "Сейчас запущен режим \n включения и выключения \n по времени!", Toast.LENGTH_LONG).show()
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

    private fun hideKeyboard() {
        val view = requireActivity().currentFocus
        if (view != null) {
            val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun isNetworkConnected() : Boolean {
        val connectivityManager = requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state == NetworkInfo.State.CONNECTED
    }

    private fun vibrate() {
        if (isHapticFeedbackEnabled) {
            buttonStartTemperatureMode.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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