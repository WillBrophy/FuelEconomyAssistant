package com.example.fueleconomyassistant;

import java.util.Date;

/**
 * Created by greenewp on 2/15/2015.
 */
public class ObdDataPoint{
    private Date mTimeCollected;
    private double mValue;

    public ObdDataPoint(Date timeCollected,double value ){
        this.mTimeCollected = timeCollected;
        this.mValue = value;
    }

    public long getTimeCollected() {
        return mTimeCollected.getTime();
    }

    public double getValue() {
        return mValue;
    }

    @Override
    public String toString() {
        return "ObdDataPoint{" +
                "mTimeCollected=" + mTimeCollected +
                ", mValue=" + mValue +
                '}';
    }
}
