package ru.vpiska.helper


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.widget.TextView
import ru.vpiska.R
import java.util.*

class DatePicker : DialogFragment(), DatePickerDialog.OnDateSetListener {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // определяем текущую дату


        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR) - 18
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)


        // создаем DatePickerDialog и возвращаем его
        val picker = DatePickerDialog(activity!!, AlertDialog.THEME_HOLO_LIGHT, this,
                year, month, day)
        picker.setTitle(resources.getString(R.string.choose_date))

        val now = System.currentTimeMillis() - 1000
        c.set(year - 99, month, day)

        picker.datePicker.minDate = c.timeInMillis
        picker.datePicker.maxDate = now

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

    @SuppressLint("SetTextI18n")
    override fun onDateSet(datePicker: android.widget.DatePicker, year: Int,
                           month: Int, day: Int) {
        var month = month

        val txtAge = activity!!.findViewById<View>(R.id.txtAge) as TextView
        month += 1
        txtAge.text = "$day-$month-$year"
    }
}