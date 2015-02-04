package com.example.fueleconomyassistant;


import com.google.api.client.util.Key;

/**
 * Created by brophywa on 1/29/2015.
 */
public class Place {

    @Key("id")
    public String id;

    @Key("name")
    public String name;

    @Key("reference")
    public String reference;

    @Key("geometry")
    public Geometry geometry;

    @Key("opening_hours")
    public OpeningHours openingHours;

    @Key("rating")
    public double rating;



    @Override
    public String toString() {
        return name + " - " + rating + " - " ;
    }


    public static class Geometry {
        @Key("location")
        public Location location;

        public static class Location{
            @Key("lat")
            public double lat;

            @Key("lng")
            public double lng;
        }
    }

    public static class OpeningHours{
        @Key("open_now")
        public boolean openNow;

        @Key("weekday_text")
        public String[] weekdayText;
    }

}