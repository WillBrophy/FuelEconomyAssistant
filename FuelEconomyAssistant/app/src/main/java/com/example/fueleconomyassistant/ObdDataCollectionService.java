package com.example.fueleconomyassistant;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ObdDataCollectionService extends Service {
    //
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        ObdDataCollectionService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ObdDataCollectionService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void beingCollection(){
        new Thread(new Runnable() {
            public void run() {
                //Collection code will go here
            }
        }).start();
    }

    public String getConfirmationString() {
        return "Method Call Successfull";
    }
}