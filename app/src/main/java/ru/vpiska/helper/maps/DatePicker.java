package ru.vpiska.helper.maps;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import ru.vpiska.R;

public class DatePicker extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // определяем текущую дату


        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);


        // создаем DatePickerDialog и возвращаем его
        DatePickerDialog picker = new DatePickerDialog(getActivity(), AlertDialog.THEME_HOLO_LIGHT, this,
                year, month, day);

        picker.setTitle(getResources().getString(R.string.choose_date));
        c.set(year+1, month, day);//Year,Mounth -1,Day
        long now = System.currentTimeMillis() - 1000;

        picker.getDatePicker().setMinDate(now);

        picker.getDatePicker().setMaxDate(c.getTimeInMillis());


        return picker;
    }

    @Override
    public void onStart() {
        super.onStart();
        // добавляем кастомный текст для кнопки
        Button nButton = ((AlertDialog) getDialog())
                .getButton(DialogInterface.BUTTON_POSITIVE);
        nButton.setText(getResources().getString(R.string.ready));
        Button ngButton = ((AlertDialog) getDialog())
                .getButton(DialogInterface.BUTTON_NEGATIVE);
        ngButton.setText(getResources().getString(R.string.cancel));

    }

    @Override
    public void onDateSet(android.widget.DatePicker datePicker, int year,
                          int month, int day) {

        final Calendar c = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        c.set(year, month+1, day);
        String timestamp = f.format(c.getTime());


        TextView txtDate = (TextView) getActivity().findViewById(R.id.txtDate);
        txtDate.setText(timestamp);
    }
}