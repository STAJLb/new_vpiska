package ru.vpiska.shop;

import java.util.Comparator;

/**
 * Created by Кирилл on 22.12.2018.
 */

public class ShopApp {

    private String productId,title;
    double money;



    ShopApp(String productId,String title,double price){
        this.productId = productId;
        this.title = title;
        this.money = Math.round(price);
    }

    public static Comparator<ShopApp> ShopComparator = new Comparator<ShopApp>() {

        public int compare(ShopApp s1, ShopApp s2) {


            //ascending order
            return Double.compare(s1.getPrice(), s2.getPrice());

            //descending order
            //return StudentName2.compareTo(StudentName1);
        }};

    public String getProductId() {
        return productId;
    }

    public String getTitle() {
        return title;
    }

    public double getPrice() {
        return money;
    }
}
