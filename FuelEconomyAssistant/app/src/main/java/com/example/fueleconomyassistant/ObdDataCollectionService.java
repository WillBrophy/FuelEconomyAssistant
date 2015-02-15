package com.example.fueleconomyassistant;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.ArrayList;

import pt.lighthouselabs.obd.commands.SpeedObdCommand;
import pt.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelEconomyObdCommand;
import pt.lighthouselabs.obd.commands.protocol.EchoOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.LineFeedOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.SelectProtocolObdCommand;
import pt.lighthouselabs.obd.commands.protocol.TimeoutObdCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;

public class ObdDataCollectionService extends Service {
    private final IBinder mBinder = new LocalBinder();      // interface for clients that bind
    ArrayList<DataPoint> mRpmHistory;
    ArrayList<DataPoint> mEconomyHistory;
    ArrayList<DataPoint> mSpeedHistory;
    BluetoothSocket mSocket;
    EngineRPMObdCommand mRpmCommand;
    FuelEconomyObdCommand mEconomyCommand;
    SpeedObdCommand mSpeedCommand;

    public boolean initializeSocket(BluetoothSocket socket){
        try {
            mSocket = socket;
            new EchoOffObdCommand().run(mSocket.getInputStream(), mSocket.getOutputStream());
            new LineFeedOffObdCommand().run(mSocket.getInputStream(), mSocket.getOutputStream());
            new TimeoutObdCommand(100).run(mSocket.getInputStream(), mSocket.getOutputStream());
            new SelectProtocolObdCommand(ObdProtocols.AUTO).run(mSocket.getInputStream(), mSocket.getOutputStream());
            mRpmCommand = new EngineRPMObdCommand();
            mEconomyCommand = new FuelEconomyObdCommand();
            mSpeedCommand = new SpeedObdCommand();
        }catch(Exception e){
            return false;
        }
        clearData();
        startScanner();
        return true;
    }

    public void clearData(){
        mRpmHistory = new ArrayList<DataPoint>();
        mEconomyHistory = new ArrayList<DataPoint>();
        mSpeedHistory = new ArrayList<DataPoint>();
    }

    private void startScanner() {
        final Runnable r = new Runnable() {
            public void run() {
                updateValues();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void updateValues() {
        try {
            mSpeedCommand.run(mSocket.getInputStream(), mSocket.getOutputStream());
            mEconomyCommand.run(mSocket.getInputStream(), mSocket.getOutputStream());
            mRpmCommand.run(mSocket.getInputStream(), mSocket.getOutputStream());

        } catch (Exception e){
            e.printStackTrace();
        }
        mSpeedHistory.add(new DataPoint(mSpeedCommand.getFormattedResult()));
        mEconomyHistory.add(new DataPoint(mEconomyCommand.getFormattedResult()));
        mRpmHistory.add(new DataPoint(mRpmCommand.getFormattedResult()));
    }


    public class LocalBinder extends Binder {
        ObdDataCollectionService getService() {
            return ObdDataCollectionService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void testWithToast(){
        //Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
    }

}
