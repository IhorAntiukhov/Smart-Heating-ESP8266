package com.arduinoworld.smartheating

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.*
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*


@SuppressLint("CutPasteId")
@Suppress("DEPRECATION", "SetTextI18n", "CommitPrefEdits")
class ManualControl : Fragment() {
    private var isHapticFeedbackEnabled = true
    private var isOverCurrentProtectionEnabled = true
    private var heatingOrBoiler = true
    private var isHeatingStarted = false
    private var isBoilerStarted = false
    private var isUserLogged = false
    private var heatingElementsCount = 1
    private var maxHeatingElementsCount = 2
    private var currentHeatingMode = 0
    private var currentBoilerMode = 0
    private var userUID = ""

    lateinit var buttonUp : ImageButton
    lateinit var sharedPreferences : SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView : View = inflater.inflate(R.layout.fragment_manual_control, container, false)

        sharedPreferences = requireActivity().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        val editPreferences = sharedPreferences.edit()

        val firebaseAuth = FirebaseAuth.getInstance()
        val realtimeDatabase = FirebaseDatabase.getInstance().reference.child("users")

        val buttonStartHeating : Button = rootView.findViewById(R.id.buttonStartHeating)
        val buttonStopHeating : Button = rootView.findViewById(R.id.buttonStopHeating)
        val buttonStartBoiler : Button = rootView.findViewById(R.id.buttonStartBoiler)
        val buttonStopBoiler : Button = rootView.findViewById(R.id.buttonStopBoiler)
        val buttonDown : ImageButton = rootView.findViewById(R.id.buttonDown)
        val labelHeatingElementsCount : TextView = rootView.findViewById(R.id.labelHeatingElementsCount)
        val layoutHeatingElementsCount : RelativeLayout = rootView.findViewById(R.id.layoutHeatingElementsCount)
        val inputHeatingOrBoiler : AutoCompleteTextView = rootView.findViewById(R.id.inputManualHeatingOrBoiler)
        buttonUp = rootView.findViewById(R.id.buttonUp)

