package com.example.fueleconomyassistant;

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
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    private static int mRPMGoal = 2000;
    private static int mSpeedGoal = 55;

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

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    //made static so outside sources could access this service to get data.
    public static ObdDataCollectionService mService;

    private LineGraphSeries<DataPoint> mRpmSeries;
    private boolean mBound;
    private String mPreviousDataGraphed;
    private LineGraphSeries<DataPoint> mMetricFuelEconomySeries;
    private LineGraphSeries<DataPoint> mImperialFuelEconomySeries;
    private LineGraphSeries<DataPoint> mImperialSpeedSeries;
    private LineGraphSeries<DataPoint> mMetricSpeedSeries;
    private String mPreviousUnits;
    private int mEconomyGoal;

    private Timer mUpdateTimer;

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

        mPreviousDataGraphed = "-1";
        mPreviousUnits = "-1";
        mRpmSeries = new LineGraphSeries<DataPoint>();
        mMetricFuelEconomySeries = new LineGraphSeries<DataPoint>();
        mImperialFuelEconomySeries = new LineGraphSeries<DataPoint>();
        mImperialSpeedSeries = new LineGraphSeries<DataPoint>();
        mMetricSpeedSeries = new LineGraphSeries<DataPoint>();
        mGraph.getViewport().setXAxisBoundsManual(true);

        enableBluetooth();

    }

    //---------------------------------------------Code To Bind to OBD Service------------------------------
    //----------------------------------------------------------------------------------------------


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
                if(mUpdateTimer != null){
                    stopUITimer();
                }
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


    //---------------------------------------------------------------------------------------------------
    //---------------------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void updateValues() {
        mUpdateTimer = new Timer();
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final String dataToGraph = prefs.getString("graph_data_pref", "0");
                        final String units = prefs.getString("units_pref", "0");
                        boolean isMetric = units.equals("0");
                        if (!dataToGraph.equals(mPreviousDataGraphed) || !units.equals(mPreviousUnits)) {
                            mGraph.removeAllSeries();
                            updateTitleViews(isMetric);
                            final double dataFormatDivider = (dataToGraph.equals("2") ? 1000.0 : 1.0);
                            mGraphTitle.setText(getGraphTitleFor(dataToGraph, isMetric));
                            mGraph.addSeries(getSeries(dataToGraph, isMetric));
                            mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                                @Override
                                public String formatLabel(double value, boolean isValueX) {
                                    return super.formatLabel(value / (isValueX ? 1.0 : dataFormatDivider), isValueX);
                                }
                            });

                        }
                        mPreviousDataGraphed = dataToGraph;
                        mPreviousUnits = units;

                        //clear graph if the graphed data has changed
                        //preform date calculations

                        //set graph bounds
                        mGraph.getViewport().setXAxisBoundsManual(true);
                        mGraph.getViewport().setMaxX((new Date().getTime() - mService.getCollectionStartTime()) / 1000);
                        mGraph.getViewport().setMinX(((new Date().getTime() - mService.getCollectionStartTime()) / 1000) - 30);
                        mGraph.getViewport().setScrollable(true);

                        //RPM Code
                        updateRPMValues();
                        updateSpeedValues(isMetric);
                        updateEconomyValues(isMetric);
                    }
                });
            }
        };
        mUpdateTimer.scheduleAtFixedRate(t, 0, 500);
    }

    private void updateTitleViews(boolean isMetric){
        mSpeedTitle.setText(getString(R.string.speed, (isMetric ? "km/h" : "mph")));
        mEconomyTitle.setText(getString(R.string.economy, (isMetric ? "L/100km" : "mpg")));
    }

    private String getGraphTitleFor(String dataToGraph, boolean isMetric){
        if (dataToGraph.equals("0")) {
            return getString(R.string.economy, (isMetric ? "L/100km" : "mpg"));
        } else if (dataToGraph.equals("1")) {
            return getString(R.string.speed, (isMetric ? "km/h" : "mph"));
        } else if (dataToGraph.equals("2")) {
            return "RPM x1000";
        }
        return "";
    }

    private LineGraphSeries<DataPoint> getSeries(String dataToGraph, boolean isMetric){
        if (dataToGraph.equals("0")) {
            if (isMetric) {
                return mMetricFuelEconomySeries;
            } else {
               return mImperialFuelEconomySeries;
            }
        } else if (dataToGraph.equals("1")) {
            if (isMetric) {
                return mMetricSpeedSeries;
            } else {
                return mImperialSpeedSeries;
            }
        } else if (dataToGraph.equals("2")) {
            return mRpmSeries;
        }

        return new LineGraphSeries<DataPoint>();
    }

    private void updateRPMValues(){
        ArrayList<ObdDataPoint> rpmData = mService.getRpmHistory();
        mEngine.setText("" + (int) (rpmData.get(rpmData.size() - 1).getValue()));
        double goodness = Math.abs(rpmData.get(rpmData.size() - 1).getValue() - mRPMGoal) / mRPMGoal;
        mEngine.setBackgroundColor(Color.argb(175, (int) (255 * goodness), (int) (255 * (1 - goodness)), 0));
        final DataPoint currentRpmPoint = new DataPoint((double) (rpmData.get(rpmData.size() - 1).getTimeCollected() - mService.getCollectionStartTime()) / 1000, rpmData.get(rpmData.size() - 1).getValue());
        if (mRpmSeries.getHighestValueX() < currentRpmPoint.getX())
            mRpmSeries.appendData(currentRpmPoint, true, 500);
    }

    private void updateSpeedValues(boolean isMetric){
        ArrayList<ObdDataPoint> imperialSpeedData = mService.getImperialSpeedHistory();
        //update the background color
        double goodnessSpeed = Math.abs(imperialSpeedData.get(imperialSpeedData.size() - 1).getValue() - mSpeedGoal) / mSpeedGoal;
        if (imperialSpeedData.get(imperialSpeedData.size() - 1).getValue() > mSpeedGoal * 2) {
            goodnessSpeed = 1;
        }
        mSpeed.setBackgroundColor(Color.argb(175, (int) (255 * goodnessSpeed), (int) (255 * (1 - goodnessSpeed)), 0));
        if (isMetric) {
            ArrayList<ObdDataPoint> metricSpeedData = mService.getMetricSpeedHistory();
            mSpeed.setText("" + (int) (metricSpeedData.get(metricSpeedData.size() - 1).getValue()));
            final DataPoint currentMetSpeedPoint = new DataPoint((double) (metricSpeedData.get(metricSpeedData.size() - 1).getTimeCollected() - mService.getCollectionStartTime()) / 1000, metricSpeedData.get(metricSpeedData.size() - 1).getValue());
            if (mMetricSpeedSeries.getHighestValueX() < currentMetSpeedPoint.getX())
                mMetricSpeedSeries.appendData(currentMetSpeedPoint, true, 500);
        } else {
            mSpeed.setText("" + (int) (imperialSpeedData.get(imperialSpeedData.size() - 1).getValue()));
            final DataPoint currentImpSpeedPoint = new DataPoint((double) (imperialSpeedData.get(imperialSpeedData.size() - 1).getTimeCollected() - mService.getCollectionStartTime()) / 1000, imperialSpeedData.get(imperialSpeedData.size() - 1).getValue());
            if (mImperialSpeedSeries.getHighestValueX() < currentImpSpeedPoint.getX())
                mImperialSpeedSeries.appendData(currentImpSpeedPoint, true, 500);
        }
    }

    private void updateEconomyValues(boolean isMetric){
        double goodnessEconomy;
        ArrayList<ObdDataPoint> imperialFuelData = mService.getImperialFuelEconomyHistory();
        goodnessEconomy = 1 - imperialFuelData.get(imperialFuelData.size() - 1).getValue() / mEconomyGoal;
        if (imperialFuelData.get(imperialFuelData.size() - 1).getValue() > mEconomyGoal) {
            goodnessEconomy = 0;
        }
        mEconomy.setBackgroundColor(Color.argb(175, (int) (255 * goodnessEconomy), (int) (255 * (1 - goodnessEconomy)), 0));

        if (isMetric) {
            ArrayList<ObdDataPoint> metricFuelData = mService.getMetricFuelEconomyHistory();
            mEconomy.setText("" + (int) (metricFuelData.get(metricFuelData.size() - 1).getValue()));
            final DataPoint currentMetFuelPoint = new DataPoint((double) (metricFuelData.get(metricFuelData.size() - 1).getTimeCollected() - mService.getCollectionStartTime()) / 1000, metricFuelData.get(metricFuelData.size() - 1).getValue());
            if (mMetricFuelEconomySeries.getHighestValueX() < currentMetFuelPoint.getX())
                mMetricFuelEconomySeries.appendData(currentMetFuelPoint, true, 500);
        } else {
            mEconomy.setText("" + (int) (imperialFuelData.get(imperialFuelData.size() - 1).getValue()));
            final DataPoint currentImpFuelPoint = new DataPoint((double) (imperialFuelData.get(imperialFuelData.size() - 1).getTimeCollected() - mService.getCollectionStartTime()) / 1000, imperialFuelData.get(imperialFuelData.size() - 1).getValue());
            if (mImperialFuelEconomySeries.getHighestValueX() < currentImpFuelPoint.getX())
                mImperialFuelEconomySeries.appendData(currentImpFuelPoint, true, 500);
        }
    }

    //restart gui updater on resume
    @Override
    public void onRestart() {
        super.onRestart();
        mEngine.setText("--");
        if(mUpdateTimer != null){
            stopUITimer();
        }
        updateValues();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            mEconomyGoal = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("economy_goal_pref", "30"));
        } catch (NumberFormatException e) {
            mEconomyGoal = 30;
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        stopUITimer();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private void stopUITimer(){
        if (mUpdateTimer != null) {
            mUpdateTimer.cancel();
            mUpdateTimer.purge();
            mUpdateTimer = null;
        }
    }

    public void chooseBluetoothAdapter() {
        ArrayList<String> deviceStrs = new ArrayList<String>();
        final ArrayList<String> devices = new ArrayList<String>();

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
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice,
                deviceStrs);

        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                String deviceAddress = devices.get(position);
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                try {
                    mSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                    mSocket.connect();

                    Toast toast = Toast.makeText(getApplicationContext(), "Connection Successful", Toast.LENGTH_LONG);
                    toast.show();
                    //Begin the OBD data service
                    startBluetoothService();

                } catch (IOException e) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Connection NOT Successful", Toast.LENGTH_LONG);
                    toast.show();
                }

            }
        });
        alertDialog.setTitle("Choose Bluetooth device");
        alertDialog.show();
    }

    public void enableBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
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
                    } else {
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
        }else{
            startTestService();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                chooseBluetoothAdapter();
            }
        }
    }

    public void startTestService() {
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to the Service, cast the IBinder and get Service instance
                ObdDataCollectionService.LocalBinder binder = (ObdDataCollectionService.LocalBinder) service;
                mService = binder.getService();
                mService.beginTestModeCollection();
                if(mUpdateTimer != null){
                    stopUITimer();
                }
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
