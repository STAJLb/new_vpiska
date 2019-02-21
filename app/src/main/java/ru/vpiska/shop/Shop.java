package ru.vpiska.shop;

import java.util.Comparator;

/**
 * Created by Кирилл on 15.12.2018.
 */


public class Shop {
    private String productId,title;
    double price;



    Shop(String productId,String title,double price){
        this.productId = productId;
        this.title = title;
        this.price = Math.round(price);
    }

    public static Comparator<Shop> ShopComparator = new Comparator<Shop>() {

        public int compare(Shop s1, Shop s2) {


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
        return price;
    }
}
