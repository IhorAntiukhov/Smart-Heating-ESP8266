package com.arduinoworld.smartheating

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings.*

@Suppress("DEPRECATION")
@SuppressLint("CommitPrefEdits")
class Settings : Fragment() {
    lateinit var editPreferences : SharedPreferences.Editor
    lateinit var buttonDefaultSettings : Button
    lateinit var inputMaxHeatingElements : EditText
    lateinit var inputStartHeatingTemperature : EditText
    lateinit var inputStopHeatingTemperature : EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        editPreferences = sharedPreferences.edit()

        val radioGroupVibrationTypes : RadioGroup = requireActivity().findViewById(R.id.radioGroupVibrationTypes)
        val radioButtonNormalVibration : RadioButton = requireActivity().findViewById(R.id.radioButtonNormalVibration)
        val radioButtonHapticFeedback : RadioButton = requireActivity().findViewById(R.id.radioButtonHapticFeedback)
        val checkBoxOverCurrentProtection : CheckBox = requireActivity().findViewById(R.id.checkBoxOverCurrentProtection)
        inputMaxHeatingElements = requireActivity().findViewById(R.id.inputMaxHeatingElements)
        inputStartHeatingTemperature = requireActivity().findViewById(R.id.inputStartHeatingTemperature)
        inputStopHeatingTemperature = requireActivity().findViewById(R.id.inputStopHeatingTemperature)
        buttonDefaultSettings = requireActivity().findViewById(R.id.buttonDefaultSettings)

        if (!sharedPreferences.getBoolean("HapticFeedbackEnabled", true)) {
            radioGroupVibrationTypes.check(R.id.radioButtonNormalVibration)
        } else {
            radioGroupVibrationTypes.check(R.id.radioButtonHapticFeedback)
        }
        checkBoxOverCurrentProtection.isChecked = sharedPreferences.getBoolean("OverCurrentProtectionEnabled", true)

        inputMaxHeatingElements.setText(sharedPreferences.getInt("MaxHeatingElements", 2).toString())
        inputStartHeatingTemperature.setText(sharedPreferences.getInt("HeatingStartTemperature", 1).toString())
        inputStopHeatingTemperature.setText(sharedPreferences.getInt("HeatingStopTemperature", 2).toString())

        radioGroupVibrationTypes.setOnCheckedChangeListener { _, checkedId ->
            vibrate()
            when (checkedId) {
                R.id.radioButtonNormalVibration -> {
                    editPreferences.putBoolean("HapticFeedbackEnabled", false).apply()
                }
                R.id.radioButtonHapticFeedback -> {
                    editPreferences.putBoolean("HapticFeedbackEnabled", true).apply()
                }
            }
        }

        checkBoxOverCurrentProtection.setOnCheckedChangeListener { _, isChecked ->
            vibrate()
            if (!isChecked) {
                val alertDialogOverCurrentProtectionBuilder = AlertDialog.Builder(requireActivity())
                alertDialogOverCurrentProtectionBuilder.setTitle("Защита от перегрузки сети")
                alertDialogOverCurrentProtectionBuilder.setMessage("Внимание! Перегрузка электросети может привести к возгоранию, " +
                        "или к другим неприятным вещам. Я не несу ответственность в случае если у вас что-то случится.")
                alertDialogOverCurrentProtectionBuilder.setPositiveButton("Понятно") { _, _ ->
                    vibrate()
                    editPreferences.putBoolean("OverCurrentProtectionEnabled", false).apply()
                }
                alertDialogOverCurrentProtectionBuilder.setNegativeButton("Отмена") { _, _ ->
                    checkBoxOverCurrentProtection.isChecked = true
                }
                val alertDialogOverCurrentProtection = alertDialogOverCurrentProtectionBuilder.create()
                alertDialogOverCurrentProtection.show()
            } else {
                editPreferences.putBoolean("OverCurrentProtectionEnabled", true).apply()
            }
        }

        buttonDefaultSettings.setOnClickListener {
            vibrate()
            radioButtonHapticFeedback.isChecked = true
            radioButtonNormalVibration.isChecked = false
            checkBoxOverCurrentProtection.isChecked = true
            inputMaxHeatingElements.setText("2")
            editPreferences.putInt("MaxHeatingElements", 2).apply()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onPause() {
        super.onPause()
        editPreferences.putInt("MaxHeatingElements", inputMaxHeatingElements.text.toString().toInt())
        editPreferences.putInt("HeatingStartTemperature", inputStartHeatingTemperature.text.toString().toInt())
        editPreferences.putInt("HeatingStopTemperature", inputStopHeatingTemperature.text.toString().toInt()).apply()
    }

    private fun vibrate() {
        if (radioButtonHapticFeedback.isChecked) {
            buttonDefaultSettings.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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