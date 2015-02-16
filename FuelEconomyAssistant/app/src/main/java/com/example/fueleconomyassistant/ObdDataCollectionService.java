package com.example.fueleconomyassistant;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Date;

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

    public String getConfirmationString() {
        return "Method Call Successfull";
    }

    //------------------------------------------------------------------
    private ArrayList<ObdDataPoint> mRpmHistory;
    private ArrayList<ObdDataPoint> mSpeedHistory;
    private ArrayList<ObdDataPoint> mFuelEconomyHistory;
    private ArrayList<ObdDataPoint> mFuelConsumptionHistory;
    private ObdDataPoint mFuelLevel;

    private BluetoothSocket mSocket;
    private EngineRPMObdCommand mRpmCommand;
    private SpeedObdCommand mSpeedCommand;
    private FuelEconomyObdCommand mFuelEconomyCommand;
    private FuelLevelObdCommand mFuelLevelObdCommand;
    private FuelConsumptionRateObdCommand mFuelConsumptionCommand;
    public boolean runObdInitialization(BluetoothSocket incomingSocket){
        mSocket = incomingSocket;
        mRpmHistory = new ArrayList<ObdDataPoint>();
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
            beginCollection();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public void beginCollection(){
        new Thread(new Runnable() {
            public void run() {
                //Let the thread sleep while the car computer prepares new data
                while(true) {
                    try {
                        Thread.sleep(1500);
                        new EchoOffObdCommand().run(mSocket.getInputStream(), mSocket.getOutputStream());
                        new LineFeedOffObdCommand().run(mSocket.getInputStream(), mSocket.getOutputStream());
                        new TimeoutObdCommand(1000).run(mSocket.getInputStream(), mSocket.getOutputStream());
                        new SelectProtocolObdCommand(ObdProtocols.AUTO).run(mSocket.getInputStream(), mSocket.getOutputStream());
                        mRpmCommand = new EngineRPMObdCommand();
                        //mSpeedCommand = new SpeedObdCommand();
                        //mFuelEconomyCommand = new FuelEconomyObdCommand();
                        //mFuelLevelObdCommand = new FuelLevelObdCommand();
                        //mFuelConsumptionCommand = new FuelConsumptionRateObdCommand();
                    //Gather the newest data from the vehicle computer
                    mRpmCommand.run(mSocket.getInputStream(), mSocket.getOutputStream());
                    mRpmHistory.add(new ObdDataPoint(new Date(), mRpmCommand.getRPM()));
                    //mSpeedCommand.run(mSocket.getInputStream(), mSocket.getOutputStream());
                    //mSpeedHistory.add(new ObdDataPoint(new Date(), mSpeedCommand.getImperialSpeed()));
                    //mFuelEconomyCommand.run(mSocket.getInputStream(), mSocket.getOutputStream());
                    //mFuelEconomyHistory.add(new ObdDataPoint(new Date(), mRpmCommand.getRPM()));
                    //mFuelConsumptionCommand.run(mSocket.getInputStream(), mSocket.getOutputStream());
                    //mFuelConsumptionHistory.add(new ObdDataPoint(new Date(), mRpmCommand.getRPM()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public ArrayList<ObdDataPoint> getRpmHistory() {
        return mRpmHistory;
    }
}