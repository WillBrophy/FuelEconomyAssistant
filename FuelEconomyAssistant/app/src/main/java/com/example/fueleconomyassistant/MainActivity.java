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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
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
    private TextView mGraphTitle;
    private TextView mEconomyTitle;
    private TextView mSpeedTitle;
    private TextView mRpmTitle;
    private Timer mUpdateTimer;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    private LineGraphSeries<DataPoint> mRpmSeries;
    private boolean mBound;
    private boolean runViewUpdate;
    private double oldX = -100;
    private String mPreviousDataGraphed;
    private LineGraphSeries<DataPoint> mMetricFuelEconomySeries;
    private LineGraphSeries<DataPoint> mImperialFuelEconomySeries;
    private LineGraphSeries<DataPoint> mImperialSpeedSeries;
    private LineGraphSeries<DataPoint> mMetricSpeedSeries;
    private String mPreviousUnits;

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
        mGraphTitle = (TextView) findViewById(R.id.graph_title);
        mEconomyTitle = (TextView) findViewById(R.id.economy_title);
        mRpmTitle = (TextView) findViewById(R.id.engine_title);
        mSpeedTitle = (TextView) findViewById(R.id.speed_title);
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
        /*
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[]{
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        */
        mPreviousDataGraphed = "-1";
        mPreviousUnits = "-1";
        mRpmSeries = new LineGraphSeries<DataPoint>();
        mMetricFuelEconomySeries = new LineGraphSeries<DataPoint>();
        mImperialFuelEconomySeries = new LineGraphSeries<DataPoint>();
        mImperialSpeedSeries = new LineGraphSeries<DataPoint>();
        mMetricSpeedSeries = new LineGraphSeries<DataPoint>();
//        mRpmSeries = new LineGraphSeries<DataPoint>(new DataPoint[500]);
        //mGraph.addSeries(mRpmSeries);
        mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    return super.formatLabel((int) value, isValueX);
                } else {
                    // show currency for y values
                    return super.formatLabel(value, isValueX);
                }
            }
        });
        mGraph.getViewport().setXAxisBoundsManual(true);
