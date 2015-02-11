package com.example.fueleconomyassistant;

import java.util.Calendar;

/**
 * Created by greenewp on 2/11/2015.
 */
public class DataPoint {
    String mData;
    Calendar mTimeRecorded;

    public DataPoint(String data){
        mData = data;
        mTimeRecorded = Calendar.getInstance();
    }
}
