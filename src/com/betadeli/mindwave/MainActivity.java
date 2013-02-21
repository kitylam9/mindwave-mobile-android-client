package com.betadeli.mindwave;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.neurosky.thinkgear.TGDevice;

public class MainActivity extends Activity {

	private TGDevice tgDevice;
	private BluetoothAdapter bluetoothAdapter;
	
	private String apiEndpointURL;
	
	private ProgressBar meditationVal;
	private ProgressBar attentionVal;
	private ProgressBar blinkVal;
	
	private TextView meditationStr;
	private TextView attentionStr;
	private TextView blinkStr;
	
	private Button button;
	private ProgressBar signal;
	
	private ScrollView svL;
	private TextView tvL;
	
	private ScrollView svR;
	private TextView tvR;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		apiEndpointURL = preferences.getString("apiEndpointURL", this.getString(R.string.api_endpoint_url));
		preferences.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {			
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				apiEndpointURL = sharedPreferences.getString("apiEndpointURL", apiEndpointURL);
			}
		});
		
		meditationVal = (ProgressBar)findViewById(R.id.meditationVal);
		attentionVal = (ProgressBar)findViewById(R.id.attentionVal);
		blinkVal = (ProgressBar)findViewById(R.id.blinkVal);
		
		meditationStr = (TextView)findViewById(R.id.meditationStr);
		attentionStr = (TextView)findViewById(R.id.attentionStr);	
		blinkStr = (TextView)findViewById(R.id.blinkStr);
		
		button = (Button)findViewById(R.id.button);
		button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connect();
            }
        });
		
		signal = (ProgressBar)findViewById(R.id.signal);
		
		svL = (ScrollView)findViewById(R.id.svL);
		tvL = (TextView)findViewById(R.id.tvL);
		tvL.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				svL.fullScroll(ScrollView.FOCUS_DOWN);
				if(tvL.getText().length() > 10000) {
					tvL.setText("");
				}
			}
		});
	    tvL.append("Android Version: " + Integer.valueOf(android.os.Build.VERSION.SDK_INT) + "\n" );
	    
	    svR = (ScrollView)findViewById(R.id.svR);
		tvR = (TextView)findViewById(R.id.tvR);
		tvR.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				svR.fullScroll(ScrollView.FOCUS_DOWN);
				if(tvR.getText().length() > 10000) {
					tvR.setText("");
				}
			}
		});
		
		new SendDataTask().execute("msg=App Ready");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case (R.id.menu_settings):
				Intent prefs = new Intent(this, MainPreferencesActivity.class);
				startActivity(prefs);
				break;
		}
		return false;
	}
	
	@Override
	public void onDestroy() {
		if(tgDevice != null) {
			tgDevice.close();
		}
	    super.onDestroy();
	}
	
	/**
	 * Handles messages from threads
	 */
	private final Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String text = (String) msg.obj;
			tvL.append(text);
		}
	};
	
	/**
	 * Handles messages from TGDevice
	 */
	private final Handler mindHandler = new Handler() {
	    @Override
	    public void handleMessage(Message msg) {
	    	switch (msg.what) {
		        case TGDevice.MSG_STATE_CHANGE:
		            switch (msg.arg1) {
		                case TGDevice.STATE_IDLE:
		                    break;
		                case TGDevice.STATE_CONNECTING:		    
		                	new SendDataTask().execute("msg=Connecting");
		                	tvL.append("Connecting\n");
		                	button.setEnabled(false);
		                	break;		                    
		                case TGDevice.STATE_CONNECTED:
		                	new SendDataTask().execute("msg=Connected");
		                	tvL.append("Connected\n");
		                	tgDevice.start();
		                    break;
		                case TGDevice.STATE_NOT_FOUND:
		                	new SendDataTask().execute("msg=Not Found");
		                	tvL.append("Not Found\n");
		                	button.setEnabled(true);
		                	break;
		                case TGDevice.STATE_NOT_PAIRED:
		                	new SendDataTask().execute("msg=Not Paired");
		                	tvL.append("Not Paired\n");
		                	button.setEnabled(true);
		                	break;
		                case TGDevice.STATE_DISCONNECTED:
		                	new SendDataTask().execute("msg=Disconnected&sgnl=-1");
		                	tvL.append("Disconnected\n");
		                	button.setEnabled(true);
		                	reset();
		                	break;
		                default:
		                	break;
		            }
		            break;
		        case TGDevice.MSG_MEDITATION:
		        	new SendDataTask().execute("mdtt=" + msg.arg1);
		        	meditationStr.setText("" + msg.arg1);
		        	meditationVal.setProgress(msg.arg1);
		        	break;
		        case TGDevice.MSG_ATTENTION:
		        	new SendDataTask().execute("attn=" + msg.arg1);
		        	attentionStr.setText("" + msg.arg1);
		        	attentionVal.setProgress(msg.arg1);
		        	break;
		        case TGDevice.MSG_BLINK:
		        	new SendDataTask().execute("blnk=" + msg.arg1);
		        	blinkStr.setText("" + msg.arg1);
		        	blinkVal.setProgress(msg.arg1);
		        	if(msg.arg1 > 70) {
		        		blinkStr.setTextColor(Color.RED);
		        	} else {
		        		blinkStr.setTextColor(Color.BLACK);
		        	}
		        	break;
		        case TGDevice.MSG_EEG_POWER:
		        	//TGEegPower ep = (TGEegPower)msg.obj;
		        	//tvR.append("EEG: " + ep.delta + "\n");
		        	break;
		        case TGDevice.MSG_POOR_SIGNAL:
		        	int val = (200 - msg.arg1) / 2;
		        	new SendDataTask().execute("sgnl=" + val);
		        	signal.setProgress(val);
		            break;
		        case TGDevice.MSG_LOW_BATTERY:
		        	tvL.append("Low Battery: " + msg.arg1 + "\n");
		        	break;
		        default:
		        	break;
		    }
	    }
	};
	
	private void connect() {
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    if(bluetoothAdapter == null) {
	    	tvL.append("Bluetooth Not Available");
	    } else {
	    	if(!bluetoothAdapter.isEnabled()) {
	    		tvL.append("Bluetooth Not Enabled\n");
	    		return;
	    	}
	    	
	    	try {
	    		tgDevice = new TGDevice(bluetoothAdapter, mindHandler);
	    		tgDevice.connect(true); 
	    	} catch(Exception ex) {
	    		tvL.append("Excepiton: " + ex.getMessage() + "\n");
	    		exit(6000);
	    		return;
	    	}
	    } 
	}
	
	private void reset() {
		meditationVal.setProgress(0);
		attentionVal.setProgress(0);
		blinkVal.setProgress(0);
		
		meditationStr.setText("0");
		attentionStr.setText("0");
		blinkStr.setText("0");
		blinkStr.setTextColor(Color.BLACK);
		
		signal.setProgress(0);
		
		tvR.setText("");
	}
	
	private class SendDataTask extends AsyncTask<String, Integer, Integer> {
	     protected Integer doInBackground(String... query) {
	         return send((String)query[0]);
	     }
	 }
	
	private int send(String query) {
		int code = -1;
		
		try {
			URL url = new URL(apiEndpointURL + "?" + query);
			System.out.println(url);
	        URLConnection connection = url.openConnection();
	    	HttpURLConnection httpConnection = (HttpURLConnection) connection;
	        httpConnection.setAllowUserInteraction(false);
	        httpConnection.setInstanceFollowRedirects(true);
	        httpConnection.setRequestMethod("POST");
	        httpConnection.connect();
	        code = httpConnection.getResponseCode();
		} catch(Exception ex) {
			Message message = new Message();
			message.obj = "HTTP Error\n";
			messageHandler.sendMessage(message);
		}
        
        return code;
	}
	
	private void exit(int delay) {
		new CountDownTimer(delay, 1000) {
		     public void onTick(long millisUntilFinished) {
		         tvL.append("Exiting in... " + millisUntilFinished / 1000 + "s \n");
		     }
		     public void onFinish() {
		         tvL.append("Bye!");
		         finish();
		     }
		}.start();
	}

}
