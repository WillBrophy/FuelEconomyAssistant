package com.example.fueleconomyassistant;


import java.util.List;

import com.google.api.client.util.Key;

/**
 * Created by brophywa on 1/29/2015.
 */
public class PlacesList {
    @Key("html_attributions")
    public String[] htmlAttributions;

    @Key("next_page_token")
    public String nextPageToken;

//    public List results;
    @Key("results")
    public Place[] results;

    @Key("status")
    public String status;
}