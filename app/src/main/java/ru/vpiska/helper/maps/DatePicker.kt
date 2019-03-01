package ru.vpiska.helper.maps

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.widget.TextView
import ru.vpiska.R
import java.text.SimpleDateFormat
import java.util.*

class DatePicker : DialogFragment(), DatePickerDialog.OnDateSetListener {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // определяем текущую дату


        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)


        // создаем DatePickerDialog и возвращаем его
        val picker = DatePickerDialog(activity!!, AlertDialog.THEME_HOLO_LIGHT, this,
                year, month, day)

        picker.setTitle(resources.getString(R.string.choose_date))
        c.set(year + 1, month, day)
        val now = System.currentTimeMillis() - 1000

        picker.datePicker.minDate = now

        picker.datePicker.maxDate = c.timeInMillis


        return picker
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

    override fun onDateSet(datePicker: android.widget.DatePicker, year: Int,
                           month: Int, day: Int) {

        val c = Calendar.getInstance()
        val f = SimpleDateFormat("yyyy-MM-dd")
        c.set(year, month + 1, day)
        val timestamp = f.format(c.time)


        val txtDate = activity!!.findViewById<TextView>(R.id.txtDate)
        txtDate.text = timestamp
    }
}