package com.example.fueleconomyassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    private Button mSettingsButton;
    private Button mMapButton;
    private GraphViewFEA mGraph;

    private ServiceConnection mConnection;

    private TextView mEconomy;
    private TextView mSpeed;
    private TextView mEngine;
    private Timer mUpdateTimer;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;

    private boolean mBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSettingsButton = (Button) findViewById(R.id.settings_button);
        mMapButton = (Button) findViewById(R.id.map_button);
        mGraph = (GraphViewFEA) findViewById(R.id.graph_view);
        mEconomy = (TextView) findViewById(R.id.economy_value);
        mSpeed = (TextView) findViewById(R.id.speed_value);
        mEngine = (TextView) findViewById(R.id.engine_value);

        mSettingsButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
            }
        });

        mMapButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, FuelMapActivity.class);
                startActivity(i);
            }
        });
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[]{
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        mGraph.addSeries(series);

        Timer mUpdateTimer = new Timer();
        mUpdateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateValues();
            }
        }, 1000, 1000);

        enableBluetooth();

    }
    //---------------------------------------------Code To Bind to OBD Service------------------------------
    //----------------------------------------------------------------------------------------------
    ObdDataCollectionService mService;

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                ObdDataCollectionService.LocalBinder binder = (ObdDataCollectionService.LocalBinder) service;
                mService = binder.getService();
                mBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };
        Intent intent = new Intent(this, ObdDataCollectionService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        //String s = "" + mService.getConfirmationString();
        //Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        Log.d("s", "Intent Created and Service Bound");
        if(mBound) {
            Log.d("s", "mBound = TRUE");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    //---------------------------------------------------------------------------------------------------
    //---------------------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void updateValues() {
        //Update the economy, speed, engine

    }


    public void checkBluetooth() {
        ArrayList deviceStrs = new ArrayList();
        final ArrayList devices = new ArrayList();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceStrs.add(device.getName() + "\n" + device.getAddress());
                devices.add(device.getAddress());
            }
        }

        // show list
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice,
                deviceStrs.toArray(new String[deviceStrs.size()]));

        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                String deviceAddress = (String) devices.get(position);
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);

                try {

                    mSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                    mSocket.connect();

                    Toast toast = Toast.makeText(getApplicationContext(), "Connection Successful", Toast.LENGTH_LONG);
                    toast.show();

                } catch (Exception e) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Connection NOT Successful", Toast.LENGTH_LONG);
                    toast.show();

                }

            }
        });



        alertDialog.setTitle("Choose Bluetooth device");
        alertDialog.show();


    }

    public void enableBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {

        }else{
            //addToReport("Bluetooth capability confirmed");
            if (!mBluetoothAdapter.isEnabled()) {
                //addToReport("Bluetooth adapter disabled, requesting user intervention");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode != RESULT_OK) {
                //addToReport("Request to enable bluetooth was denied");
            }else{
                checkBluetooth();
                //addToReport("Bluetooth adapter successfully enabled");
            }
        }
    }

}
