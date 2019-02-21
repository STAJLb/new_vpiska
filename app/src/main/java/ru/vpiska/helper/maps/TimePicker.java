package ru.vpiska.helper.maps;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.TextView;


import java.text.SimpleDateFormat;
import java.util.Calendar;

import ru.vpiska.R;


public class TimePicker extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        Dialog picker = new TimePickerDialog(getActivity(),  this,
                hour,minute,DateFormat.is24HourFormat(getActivity()));

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
    public void onTimeSet(android.widget.TimePicker timePicker, int hourOfDay, int minute) {





        @SuppressLint("SimpleDateFormat") SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");
        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND,00);

        String timestamp = f.format(c.getTime());
        TextView txtTime = (TextView) getActivity().findViewById(R.id.txtTime);
        txtTime.setText(timestamp);
    }


}

