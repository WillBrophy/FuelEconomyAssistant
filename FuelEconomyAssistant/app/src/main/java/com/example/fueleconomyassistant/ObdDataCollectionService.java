package com.example.fueleconomyassistant;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import pt.lighthouselabs.obd.commands.SpeedObdCommand;
import pt.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelConsumptionRateObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelEconomyObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelLevelObdCommand;
import pt.lighthouselabs.obd.commands.protocol.EchoOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.LineFeedOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.SelectProtocolObdCommand;
import pt.lighthouselabs.obd.commands.protocol.TimeoutObdCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;

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
                //Let the thread sleep while the car computer prepares new data
                try{
                    Thread.sleep(50);
                }catch(Exception e){
                    e.printStackTrace();
                }
                //Gather the newest data from the vehicle computer

            }
        }).start();
    }

    public String getConfirmationString() {
        return "Method Call Successfull";
    }

    //------------------------------------------------------------------
    private BluetoothSocket mSocket;
    private EngineRPMObdCommand mRpmCommand;
    private SpeedObdCommand mSpeedCommand;
    private FuelEconomyObdCommand mFuelEconomyCommand;
    private FuelLevelObdCommand mFuelLevelObdCommand;
    private FuelConsumptionRateObdCommand mFuelConsumptionCommand;
    public void runObdInitialization(BluetoothSocket incomingSocket){
        mSocket = incomingSocket;
        try {
            new EchoOffObdCommand().run(mSocket.getInputStream(), mSocket.getOutputStream());
            new LineFeedOffObdCommand().run(mSocket.getInputStream(), mSocket.getOutputStream());
            new TimeoutObdCommand(100).run(mSocket.getInputStream(), mSocket.getOutputStream());
            new SelectProtocolObdCommand(ObdProtocols.AUTO).run(mSocket.getInputStream(), mSocket.getOutputStream());
            mRpmCommand = new EngineRPMObdCommand();
            mSpeedCommand = new SpeedObdCommand();
            mFuelEconomyCommand = new FuelEconomyObdCommand();
            mFuelLevelObdCommand = new FuelLevelObdCommand();
            mFuelConsumptionCommand = new FuelConsumptionRateObdCommand();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}