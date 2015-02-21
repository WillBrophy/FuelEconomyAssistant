package com.example.fueleconomyassistant;

import com.jjoe64.graphview.series.DataPoint;

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

    public double getTimeCollected()  {
         return (double) mTimeCollected.getTime();
    }

    public double getValue() {
        return mValue;
    }

    public DataPoint convertToGraphingDataPoint(ObdDataCollectionService service){
        return new DataPoint((this.getTimeCollected() - service.getCollectionStartTime())/1000,this.getValue());
    }

    @Override
    public String toString() {
        return "ObdDataPoint{" +
                "mTimeCollected=" + mTimeCollected +
                ", mValue=" + mValue +
                '}';
    }
}
