package com.example.fueleconomyassistant;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
	
	private Button mSettingsButton;
	private Button mMapButton;
	private GraphViewFEA mGraph;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSettingsButton = (Button) findViewById(R.id.settings_button);
		mMapButton = (Button) findViewById(R.id.map_button);
		mGraph = (GraphViewFEA) findViewById(R.id.graph_view);
		
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
		LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[] {
		          new DataPoint(0, 1),
		          new DataPoint(1, 5),
		          new DataPoint(2, 3),
		          new DataPoint(3, 2),
		          new DataPoint(4, 6)
		});
		mGraph.addSeries(series);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
