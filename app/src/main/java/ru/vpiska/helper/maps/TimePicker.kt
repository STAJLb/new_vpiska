package ru.vpiska.helper.maps


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import ru.vpiska.R
import java.text.SimpleDateFormat
import java.util.*


class TimePicker : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current time as the default values for the picker
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        return TimePickerDialog(activity, this,
                hour, minute, DateFormat.is24HourFormat(activity))
    }

    override fun onStart() {
        super.onStart()
        // добавляем кастомный текст для кнопки
        val nButton = (dialog as AlertDialog)
                .getButton(DialogInterface.BUTTON_POSITIVE)
        nButton.text = resources.getString(R.string.ready)
        val ngButton = (dialog as AlertDialog)
                .getButton(DialogInterface.BUTTON_NEGATIVE)
        ngButton.text = resources.getString(R.string.cancel)

    }

    override fun onTimeSet(timePicker: android.widget.TimePicker, hourOfDay: Int, minute: Int) {


        @SuppressLint("SimpleDateFormat") val f = SimpleDateFormat("HH:mm:ss")
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, hourOfDay)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)

        val timestamp = f.format(c.time)
        val txtTime = activity!!.findViewById<View>(R.id.txtTime) as TextView
        txtTime.text = timestamp
    }


}