//        mGraph.getViewport().setMaxX(new Date().getTime());
//        mGraph.getViewport().setMinX(new Date().getTime() - 50*60*1000);
//        mGraph.getViewport().setScrollable(true);
        enableBluetooth();

    }
    //---------------------------------------------Code To Bind to OBD Service------------------------------
    //----------------------------------------------------------------------------------------------
    ObdDataCollectionService mService;


    private void startBluetoothService() {

        // Bind to Service
        mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to the Service, cast the IBinder and get Service instance
                ObdDataCollectionService.LocalBinder binder = (ObdDataCollectionService.LocalBinder) service;
                mService = binder.getService();
                mService.beginPlxCollection(mSocket);
                runViewUpdate = true;
                updateValues();
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
    }
    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        runViewUpdate = false;
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
        Log.d("WINFIELD","UPDATEVALUES CALLED");
        if(runViewUpdate){
            Log.d("WINFIELD","runViewUpdate TRUE");
        }else{
            Log.d("WINFIELD","runViewUpdate FALSE");
        }
        new Thread(new Runnable() {
            public void run() {
                //Let the thread sleep to compensate for graph interval
                 ArrayList<ObdDataPoint> currentData;
                while (runViewUpdate){
                    try {
                        Thread.sleep(1000);
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

                        final String dataToGraph = prefs.getString("graph_data_pref", "0");
                        final String units = prefs.getString("units_pref", "0");
                        //if(!dataToGraph.equals( mPreviousDataGraphed) || !units.equals(mPreviousUnits)) {
                            mGraph.removeAllSeries();
                            if(units.equals("0")){
                                mSpeedTitle.setText("Speed(km/h)");
                                mEconomyTitle.setText("Economy(L/100km)");
                            }else{
                                mSpeedTitle.setText("Speed(mph)");
                                mEconomyTitle.setText("Economy(mpg)");
                            }
                            if (dataToGraph.equals("0")) {
                                mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
                                    @Override
                                    public String formatLabel(double value, boolean isValueX) {
                                        if (isValueX) {
                                            return super.formatLabel((int) value, isValueX);
                                        } else {
                                            // show currency for y values
                                            return super.formatLabel(value, isValueX);
                                        }
                                    }
                                });
                                if(units.equals("0")) {
                                    mGraphTitle.setText("Economy(L/100Km)");
                                    mGraph.addSeries(mMetricFuelEconomySeries);
                                }else{
                                    mGraphTitle.setText("Economy(mpg)");
                                    mGraph.addSeries(mImperialFuelEconomySeries);
                                }
                            } else if (dataToGraph.equals("1")) {
                                mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
                                    @Override
                                    public String formatLabel(double value, boolean isValueX) {
                                        if (isValueX) {
                                            return super.formatLabel((int) value, isValueX);
                                        } else {
                                            // show currency for y values
                                            return super.formatLabel(value, isValueX);
                                        }
                                    }
                                });
                                    if(units.equals("0")){
                                        mGraphTitle.setText("Speed(km/h)");
                                        mGraph.addSeries(mMetricSpeedSeries);
                                    }else{
                                        mGraphTitle.setText("Speed(mph)");
                                        mGraph.addSeries(mImperialSpeedSeries);
                                    }
                            } else if (dataToGraph.equals("2")) {
                                mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
                                    @Override
                                    public String formatLabel(double value, boolean isValueX) {
                                        if (isValueX) {
                                            return super.formatLabel((int) value, isValueX);
                                        } else {
                                            // show currency for y values
                                            return super.formatLabel(value/1000, isValueX);
                                        }
                                    }
                                });
                                mGraphTitle.setText("RPM x1000");
                                mGraph.addSeries(mRpmSeries);
                            }
                        //}
                        mPreviousDataGraphed = dataToGraph;
                        mPreviousUnits = units;

                    //clear graph if the graphed data has changed
                        Log.d("WINFIELD","Data to graph: " + dataToGraph);
                    //preform date calculations
                        final long currentTime = new Date().getTime();
                        long oldestTime = currentTime - 5*60*1000;
                    //Update Rpm Values Here
                        currentData = mService.getRpmHistory();
                        final ArrayList<ObdDataPoint> currentDataFinal = currentData;
                        //mEngine.setText("" + (int) (currentDataFinal.get(currentDataFinal.size() - 1).getValue()));
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run(){
                                //set graph bounds
                                mGraph.getViewport().setXAxisBoundsManual(true);
                                mGraph.getViewport().setMaxX((new Date().getTime() - mService.getCollectionStartTime()) / 1000);
                                mGraph.getViewport().setMinX(((new Date().getTime() - mService.getCollectionStartTime()) / 1000) - 15);
                                mGraph.getViewport().setScrollable(true);

                                    //RPM Code
                                    mEngine.setText("" + (int) (currentDataFinal.get(currentDataFinal.size() - 1).getValue()));
                                //if(dataToGraph == "2") {
                                    final DataPoint currentRpmPoint = new DataPoint((double) (currentDataFinal.get(currentDataFinal.size() - 1).getTimeCollected() - mService.getCollectionStartTime()) / 1000, (double) currentDataFinal.get(currentDataFinal.size() - 1).getValue());
                                    mRpmSeries.appendData(currentRpmPoint, true, 500);
                                //}
                                    //Metric Fuel Economy Code
                                    ArrayList<ObdDataPoint> metricFuelData = mService.getMetricFuelEconomyHistory();
                                    mEconomy.setText("" + (int) (metricFuelData.get(metricFuelData.size() - 1).getValue()));
                                    final DataPoint currentMetFuelPoint = new DataPoint((double) (metricFuelData.get(metricFuelData.size() - 1).getTimeCollected() - mService.getCollectionStartTime()) / 1000, (double) metricFuelData.get(metricFuelData.size() - 1).getValue());
                                    mMetricFuelEconomySeries.appendData(currentMetFuelPoint, true, 500);
                                //}
                                    //Imperial Fuel Economy Code
                                    ArrayList<ObdDataPoint> imperialFuelData = mService.getImperialFuelEconomyHistory();
                                    mEconomy.setText("" + (int) (imperialFuelData.get(imperialFuelData.size() - 1).getValue()));
                                    final DataPoint currentImpFuelPoint = new DataPoint((double) (imperialFuelData.get(imperialFuelData.size() - 1).getTimeCollected() - mService.getCollectionStartTime()) / 1000, (double) imperialFuelData.get(imperialFuelData.size() - 1).getValue());
                                    mImperialFuelEconomySeries.appendData(currentImpFuelPoint, true, 500);
                                //
                                    //Imperial Speed code
                                ArrayList<ObdDataPoint> imperialSpeedData = mService.getImperialSpeedHistory();
                                mEconomy.setText("" + (int) (imperialSpeedData.get(imperialSpeedData.size() - 1).getValue()));
                                final DataPoint currentImpSpeedPoint = new DataPoint((double) (imperialSpeedData.get(imperialSpeedData.size() - 1).getTimeCollected() - mService.getCollectionStartTime()) / 1000, (double) imperialSpeedData.get(imperialSpeedData.size() - 1).getValue());
                                mImperialSpeedSeries.appendData(currentImpSpeedPoint, true, 500);

                                    //Metric Speed Code
                                ArrayList<ObdDataPoint> metricSpeedData = mService.getMetricSpeedHistory();
                                mEconomy.setText("" + (int) (metricSpeedData.get(metricSpeedData.size() - 1).getValue()));
                                final DataPoint currentMetSpeedPoint = new DataPoint((double) (metricSpeedData.get(metricSpeedData.size() - 1).getTimeCollected() - mService.getCollectionStartTime()) / 1000, (double) metricSpeedData.get(metricSpeedData.size() - 1).getValue());
                                mMetricSpeedSeries.appendData(currentMetSpeedPoint, true, 500);
                                }
                        });
                        Log.d("WINFIELD","mGraphUpdated");
                        //Update Fuel Economy Values Here
                        //Update Speed Values Here
                        //Update Fuel Consumption Values here
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }
            }
        }).start();
    }

    //restart gui updater on resume
    @Override
    public void onRestart(){
        super.onRestart();
        mEngine.setText("--");
        runViewUpdate = true;
        updateValues();
    }

    public void chooseBluetoothAdapter() {
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



        // show list of available bluetooth devices
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
                    //Begin the OBD data service
                    startBluetoothService();

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
            //Bluetooth Capability Confirmed
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.plx_dialog_message).setTitle(R.string.plx_dialog_title);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //User Chose to Enable Bluetooth
                    if (!mBluetoothAdapter.isEnabled()) {
                        //addToReport("Bluetooth adapter disabled, requesting user intervention");
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }else{
                        chooseBluetoothAdapter();
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                    startTestService();

                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
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
                chooseBluetoothAdapter();
                //addToReport("Bluetooth adapter successfully enabled");
            }
        }
    }
    public void startTestService(){
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to the Service, cast the IBinder and get Service instance
                ObdDataCollectionService.LocalBinder binder = (ObdDataCollectionService.LocalBinder) service;
                mService = binder.getService();
                mService.beginTestModeCollection();
                runViewUpdate = true;
                updateValues();
                mBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };
        Intent intent = new Intent(this, ObdDataCollectionService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
}