        isOverCurrentProtectionEnabled = sharedPreferences.getBoolean("OverCurrentProtectionEnabled", true)
        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)
        heatingOrBoiler = sharedPreferences.getBoolean("ManualHeatingOrBoiler", true)
        isHeatingStarted = sharedPreferences.getBoolean("HeatingStarted", false)
        isBoilerStarted = sharedPreferences.getBoolean("BoilerStarted", false)
        isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        heatingElementsCount = sharedPreferences.getInt("HeatingElements", 1)
        maxHeatingElementsCount = sharedPreferences.getInt("MaxHeatingElements", 2)
        currentHeatingMode = sharedPreferences.getInt("CurrentHeatingMode", 0)
        currentBoilerMode = sharedPreferences.getInt("CurrentBoilerMode", 0)

        val heatingOrBoilerList = listOf("Управление Котлом", "Управление Бойлером")
        val heatingOrBoilerArrayAdapter = ArrayAdapter(requireActivity().applicationContext, R.layout.dropdown_menu_item, heatingOrBoilerList)
        inputHeatingOrBoiler.setAdapter(heatingOrBoilerArrayAdapter)
        inputHeatingOrBoiler.threshold = 1

        if (isUserLogged) {
            userUID = firebaseAuth.currentUser!!.uid
        }

        if (maxHeatingElementsCount == 1) {
            layoutHeatingElementsCount.visibility = View.GONE
            val params = buttonStartHeating.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    13F,
                    requireActivity().resources.displayMetrics
            ).toInt()
            buttonStartHeating.layoutParams = params
        }

        Handler().postDelayed({
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    requireActivity().runOnUiThread(Thread {
                        realtimeDatabase.child(userUID).child("temperature").addValueEventListener(
                            object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    requireActivity().findViewById<TextView>(R.id.labelTemperatureInHeader).text = "${snapshot.getValue(Int::class.java).toString()}°C"
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    if (sharedPreferences.getBoolean("UserLogged", false)) {
                                        Toast.makeText(activity, "Не удалось получить температуру!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            })
                    })
                }
            }
        }, 200)

        labelHeatingElementsCount.text = "$heatingElementsCount тэн"
        if (heatingOrBoiler) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                inputHeatingOrBoiler.setText("Управление Котлом", false)
            }
            if (isHeatingStarted) {
                buttonUp.setImageResource(R.drawable.arrow_up_blocked_icon)
                buttonDown.setImageResource(R.drawable.arrow_down_blocked_icon)
                buttonStartHeating.visibility = View.GONE
                buttonStopHeating.visibility = View.VISIBLE
            } else {
                when {
                    maxHeatingElementsCount == 2 -> {
                        when (heatingElementsCount) {
                            1 -> {
                                buttonUp.setImageResource(R.drawable.arrow_up_icon)
                                buttonDown.setImageResource(R.drawable.arrow_down_blocked_icon)
                            }
                            2 -> {
                                buttonUp.setImageResource(R.drawable.arrow_up_blocked_icon)
                                buttonDown.setImageResource(R.drawable.arrow_down_icon)
                            }
                        }
                    }
                    maxHeatingElementsCount > 2 -> {
                        when (heatingElementsCount) {
                            1 -> {
                                buttonUp.setImageResource(R.drawable.arrow_up_icon)
                                buttonDown.setImageResource(R.drawable.arrow_down_blocked_icon)
                            }
                            in 2 until maxHeatingElementsCount -> {
                                buttonUp.setImageResource(R.drawable.arrow_up_icon)
                                buttonDown.setImageResource(R.drawable.arrow_down_icon)
                            }
                            maxHeatingElementsCount -> {
                                buttonUp.setImageResource(R.drawable.arrow_up_blocked_icon)
                                buttonDown.setImageResource(R.drawable.arrow_down_icon)
                            }
                        }
                    }
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                inputHeatingOrBoiler.setText("Управление Бойлером", false)
            }
            layoutHeatingElementsCount.visibility = View.GONE
            buttonStartHeating.visibility = View.GONE
            if (isBoilerStarted) {
                buttonStopBoiler.visibility = View.VISIBLE
            } else {
                buttonStartBoiler.visibility = View.VISIBLE
            }
        }

        inputHeatingOrBoiler.setOnItemClickListener { _, _, position, _ ->
            vibrate()
            if (position == 0) {
                heatingOrBoiler = true
                if (maxHeatingElementsCount != 1) {
                    layoutHeatingElementsCount.visibility = View.VISIBLE
                }
                if (isHeatingStarted) {
                    buttonStopHeating.visibility = View.VISIBLE
                } else {
                    buttonStartHeating.visibility = View.VISIBLE
                }
                buttonStartBoiler.visibility = View.GONE
                buttonStopBoiler.visibility = View.GONE
            } else {
                heatingOrBoiler = false
                layoutHeatingElementsCount.visibility = View.GONE
                if (isBoilerStarted) {
                    buttonStopBoiler.visibility = View.VISIBLE
                } else {
                    buttonStartBoiler.visibility = View.VISIBLE
                }
                buttonStartHeating.visibility = View.GONE
                buttonStopHeating.visibility = View.GONE
            }
            editPreferences.putBoolean("ManualHeatingOrBoiler", heatingOrBoiler).apply()
        }

        buttonUp.setOnClickListener {
            vibrate()
            if (heatingElementsCount < maxHeatingElementsCount && !isHeatingStarted) {
                heatingElementsCount += 1
                labelHeatingElementsCount.text = "$heatingElementsCount тэн"
                buttonDown.setImageResource(R.drawable.arrow_down_icon)
                if (heatingElementsCount == maxHeatingElementsCount) {
                    buttonUp.setImageResource(R.drawable.arrow_up_blocked_icon)
                }
                editPreferences.putInt("HeatingElements", heatingElementsCount).apply()
            } else {
                if (!isHeatingStarted) {
                    Toast.makeText(activity, "Вы установили максимальное количество тэнов!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(activity, "Вы не можете изменять \n количество тэнов, пока \n запущен котёл!", Toast.LENGTH_LONG).show()
                }
            }
        }

        buttonDown.setOnClickListener {
            vibrate()
            if (heatingElementsCount > 1 && !isHeatingStarted) {
                heatingElementsCount -= 1
                labelHeatingElementsCount.text = "$heatingElementsCount тэн"
                buttonUp.setImageResource(R.drawable.arrow_up_icon)
                if (heatingElementsCount == 1) {
                    buttonDown.setImageResource(R.drawable.arrow_down_blocked_icon)
                }
                editPreferences.putInt("HeatingElements", heatingElementsCount).apply()
            } else {
                if (!isHeatingStarted) {
                    Toast.makeText(activity, "Вы установили минимальное количество тэнов!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(activity, "Вы не можете изменять \n количество тэнов, пока \n запущен котёл!", Toast.LENGTH_LONG).show()
                }
            }
        }

        buttonStartHeating.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    if (currentHeatingMode == 0) {
                        if (isOverCurrentProtectionEnabled && heatingElementsCount == maxHeatingElementsCount && (isBoilerStarted || currentBoilerMode == 1 || currentBoilerMode == 2 || currentBoilerMode == 3)) {
                            overCurrentProtection()
                        } else {
                            isHeatingStarted = true
                            buttonUp.setImageResource(R.drawable.arrow_up_blocked_icon)
                            buttonDown.setImageResource(R.drawable.arrow_down_blocked_icon)
                            buttonStartHeating.visibility = View.GONE
                            buttonStopHeating.visibility = View.VISIBLE
                            editPreferences.putBoolean("HeatingStarted", true).apply()
                            realtimeDatabase.child(userUID).child("heatingElements").setValue(heatingElementsCount)
                            Handler().postDelayed({
                                realtimeDatabase.child(userUID).child("heatingStarted").setValue(true)
                            }, 200)
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

        buttonStopHeating.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    if (currentHeatingMode == 0) {
                        isHeatingStarted = false
                        when {
                            maxHeatingElementsCount == 2 -> {
                                when (heatingElementsCount) {
                                    1 -> {
                                        buttonUp.setImageResource(R.drawable.arrow_up_icon)
                                        buttonDown.setImageResource(R.drawable.arrow_down_blocked_icon)
                                    }
                                    2 -> {
                                        buttonUp.setImageResource(R.drawable.arrow_up_blocked_icon)
                                        buttonDown.setImageResource(R.drawable.arrow_down_icon)
                                    }
                                }
                            }
                            maxHeatingElementsCount > 2 -> {
                                when (heatingElementsCount) {
                                    1 -> {
                                        buttonUp.setImageResource(R.drawable.arrow_up_icon)
                                        buttonDown.setImageResource(R.drawable.arrow_down_blocked_icon)
                                    }
                                    in 2 until maxHeatingElementsCount -> {
                                        buttonUp.setImageResource(R.drawable.arrow_up_icon)
                                        buttonDown.setImageResource(R.drawable.arrow_down_icon)
                                    }
                                    maxHeatingElementsCount -> {
                                        buttonUp.setImageResource(R.drawable.arrow_up_blocked_icon)
                                        buttonDown.setImageResource(R.drawable.arrow_down_icon)
                                    }
                                }
                            }
                        }
                        buttonStartHeating.visibility = View.VISIBLE
                        buttonStopHeating.visibility = View.GONE
                        editPreferences.putBoolean("HeatingStarted", false).apply()
                        realtimeDatabase.child(userUID).child("heatingStarted").setValue(false)
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

        buttonStartBoiler.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    if (currentBoilerMode == 0) {
                        if (isOverCurrentProtectionEnabled && heatingElementsCount == maxHeatingElementsCount &&
                           isHeatingStarted || (currentHeatingMode == 1 && sharedPreferences.getInt("HeatingElementsTimer", 1) == maxHeatingElementsCount) ||
                           (currentHeatingMode == 2 && sharedPreferences.getBoolean("HeatingElementsTimeContains2", false)) ||
                           (currentBoilerMode == 3 && sharedPreferences.getInt("HeatingElementsTemperature", 1) == maxHeatingElementsCount)) {
                            overCurrentProtection()
                        } else {
                            isBoilerStarted = true
                            buttonStopBoiler.visibility = View.VISIBLE
                            buttonStartBoiler.visibility = View.GONE
                            editPreferences.putBoolean("BoilerStarted", true).apply()
                            realtimeDatabase.child(userUID).child("boilerStarted").setValue(true)
                        }
                    } else {
                        printBoilerMode()
                    }
                } else {
                    Toast.makeText(activity, "Вы не вошли в пользователя!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonStopBoiler.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    if (currentBoilerMode == 0) {
                        isBoilerStarted = false
                        buttonStartBoiler.visibility = View.VISIBLE
                        buttonStopBoiler.visibility = View.GONE
                        editPreferences.putBoolean("BoilerStarted", false).apply()
                        realtimeDatabase.child(userUID).child("boilerStarted").setValue(false)
                    } else {
                        printBoilerMode()
                    }
                } else {
                    Toast.makeText(activity, "Вы не вошли в пользователя!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        return rootView
    }

    private fun overCurrentProtection() {
        val alertDialogOverCurrentProtectionBuilder = AlertDialog.Builder(requireActivity())
        alertDialogOverCurrentProtectionBuilder.setTitle("Защита от перегрузки сети")
        alertDialogOverCurrentProtectionBuilder.setMessage(
            "Вы не можете одновременно включать котёл на максимальную " +
                    "мощность и бойлер! Это может привести к перегрузке электросети. Вы можете отключить защиту " +
                    "от перегрузки в настройках."
        )
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
            1 -> {
                Toast.makeText(activity, "Сейчас запущен таймер!", Toast.LENGTH_LONG).show()
            }
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
            1 -> {
                Toast.makeText(activity, "Сейчас запущен таймер!", Toast.LENGTH_LONG).show()
            }
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
            buttonUp.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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