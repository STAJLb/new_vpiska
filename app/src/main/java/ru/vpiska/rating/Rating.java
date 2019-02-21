package ru.vpiska.rating;

/**
 * Created by Кирилл on 23.10.2018.
 */

public class Rating {
    private String name,rating,id;



    Rating(String name,String rating,String id){
        this.id = id;
        this.name = name;
        this.rating = rating;
    }

    public String getName(){
        return this.name;
    }

    public String getRating(){
        return this.rating;
    }

    public String getId() {
        return id;
    }

}
