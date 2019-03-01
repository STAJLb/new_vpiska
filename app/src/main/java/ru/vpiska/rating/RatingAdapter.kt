package ru.vpiska.rating


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import ru.vpiska.R
import ru.vpiska.profile.OtherProfileActivity
import java.util.*

class RatingAdapter internal constructor(context: Context, private val layout: Int, private val ratingList: ArrayList<Rating>) : ArrayAdapter<Rating>(context, layout, ratingList) {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val mContext = getContext()

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        val viewHolder: ViewHolder
        if (convertView == null) {
            convertView = inflater.inflate(this.layout, parent, false)
            viewHolder = ViewHolder(convertView!!)
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }
        val product = ratingList[position]

        viewHolder.nameView.text = product.name
        viewHolder.ratingView.text = "Рейтинг: " + product.rating


        viewHolder.linkButton.setOnClickListener {
            val intent = Intent(mContext, OtherProfileActivity::class.java)
            intent.putExtra("id_user", product.id)
            //Toast.makeText(getContext(),  product.getId(), Toast.LENGTH_LONG).show();
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            mContext.startActivity(intent)
        }


        return convertView
    }


    private inner class ViewHolder internal constructor(view: View) {
        internal val linkButton: Button = view.findViewById(R.id.linkButton)
        internal val nameView: TextView = view.findViewById(R.id.nameView)
        internal val ratingView: TextView = view.findViewById(R.id.ratingView)

    }
}