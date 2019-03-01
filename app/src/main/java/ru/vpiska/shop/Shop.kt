package ru.vpiska.shop

import java.util.*

/**
 * Created by Кирилл on 15.12.2018.
 */


class Shop internal constructor(val productId: String, val title: String, price: Double) {
    var price: Double = 0.toDouble()
        internal set


    init {
        this.price = Math.round(price).toDouble()
    }

    companion object {

        var ShopComparator: Comparator<Shop> = Comparator { s1, s2 ->
            //ascending order
            java.lang.Double.compare(s1.price, s2.price)

            //descending order
            //return StudentName2.compareTo(StudentName1);
        }
    }
}
