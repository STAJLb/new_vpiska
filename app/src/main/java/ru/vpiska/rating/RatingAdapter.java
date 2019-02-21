package ru.vpiska.rating;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import ru.vpiska.R;
import ru.vpiska.party.PartyActivity;
import ru.vpiska.profile.OtherProfileActivity;

public class RatingAdapter extends ArrayAdapter<Rating> {
    private LayoutInflater inflater;
    private int layout;
    private ArrayList<Rating> ratingList;
    private Context mContext = getContext();

    RatingAdapter(Context context, int resource, ArrayList<Rating> rating) {
        super(context, resource, rating);
        this.ratingList = rating;
        this.layout = resource;
        this.inflater = LayoutInflater.from(context);
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    public View getView(int position, View convertView,@NonNull ViewGroup parent) {

        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(this.layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        final Rating product = ratingList.get(position);

        viewHolder.nameView.setText(product.getName());
        viewHolder.ratingView.setText("Рейтинг: " + product.getRating());


        viewHolder.linkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, OtherProfileActivity.class);
                intent.putExtra("id_user", product.getId());
                //Toast.makeText(getContext(),  product.getId(), Toast.LENGTH_LONG).show();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                mContext.startActivity(intent);



            }
        });


        return convertView;
    }



    private class ViewHolder {
        final Button linkButton;
        final TextView nameView,ratingView;

        ViewHolder(View view) {
            linkButton = (Button) view.findViewById(R.id.linkButton);
            nameView = (TextView) view.findViewById(R.id.nameView);
            ratingView = (TextView) view.findViewById(R.id.ratingView);

        }
    }
}