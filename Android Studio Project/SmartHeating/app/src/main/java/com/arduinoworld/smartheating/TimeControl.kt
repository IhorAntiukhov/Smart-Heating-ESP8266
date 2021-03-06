package com.arduinoworld.smartheating

import android.annotation.SuppressLint
import android.app.*
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.*
import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_time_control.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("SimpleDateFormat")
@Suppress("DEPRECATION", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class TimeControl : Fragment() {
    private var isHapticFeedbackEnabled = true
    private var isOverCurrentProtectionEnabled = true
    private var isNotificationChannelCreated = false
    private var isNotificationGroupCreated = false
    private var isTimeNotificationsEnabled = true
    private var notificationChannelsCount = 0
    private var isUserLogged = false
    private var heatingOrBoiler = true
    private var heatingTimestampType = false
    private var boilerTimestampType = false
    private var isHeatingTimeModeStarted = false
    private var isBoilerTimeModeStarted = false
    private var heatingBroadcastIds = 0
    private var boilerBroadcastIds = 99
    private var heatingElementsCount = 1
    private var maxHeatingElementsCount = 2
    private var currentHeatingMode = 0
    private var currentBoilerMode = 0
    private var heatingTimestampsArrayListItemCount = 0
    private var boilerTimestampsArrayListItemCount = 0
    private var userUID = ""

    private var heatingTimestampsArrayList = ArrayList<String>()
    private var boilerTimestampsArrayList = ArrayList<String>()
    private var heatingTimestampTypesArrayList = ArrayList<Boolean>()
    private var boilerTimestampTypesArrayList = ArrayList<Boolean>()
    private var heatingElementsArrayList = ArrayList<Int>()

    private var heatingTimestampsArrayListJson = ""
    private var boilerTimestampsArrayListJson = ""
    private var heatingTimestampTypesArrayListJson = ""
    private var boilerTimestampTypesArrayListJson = ""
    private var heatingElementsArrayListJson = ""

    private lateinit var heatingRecyclerAdapter : HeatingRecyclerAdapter
    lateinit var boilerRecyclerAdapter : BoilerRecyclerAdapter

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        val editPreferences = sharedPreferences.edit()

        val firebaseAuth = FirebaseAuth.getInstance()
        val realtimeDatabase = FirebaseDatabase.getInstance().reference.child("users")

        val alarmManager : AlarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val gson = Gson()

        val recyclerViewHeatingTimestamps : RecyclerView = requireActivity().findViewById(R.id.recyclerViewHeatingTimestamps)
        val recyclerViewBoilerTimestamps : RecyclerView = requireActivity().findViewById(R.id.recyclerViewBoilerTimestamps)
        val scrollViewHeating : ScrollView = requireActivity().findViewById(R.id.scrollViewHeating)
        val scrollViewBoiler : ScrollView = requireActivity().findViewById(R.id.scrollViewBoiler)
        val buttonAddTimestamp : Button = requireActivity().findViewById(R.id.buttonAddTimestamp)
        val buttonDeleteTimestamps : Button = requireActivity().findViewById(R.id.buttonDeleteTimestamps)
        val buttonStartTimeMode : Button = requireActivity().findViewById(R.id.buttonStartTimeMode)
        val buttonStopTimeMode : Button = requireActivity().findViewById(R.id.buttonStopTimeMode)
        val inputHeatingOrBoiler : AutoCompleteTextView = requireActivity().findViewById(R.id.inputTimeHeatingOrBoiler)
        val inputHeatingElementsCount : AutoCompleteTextView = requireActivity().findViewById(R.id.inputHeatingElementsTime)
        val inputLayoutHeatingElementsCount : TextInputLayout = requireActivity().findViewById(R.id.inputLayoutHeatingElementsTime)

        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)
        isOverCurrentProtectionEnabled = sharedPreferences.getBoolean("OverCurrentProtectionEnabled", true)
        isNotificationChannelCreated = sharedPreferences.getBoolean("TimeNotificationChannelCreated", false)
        isNotificationGroupCreated = sharedPreferences.getBoolean("NotificationGroupCreated", false)
        isTimeNotificationsEnabled = sharedPreferences.getBoolean("TimeNotifications", true)
        notificationChannelsCount = sharedPreferences.getInt("NotificationChannelsCount", 0)
        heatingOrBoiler = sharedPreferences.getBoolean("TimeHeatingOrBoiler", true)
        isHeatingTimeModeStarted = sharedPreferences.getBoolean("HeatingTimeModeStarted", false)
        isBoilerTimeModeStarted = sharedPreferences.getBoolean("BoilerTimeModeStarted", false)
        heatingTimestampType = sharedPreferences.getBoolean("HeatingTimestampType", false)
        boilerTimestampType = sharedPreferences.getBoolean("BoilerTimestampType", false)
        isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        heatingElementsCount = sharedPreferences.getInt("HeatingElementsTime", 1)
        maxHeatingElementsCount = sharedPreferences.getInt("MaxHeatingElements", 2)
        heatingBroadcastIds = sharedPreferences.getInt("HeatingBroadcastIds", 0)
        boilerBroadcastIds = sharedPreferences.getInt("BoilerBroadcastIds", 99)
        currentHeatingMode = sharedPreferences.getInt("CurrentHeatingMode", 0)
        currentBoilerMode = sharedPreferences.getInt("CurrentBoilerMode", 0)

        val heatingOrBoilerList = listOf("???????????????????? ????????????", "???????????????????? ????????????????")
        val heatingOrBoilerArrayAdapter = ArrayAdapter(requireActivity().applicationContext, R.layout.dropdown_menu_item, heatingOrBoilerList)
        inputHeatingOrBoiler.setAdapter(heatingOrBoilerArrayAdapter)
        inputHeatingOrBoiler.threshold = 1

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
                        heatingElementsCountList.add("???????????????? 1 ?????? ??????????")
                    }
                    i in 2..4 -> {
                        heatingElementsCountList.add("???????????????? $i ???????? ??????????")
                    }
                    i >= 5 -> {
                        heatingElementsCountList.add("???????????????? $i ?????????? ??????????")
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
                    inputHeatingElementsCount.setText("???????????????? 1 ?????? ??????????", false)
                }
                heatingElementsCount in 2..4 -> {
                    inputHeatingElementsCount.setText("???????????????? $heatingElementsCount ???????? ??????????", false)
                }
                heatingElementsCount >= 5 -> {
                    inputHeatingElementsCount.setText("???????????????? $heatingElementsCount ?????????? ??????????", false)
                }
            }
        }

        heatingTimestampsArrayListJson = sharedPreferences.getString("HeatingTimestampsArrayList", "").toString()
        if (heatingTimestampsArrayListJson != "") {
            heatingTimestampTypesArrayListJson = sharedPreferences.getString("HeatingTimestampTypesArrayList", "").toString()
            heatingElementsArrayListJson = sharedPreferences.getString("HeatingElementsArrayList", "").toString()
            heatingTimestampsArrayList = gson.fromJson(heatingTimestampsArrayListJson, object : TypeToken<ArrayList<String?>?>() {}.type)
            heatingTimestampTypesArrayList = gson.fromJson(heatingTimestampTypesArrayListJson, object : TypeToken<ArrayList<Boolean?>?>() {}.type)
            heatingElementsArrayList = gson.fromJson(heatingElementsArrayListJson, object : TypeToken<ArrayList<Int?>?>() {}.type)
        }
        boilerTimestampsArrayListJson = sharedPreferences.getString("BoilerTimestampsArrayList", "").toString()
        if (boilerTimestampsArrayListJson != "") {
            boilerTimestampTypesArrayListJson = sharedPreferences.getString("BoilerTimestampTypesArrayList", "").toString()
            boilerTimestampsArrayList = gson.fromJson(boilerTimestampsArrayListJson, object : TypeToken<ArrayList<String?>?>() {}.type)
            boilerTimestampTypesArrayList = gson.fromJson(boilerTimestampTypesArrayListJson, object : TypeToken<ArrayList<Boolean?>?>() {}.type)
        }

        if (heatingOrBoiler) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                inputHeatingOrBoiler.setText("???????????????????? ????????????", false)
            }
            if (heatingTimestampsArrayListJson != "") {
                scrollViewHeating.visibility = View.VISIBLE
                heatingRecyclerAdapter = HeatingRecyclerAdapter(heatingTimestampsArrayList, heatingTimestampTypesArrayList, heatingElementsArrayList)
                recyclerViewHeatingTimestamps.apply {
                    adapter = heatingRecyclerAdapter
                    layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                }
                if (heatingTimestampType) {
                    buttonAddTimestamp.text = "+ ?????????? ????????????????????"
                } else {
                    buttonAddTimestamp.text = "+ ?????????? ??????????????????"
                }
                if (isHeatingTimeModeStarted) {
                    inputLayoutHeatingElementsCount.visibility = View.GONE
                    buttonAddTimestamp.visibility = View.GONE
                    buttonStartTimeMode.visibility = View.GONE
                    buttonStopTimeMode.visibility = View.VISIBLE
                } else {
                    buttonDeleteTimestamps.visibility = View.VISIBLE
                    buttonStartTimeMode.visibility = View.VISIBLE
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                inputHeatingOrBoiler.setText("???????????????????? ????????????????", false)
            }
            inputLayoutHeatingElementsCount.visibility = View.GONE
            if (boilerTimestampsArrayListJson != "") {
                scrollViewBoiler.visibility = View.VISIBLE
                boilerRecyclerAdapter = BoilerRecyclerAdapter(boilerTimestampsArrayList, boilerTimestampTypesArrayList)
                recyclerViewBoilerTimestamps.apply {
                    adapter = boilerRecyclerAdapter
                    layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                }
                if (boilerTimestampType) {
                    buttonAddTimestamp.text = "+ ?????????? ????????????????????"
                } else {
                    buttonAddTimestamp.text = "+ ?????????? ??????????????????"
                }
                if (isBoilerTimeModeStarted) {
                    buttonStartTimeMode.visibility = View.VISIBLE
                    buttonStopTimeMode.visibility = View.GONE
                    buttonAddTimestamp.visibility = View.GONE
                } else {
                    buttonDeleteTimestamps.visibility = View.VISIBLE
                    buttonStartTimeMode.visibility = View.VISIBLE
                }
            }
        }

        inputHeatingOrBoiler.setOnItemClickListener { _, _, position, _ ->
            vibrate()
            if (position == 0) {
                heatingOrBoiler = true
                scrollViewBoiler.visibility = View.GONE
                if (heatingTimestampType) {
                    buttonAddTimestamp.text = "+ ?????????? ????????????????????"
                } else {
                    buttonAddTimestamp.text = "+ ?????????? ??????????????????"
                }
                if (heatingTimestampsArrayListJson != "") {
                    scrollViewHeating.visibility = View.VISIBLE
                    heatingRecyclerAdapter = HeatingRecyclerAdapter(heatingTimestampsArrayList, heatingTimestampTypesArrayList, heatingElementsArrayList)
                    recyclerViewHeatingTimestamps.apply {
                        adapter = heatingRecyclerAdapter
                        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    }
                    if (isHeatingTimeModeStarted) {
                        buttonAddTimestamp.visibility = View.GONE
                        buttonDeleteTimestamps.visibility = View.GONE
                        buttonStartTimeMode.visibility = View.GONE
                        buttonStopTimeMode.visibility = View.VISIBLE
                    } else {
                        if (maxHeatingElementsCount != 1) {
                            inputLayoutHeatingElementsCount.visibility = View.VISIBLE
                        }
                        buttonAddTimestamp.visibility = View.VISIBLE
                        buttonDeleteTimestamps.visibility = View.VISIBLE
                        buttonStartTimeMode.visibility = View.VISIBLE
                        buttonStopTimeMode.visibility = View.GONE
                    }
                } else {
                    if (maxHeatingElementsCount != 1) {
                        inputLayoutHeatingElementsCount.visibility = View.VISIBLE
                    }
                    buttonAddTimestamp.visibility = View.VISIBLE
                    buttonDeleteTimestamps.visibility = View.GONE
                    buttonStartTimeMode.visibility = View.GONE
                    buttonStopTimeMode.visibility = View.GONE
                }
            } else {
                heatingOrBoiler = false
                inputLayoutHeatingElementsCount.visibility = View.GONE
                scrollViewHeating.visibility = View.GONE
                if (boilerTimestampType) {
                    buttonAddTimestamp.text = "+ ?????????? ????????????????????"
                } else {
                    buttonAddTimestamp.text = "+ ?????????? ??????????????????"
                }
                if (boilerTimestampsArrayListJson != "") {
                    scrollViewBoiler.visibility = View.VISIBLE
                    boilerRecyclerAdapter = BoilerRecyclerAdapter(boilerTimestampsArrayList, boilerTimestampTypesArrayList)
                    recyclerViewBoilerTimestamps.apply {
                        adapter = boilerRecyclerAdapter
                        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    }
                    if (isBoilerTimeModeStarted) {
                        buttonAddTimestamp.visibility = View.GONE
                        buttonDeleteTimestamps.visibility = View.GONE
                        buttonStartTimeMode.visibility = View.GONE
                        buttonStopTimeMode.visibility = View.VISIBLE
                    } else {
                        buttonAddTimestamp.visibility = View.VISIBLE
                        buttonDeleteTimestamps.visibility = View.VISIBLE
                        buttonStartTimeMode.visibility = View.VISIBLE
                        buttonStopTimeMode.visibility = View.GONE
                    }
                } else {
                    buttonAddTimestamp.visibility = View.VISIBLE
                    buttonDeleteTimestamps.visibility = View.GONE
                    buttonStartTimeMode.visibility = View.GONE
                    buttonStopTimeMode.visibility = View.GONE
                }
            }
            editPreferences.putBoolean("TimeHeatingOrBoiler", heatingOrBoiler).apply()
        }

        inputHeatingElementsCount.setOnItemClickListener { _, _, position, _ ->
            vibrate()
            heatingElementsCount = position + 1
            editPreferences.putInt("HeatingElementsTime", heatingElementsCount).apply()
        }

        buttonAddTimestamp.setOnClickListener {
            vibrate()
            val calendar = Calendar.getInstance()
            val timePicker = TimePickerDialog(requireContext(), { _, hours, minutes ->
                vibrate()
                if (heatingOrBoiler) {
                    heatingTimestampType = !heatingTimestampType
                    if (heatingTimestampType) {
                        buttonAddTimestamp.text = "+ ?????????? ????????????????????"
                        heatingElementsArrayList.add(heatingElementsCount)
                    } else {
                        buttonAddTimestamp.text = "+ ?????????? ??????????????????"
                        heatingElementsArrayList.add(0)
                    }
                    scrollViewBoiler.visibility = View.GONE
                    scrollViewHeating.visibility = View.VISIBLE
                    buttonDeleteTimestamps.visibility = View.VISIBLE
                    buttonStartTimeMode.visibility = View.VISIBLE

                    if (hours < 10 && minutes < 10) {
                        heatingTimestampsArrayList.add("0$hours:0$minutes")
                    }
                    if (hours < 10 && minutes >= 10) {
                        heatingTimestampsArrayList.add("0$hours:$minutes")
                    }
                    if (hours >= 10 && minutes < 10) {
                        heatingTimestampsArrayList.add("$hours:0$minutes")
                    }
                    if (hours >= 10 && minutes >= 10) {
                        heatingTimestampsArrayList.add("$hours:$minutes")
                    }
                    heatingTimestampTypesArrayList.add(heatingTimestampType)

                    heatingRecyclerAdapter = HeatingRecyclerAdapter(heatingTimestampsArrayList, heatingTimestampTypesArrayList, heatingElementsArrayList)
                    recyclerViewHeatingTimestamps.apply {
                        adapter = heatingRecyclerAdapter
                        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    }
                    heatingTimestampsArrayListJson = gson.toJson(heatingTimestampsArrayList)
                    heatingTimestampTypesArrayListJson = gson.toJson(heatingTimestampTypesArrayList)
                    heatingElementsArrayListJson = gson.toJson(heatingElementsArrayList)
                    editPreferences.putString("HeatingTimestampsArrayList", heatingTimestampsArrayListJson)
                    editPreferences.putString("HeatingTimestampTypesArrayList", heatingTimestampTypesArrayListJson)
                    editPreferences.putString("HeatingElementsArrayList", heatingElementsArrayListJson)
                    editPreferences.putBoolean("HeatingTimestampType", heatingTimestampType).apply()
                } else {
                    boilerTimestampType = !boilerTimestampType
                    if (boilerTimestampType) {
                        buttonAddTimestamp.text = "+ ?????????? ????????????????????"
                    } else {
                        buttonAddTimestamp.text = "+ ?????????? ??????????????????"
                    }
                    scrollViewHeating.visibility = View.GONE
                    scrollViewBoiler.visibility = View.VISIBLE
                    buttonDeleteTimestamps.visibility = View.VISIBLE
                    buttonStartTimeMode.visibility = View.VISIBLE
                    if (hours < 10 && minutes < 10) {
                        boilerTimestampsArrayList.add("0$hours:0$minutes")
                    }
                    if (hours < 10 && minutes >= 10) {
                        boilerTimestampsArrayList.add("0$hours:$minutes")
                    }
                    if (hours >= 10 && minutes < 10) {
                        boilerTimestampsArrayList.add("$hours:0$minutes")
                    }
                    if (hours >= 10 && minutes >= 10) {
                        boilerTimestampsArrayList.add("$hours:$minutes")
                    }
                    boilerTimestampTypesArrayList.add(boilerTimestampType)

                    boilerRecyclerAdapter = BoilerRecyclerAdapter(boilerTimestampsArrayList, boilerTimestampTypesArrayList)
                    recyclerViewBoilerTimestamps.apply {
                        adapter = boilerRecyclerAdapter
                        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    }
                    boilerTimestampsArrayListJson = gson.toJson(boilerTimestampsArrayList)
                    boilerTimestampTypesArrayListJson = gson.toJson(boilerTimestampTypesArrayList)
                    editPreferences.putString("BoilerTimestampsArrayList", boilerTimestampsArrayListJson)
                    editPreferences.putString("BoilerTimestampTypesArrayList", boilerTimestampTypesArrayListJson)
                    editPreferences.putBoolean("BoilerTimestampType", boilerTimestampType).apply()
                }
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), DateFormat.is24HourFormat(requireContext()))
            timePicker.show()
        }

        buttonDeleteTimestamps.setOnClickListener {
            vibrate()
            buttonDeleteTimestamps.visibility = View.GONE
            buttonStartTimeMode.visibility = View.GONE
            if (heatingOrBoiler) {
                heatingTimestampsArrayListItemCount = heatingTimestampsArrayList.size
                heatingTimestampsArrayList.clear()
                heatingTimestampTypesArrayList.clear()
                heatingElementsArrayList.clear()
                heatingRecyclerAdapter.notifyItemRangeChanged(0, heatingTimestampsArrayListItemCount)
                heatingTimestampType = false
                heatingTimestampsArrayListJson = ""
                buttonAddTimestamp.text = "+ ?????????? ??????????????????"
                scrollViewHeating.visibility = View.GONE
                editPreferences.putString("HeatingTimestampsArrayList", "")
                editPreferences.putString("HeatingTimestampTypesArrayList", "")
                editPreferences.putString("HeatingElementsArrayList", "")
                editPreferences.putBoolean("HeatingTimestampType", false).apply()
            } else {
                boilerTimestampsArrayListItemCount = boilerTimestampsArrayList.size
                boilerTimestampsArrayList.clear()
                boilerTimestampTypesArrayList.clear()
                boilerRecyclerAdapter.notifyItemRangeChanged(0, boilerTimestampsArrayListItemCount)
                boilerTimestampType = false
                buttonAddTimestamp.text = "+ ?????????? ??????????????????"
                scrollViewBoiler.visibility = View.GONE
                editPreferences.putString("BoilerTimestampsArrayList", "")
                editPreferences.putString("BoilerTimestampTypesArrayList", "")
                editPreferences.putString("BoilerElementsArrayList", "")
                editPreferences.putBoolean("BoilerTimestampType", false).apply()
            }
        }

        buttonStartTimeMode.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    if ((heatingOrBoiler && currentHeatingMode == 0) || (!heatingOrBoiler && currentBoilerMode == 0)) {
                        if (isOverCurrentProtectionEnabled && ((heatingOrBoiler && heatingElementsArrayList.contains(maxHeatingElementsCount) && (sharedPreferences.getBoolean("BoilerStarted", false) || isBoilerTimeModeStarted ||  currentBoilerMode == 1 || currentBoilerMode == 3)) ||
                           (!heatingOrBoiler && (sharedPreferences.getBoolean("HeatingStarted", false) && sharedPreferences.getInt("HeatingElements", 1) == maxHeatingElementsCount) || (isHeatingTimeModeStarted && heatingElementsArrayList.contains(maxHeatingElementsCount)) ||
                           (currentHeatingMode == 1 && sharedPreferences.getInt("HeatingElementsTimer", 1) == maxHeatingElementsCount) ||
                           (currentBoilerMode == 3 && sharedPreferences.getInt("HeatingElementsTemperature", 1) == maxHeatingElementsCount)))) {
                            overCurrentProtection()
                        } else {
                            inputLayoutHeatingElementsCount.visibility = View.GONE
                            buttonAddTimestamp.visibility = View.GONE
                            buttonDeleteTimestamps.visibility = View.GONE
                            buttonStartTimeMode.visibility = View.GONE
                            buttonStopTimeMode.visibility = View.VISIBLE
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val notificationManager = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                if (!isNotificationGroupCreated) {
                                    notificationManager.createNotificationChannelGroup(NotificationChannelGroup("SmartHeating", "?????????? ??????????????????"))
                                    editPreferences.putBoolean("NotificationGroupCreated", true).apply()
                                }
                                if (!isNotificationChannelCreated) {
                                    val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                                    val notificationChannel = NotificationChannel("TimeNotification$notificationChannelsCount", "?????????????????????? ???? ??????????????", NotificationManager.IMPORTANCE_DEFAULT)
                                    notificationChannel.description = "?????????????????????? ?????????????? ???????????????????? ?????? ??????????????????/???????????????????? ??????????, ?????? ?????????????? ???? ??????????????"
                                    notificationChannel.enableLights(true)
                                    notificationChannel.enableVibration(false)
                                    notificationChannel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + requireActivity().packageName + "/" + R.raw.notification), audioAttributes)
                                    notificationChannel.group = "SmartHeating"
                                    notificationChannel.lockscreenVisibility = View.VISIBLE
                                    notificationManager.createNotificationChannel(notificationChannel)
                                    editPreferences.putBoolean("TimeNotificationChannelCreated", true).apply()
                                }
                            }
                            if (heatingOrBoiler) {
                                isHeatingTimeModeStarted = true
                                if (isTimeNotificationsEnabled) {
                                    heatingTimestampsArrayList.forEach {
                                        heatingBroadcastIds += 1
                                        val pendingIntent = PendingIntent.getBroadcast(requireActivity(), heatingBroadcastIds, Intent(requireActivity(), TimeNotificationsReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
                                        val notifyTime : Calendar = Calendar.getInstance()
                                        notifyTime.set(Calendar.HOUR_OF_DAY, SimpleDateFormat("hh:mm").parse(it).hours)
                                        notifyTime.set(Calendar.MINUTE, SimpleDateFormat("hh:mm").parse(it).minutes)
                                        notifyTime.set(Calendar.SECOND, 0)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notifyTime.timeInMillis, pendingIntent)
                                        }
                                    }
                                }
                                editPreferences.putInt("CurrentHeatingMode", 2)
                                editPreferences.putBoolean("HeatingTimeModeStarted", true)
                                editPreferences.putInt("HeatingBroadcastIds", heatingBroadcastIds).apply()
                                editPreferences.putBoolean("HeatingElementsTimeContains2", heatingElementsArrayList.contains(maxHeatingElementsCount))
                                realtimeDatabase.child(userUID).child("heatingArray").setValue(heatingElementsArrayList)
                                Handler().postDelayed({
                                    realtimeDatabase.child(userUID).child("heatingTime").setValue(heatingTimestampsArrayList)
                                }, 200)
                            } else {
                                isBoilerTimeModeStarted = true
                                if (isTimeNotificationsEnabled) {
                                    boilerTimestampsArrayList.forEach {
                                        boilerBroadcastIds += 1
                                        val pendingIntent = PendingIntent.getBroadcast(requireActivity(), boilerBroadcastIds, Intent(requireActivity(), TimeNotificationsReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
                                        val notifyTime : Calendar = Calendar.getInstance()
                                        notifyTime.set(Calendar.HOUR_OF_DAY, SimpleDateFormat("hh:mm").parse(it).hours)
                                        notifyTime.set(Calendar.MINUTE, SimpleDateFormat("hh:mm").parse(it).minutes)
                                        notifyTime.set(Calendar.SECOND, 0)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notifyTime.timeInMillis, pendingIntent)
                                        }
                                    }
                                }
                                editPreferences.putInt("CurrentBoilerMode", 2)
                                editPreferences.putBoolean("BoilerTimeModeStarted", true)
                                editPreferences.putInt("BoilerBroadcastIds", boilerBroadcastIds).apply()
                                realtimeDatabase.child(userUID).child("boilerTime").setValue(boilerTimestampsArrayList)
                            }
                        }
                    } else {
                        if (heatingOrBoiler) {
                            printHeatingMode()
                        } else {
                            printBoilerMode()
                        }
                    }
                } else {
                    Toast.makeText(activity, "???? ???? ?????????? ?? ????????????????????????!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, "???? ???? ???????????????????? ?? ??????????????????!", Toast.LENGTH_LONG).show()
            }
        }

        buttonStopTimeMode.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (maxHeatingElementsCount != 1) {
                    inputLayoutHeatingElementsCount.visibility = View.VISIBLE
                }
                buttonAddTimestamp.visibility = View.VISIBLE
                buttonDeleteTimestamps.visibility = View.VISIBLE
                buttonStartTimeMode.visibility = View.VISIBLE
                buttonStopTimeMode.visibility = View.GONE
                if (heatingOrBoiler) {
                    isHeatingTimeModeStarted = false
                    scrollViewHeating.visibility = View.VISIBLE
                    if (isTimeNotificationsEnabled) {
                        for (i in 1..heatingBroadcastIds) {
                            val pendingIntent = PendingIntent.getBroadcast(requireActivity(), heatingBroadcastIds, Intent(requireActivity(), TimeNotificationsReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
                            pendingIntent!!.cancel()
                        }
                    }
                    editPreferences.putInt("CurrentHeatingMode", 0)
                    editPreferences.putBoolean("HeatingTimeModeStarted", false).apply()
                    realtimeDatabase.child(userUID).child("heatingTime").setValue("0")
                } else {
                    isBoilerTimeModeStarted = false
                    scrollViewBoiler.visibility = View.VISIBLE
                    if (isTimeNotificationsEnabled) {
                        for (i in 100..boilerBroadcastIds) {
                            val pendingIntent = PendingIntent.getBroadcast(requireActivity(), boilerBroadcastIds, Intent(requireActivity(), TimeNotificationsReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
                            pendingIntent!!.cancel()
                        }
                    }
                    editPreferences.putInt("CurrentBoilerMode", 0)
                    editPreferences.putBoolean("BoilerTimeModeStarted", false).apply()
                    realtimeDatabase.child(userUID).child("boilerTime").setValue("0")
                }
            } else {
                Toast.makeText(activity, "???? ???? ???????????????????? ?? ??????????????????!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_time_control, container, false)
    }

    private fun overCurrentProtection() {
        val alertDialogOverCurrentProtectionBuilder = AlertDialog.Builder(requireActivity())
        alertDialogOverCurrentProtectionBuilder.setTitle("???????????? ???? ???????????????????? ????????")
        alertDialogOverCurrentProtectionBuilder.setMessage("???? ???? ???????????? ???????????????????????? ???????????????? ?????????? ???? ???????????????????????? " +
                "???????????????? ?? ????????????! ?????? ?????????? ???????????????? ?? ???????????????????? ??????????????????????. ???? ???????????? ?????????????????? ???????????? " +
                "???? ???????????????????? ?? ????????????????????.")
        alertDialogOverCurrentProtectionBuilder.setPositiveButton("??????????????????") { _, _ ->
            vibrate()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                (activity as AppCompatActivity).supportActionBar?.title = "??????????????????"
            }
            requireActivity().navigationView.setCheckedItem(R.id.buttonSettings)
            val fragmentManager = requireActivity().supportFragmentManager.beginTransaction()
            fragmentManager.replace(R.id.frameLayout, Settings()).commit()
        }
        alertDialogOverCurrentProtectionBuilder.setNegativeButton("????????????") { _, _ ->
            vibrate()
        }
        val alertDialogOverCurrentProtection = alertDialogOverCurrentProtectionBuilder.create()
        alertDialogOverCurrentProtection.show()
    }

    private fun printHeatingMode() {
        when (currentHeatingMode) {
            1 -> {
                Toast.makeText(activity, "???????????? ?????????????? ????????????!", Toast.LENGTH_LONG).show()
            }
            3 -> {
                Toast.makeText(activity, "???????????? ?????????????? ?????????? \n ?????????????????? ?? ???????????????????? \n ???? ??????????????????????!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun printBoilerMode() {
        when (currentBoilerMode) {
            1 -> {
                Toast.makeText(activity, "???????????? ?????????????? ????????????!", Toast.LENGTH_LONG).show()
            }
            3 -> {
                Toast.makeText(activity, "???????????? ?????????????? ?????????? \n ?????????????????? ?? ???????????????????? \n ???? ??????????????????????!", Toast.LENGTH_LONG).show()
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
            buttonAddTimestamp.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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