package com.redevs.crapforcoins;

import java.text.DecimalFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, OnSharedPreferenceChangeListener {
	
	//UI Elements
    TextView mainTimerText, mainMoneyText;
    Button timerButton, resetButton, confirmButton, statisticsButton;
    SharedPreferences mySharedPref;
    Notification notification;
    //Variables
    private Handler mHandler = new Handler();
    private long startTime, elapsedTime, pauseTime;
    private double money;
    private String hours, minutes, seconds, milliseconds;
    private String moneyMade;
    private long secs, mins, hrs;
	private boolean stopped = false;
	//Settings Shared Pref Variables
	private float rate;
	private int frequencyInt;
	private int currencyInt;
	private String currency, frequency;
	private float hoursPerWeek;
	private boolean useNotification;
	private boolean started = false;
	//Constants
	private final int REFRESH_RATE = 10;
	private final boolean CLICKABLE = true;
	private final boolean UNCLICKABLE = false;
	private final int STAT_DIALOG = 0;
	private final int ABOUT_DIALOG = 1;
	private final int FIRST_DIALOG = 2;
	private final int NOTIFICATION_CODE = 1234;
	//Statistics
	private int totalSessions;
	private double totalMoneyMade;
	private double averageMoneyMade;
	private double highestPayout;
	private long averageSessionTime;
	private long totalTimeSpent;
	private long longestSession;
	//End Statistics

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);	
        
        loadPref();
        
        //UI Init
        mainTimerText = (TextView) findViewById(R.id.mainTimerView);
        mainTimerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        mainTimerText.setText("00:00:00:00");
        
        mainMoneyText = (TextView) findViewById(R.id.mainMoneyView);
        mainMoneyText.setText("");
        
        timerButton = (Button) findViewById(R.id.TimerButton);
        timerButton.setText("Start Timer");
        timerButton.setOnClickListener(this);
        
        resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setText("Reset Timer");
        resetButton.setOnClickListener(this);
        resetButton.setVisibility(View.INVISIBLE);
        resetButton.setClickable(UNCLICKABLE);
        
        confirmButton = (Button) findViewById(R.id.confirmButton);
        confirmButton.setText("Confirm");
        confirmButton.setOnClickListener(this);
        confirmButton.setVisibility(View.INVISIBLE);
        confirmButton.setClickable(UNCLICKABLE);
        
        statisticsButton = (Button) findViewById(R.id.statisticsButton);
        statisticsButton.setText("Statistics");
        statisticsButton.setOnClickListener(this);
        
        checkForFirstTime();
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        mySharedPref.registerOnSharedPreferenceChangeListener(this);
        loadPref();
        removeNotification();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        mySharedPref.unregisterOnSharedPreferenceChangeListener(this);
        loadPref();
        if(stopped && useNotification){
        	createNotification();
        }
    }
    
	protected void makeToast(String toastText) {
		Toast toast = Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT);
		toast.show();
	}
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.menu_settings:
			startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
			return true;
		case R.id.menu_about:
			makeDialog(ABOUT_DIALOG);
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.TimerButton:
			if(stopped == true){ 		//State INIT, 3, 2
				timerButton.setText("Start Timer");
				stopClick();
				stopped = false;
				started = true;
			}
			else{						//State INIT to 1
				timerButton.setText("Stop Timer");
				startClick();
				stopped = true;
			}
			break;
		case R.id.confirmButton:  //State 2 to 
			confirmClick();
			break;
		case R.id.resetButton:
			resetClick();
			break;
		case R.id.statisticsButton:
			statisticsClick();
			break;
		}
	}
	
	private void startClick(){
		if(!started){	//Reset or Initial Launch
			startTime = System.currentTimeMillis();
		}
		else{			//Continuation of timer
			startTime = System.currentTimeMillis() - pauseTime;
		}
		mHandler.removeCallbacks(startTimer);
		mHandler.postDelayed(startTimer, 0);
		DecimalFormat form;
		String formattedRate;
		if((currencyInt == 0)||(currencyInt == 1)||(currencyInt == 3)){
			form = new DecimalFormat("0.00");
			formattedRate = String.valueOf(form.format(rate));
		}
		else{
			form = new DecimalFormat("0");
			formattedRate = String.valueOf(form.format(rate));
		}
		makeToast("Making " + currency + formattedRate + " per " + frequency);
		confirmButton.setVisibility(View.INVISIBLE);
		resetButton.setVisibility(View.INVISIBLE);
		confirmButton.setClickable(UNCLICKABLE);
		resetButton.setClickable(UNCLICKABLE);
	}
	
	private void stopClick(){
		pauseTime = elapsedTime;
		mHandler.removeCallbacks(startTimer);
		mHandler.postDelayed(startTimer, 0);
		confirmButton.setVisibility(View.VISIBLE);
		resetButton.setVisibility(View.VISIBLE);
		confirmButton.setClickable(CLICKABLE);
		resetButton.setClickable(CLICKABLE);
	}
	
	private void confirmClick(){
		//Store Statistics
		started = false;
		setStats();
		mainTimerText.setText("00:00:00:00");
		mainMoneyText.setText("");
		confirmButton.setVisibility(View.INVISIBLE);
		resetButton.setVisibility(View.INVISIBLE);
		confirmButton.setClickable(UNCLICKABLE);
		resetButton.setClickable(UNCLICKABLE);
	}
	
	private void resetClick(){
		//Reset The Timer
		started = false;
		mainTimerText.setText("00:00:00:00");
		mainMoneyText.setText("");
		confirmButton.setVisibility(View.INVISIBLE);
		resetButton.setVisibility(View.INVISIBLE);
		confirmButton.setClickable(UNCLICKABLE);
		resetButton.setClickable(UNCLICKABLE);
	}
	
	private void statisticsClick(){
		makeDialog(STAT_DIALOG);
	}
	
	private Runnable startTimer = new Runnable() {
		public void run(){
			elapsedTime = System.currentTimeMillis() - startTime;
			if(stopped)
				updateTimer(elapsedTime);
			mHandler.postDelayed(this, REFRESH_RATE);
		}
	};
	
	private void updateTimer(float time){
		secs = (long)(time/1000);
		mins = (long)((time/1000)/60);
		hrs = (long)(((time/1000)/60)/60);

		secs = secs % 60;
		seconds=String.valueOf(secs);
    	if(secs == 0){
    		seconds = "00";
    	}
    	if(secs <10 && secs > 0){
    		seconds = "0"+seconds;
    	}
    	mins = mins % 60;
		minutes=String.valueOf(mins);
    	if(mins == 0){
    		minutes = "00";
    	}
    	if(mins <10 && mins > 0){
    		minutes = "0"+minutes;
    	}
    	
    	hours=String.valueOf(hrs);
    	if(hrs == 0){
    		hours = "00";
    	}
    	if(hrs <10 && hrs > 0){
    		hours = "0"+hours;
    	}
    	
    	milliseconds = String.valueOf((long)time);
    	
    	if(milliseconds.length()==3){
    		milliseconds = "0"+milliseconds;
    	}
    	if(milliseconds.length()==2){
    		milliseconds = "00"+milliseconds;
    	}
      	if(milliseconds.length()<=1){
    		milliseconds = "000";
    	}
      	
      	milliseconds = milliseconds.substring(milliseconds.length()-3, milliseconds.length()-1);
    	
		mainTimerText.setText(hours + ":" + minutes + ":" + seconds + ":" + milliseconds);
		
		updateMoney(time);
	}
	private void updateMoney(float time) {
		money = ((((time * rate)/ 1000)/ 60)/ 60);  //Hours to seconds
		if(currencyInt == 0)
			currency = "$";
		if(currencyInt == 1)
			currency = "€";
		if(currencyInt == 2)
			currency = "¥";
		if(currencyInt == 3)
			currency = "£";
		
		if(frequencyInt == 1){	//Monthly
			money = money / (4.34812 * hoursPerWeek);
		}
		else if(frequencyInt == 2){	//Yearly
			money = money / (12 * 4.34812 * hoursPerWeek);
		}
		
		DecimalFormat form = new DecimalFormat("0.00");
		moneyMade = String.valueOf(form.format(money));
		mainMoneyText.setText(currency + moneyMade);
	} 
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {		
		loadPref();
	}
    
	private void loadPref(){
		mySharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		mySharedPref.registerOnSharedPreferenceChangeListener(this);
		String frequencyString = mySharedPref.getString("frequency_preference", "0");
		String earningString = mySharedPref.getString("earning_preference", "0");
		String currencyString = mySharedPref.getString("currency_preference", "0");
		String hoursString = mySharedPref.getString("hours_preference", "40");
		useNotification = mySharedPref.getBoolean("notification_preference", true);
		
		frequencyInt = Integer.valueOf(frequencyString);
		rate = Float.valueOf(earningString);
		currencyInt = Integer.valueOf(currencyString);
		hoursPerWeek = Float.valueOf(hoursString);
		
		if(frequencyInt == 0)
			frequency = "hour";
		if(frequencyInt == 1)
			frequency = "month";
		if(frequencyInt == 2)
			frequency = "year";
		
		if(currencyInt == 0)
			currency = "$";
		if(currencyInt == 1)
			currency = "€";
		if(currencyInt == 2)
			currency = "¥";
		if(currencyInt == 3)
			currency = "£";
		
		getStats();
		
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key){
		if(key == "frequency_preference"){
			frequencyInt = Integer.valueOf(sp.getString(key, "0"));
			if(frequencyInt == 0)
				frequency = "hour";
			if(frequencyInt == 1)
				frequency = "month";
			if(frequencyInt == 2)
				frequency = "year";
		}
		if(key == "earning_preference")
			rate = Integer.valueOf(sp.getString(key, "0"));
		if(key == "currency_preference"){
			currencyInt = Integer.valueOf(sp.getString(key, "0"));
			if(currencyInt == 0)
				currency = "$";
			if(currencyInt == 1)
				currency = "€";
			if(currencyInt == 2)
				currency = "¥";
			if(currencyInt == 3)
				currency = "£";
		}
		if(key == "notification_preference")
			useNotification = sp.getBoolean(key, true);
		
		if(key == "totalSessions")
			totalSessions = sp.getInt(key, 0);
			
		if(key == "totalMoneyMade")
			totalMoneyMade = Double.valueOf(sp.getString(key, "0"));
		
		if(key == "averageMoneyMade")
			averageMoneyMade = Double.valueOf(sp.getString(key, "0"));
		
		if(key == "highestPayout")
			highestPayout = Double.valueOf(sp.getString(key, "0"));
		
		if(key == "averageSessionTime")
			averageSessionTime = sp.getLong(key, 0);
		
		if(key == "totalTimeSpent")
			totalTimeSpent = sp.getLong(key, 0);
		
		if(key == "longestSession")
			longestSession = sp.getLong(key, 0);
		
	}
	
	private void checkForFirstTime(){
		boolean firstTime = mySharedPref.getBoolean("first_time", false);
		if(!firstTime){ //First Time
			
			firstTime = true;
			SharedPreferences.Editor editor = mySharedPref.edit();
			editor.putBoolean("first_time", firstTime);
			editor.commit();
			
			makeDialog(FIRST_DIALOG);
		}
	}
	
	private void makeDialog(int type){
		String message = null, positiveButton = null, title = null, negativeButton = null;
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		switch(type){
		case FIRST_DIALOG:
			message = "Welcome to Crap for Coins!\n\n" +
					"You're going to be redirected to the settings to input your earnings.\n\n" +
					"This dialog won't show up again, but you can change the settings at any time!";
			positiveButton = "Okay";
			title = "First Time";
			alertBuilder.setPositiveButton(positiveButton, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
				}
			});
			break;
			
		case ABOUT_DIALOG:
			message = "I made this app instead of studying for exams.";
			positiveButton = "Well, that was dumb.";
			title = "Crap for Coins";
			alertBuilder.setPositiveButton(positiveButton, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();	
				}	
			});
			break;
			
		case STAT_DIALOG:
			DecimalFormat form;
			if((currencyInt == 0)||(currencyInt == 1)||(currencyInt == 3)){
				form = new DecimalFormat("0.00");
			}
			else{
				form = new DecimalFormat("0");
			}
			String formattedTotalTime = "0s", formattedAverageTime = "0s", formattedLongestSession = "0s";
			if(totalTimeSpent != 0)
				formattedTotalTime = getTimeFormat(totalTimeSpent);
			if(averageSessionTime != 0)
				formattedAverageTime = getTimeFormat(averageSessionTime);
			if(longestSession != 0)	
				formattedLongestSession = getTimeFormat(longestSession);
			message = "Total Money Made: " + currency + String.valueOf(form.format(totalMoneyMade)) +
					"\nTotal Time Spent: " + formattedTotalTime +
					"\nTotal Sessions: " + totalSessions +
					"\n\nLongest Session: " + formattedLongestSession +
					"\nHighest Payout: " + currency + String.valueOf(form.format(highestPayout)) +
					"\n\nAverage Session: " + formattedAverageTime +
					"\n\nAverage Payout: " + currency + String.valueOf(form.format(averageMoneyMade));
			positiveButton = "Okay";
			negativeButton = "Reset Statistics";
			title = "Statistics";
			alertBuilder.setPositiveButton(positiveButton, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();	
				}
			});
			alertBuilder.setNegativeButton(negativeButton, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					resetStats();
				}
			});
			break;	
		}

		alertBuilder.setMessage(message);
		AlertDialog alert = alertBuilder.create();
		alert.setTitle(title);
		alert.show();	
	}
	
	 @TargetApi(Build.VERSION_CODES.JELLY_BEAN) public void createNotification() {
		 PendingIntent pi = getPendingIntent();
		 notification = new Notification.Builder(this)
		    .setContentTitle("Crap for Coins")
		    .setContentText("Your timer is still running!")
		    .setTicker("Your timer is still running!")
		    .setAutoCancel(true)
		    .setContentIntent(pi)
		    .setSmallIcon(R.drawable.ic_launcher).build();
		  NotificationManager notificationManager = getNotificationManager();
		  notificationManager.notify(NOTIFICATION_CODE, notification);
	 }
	 
	 private PendingIntent getPendingIntent() {
		 Intent intent = new Intent(this, MainActivity.class);
		 intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
				 Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(this, 0, intent, 0);
	}

	public NotificationManager getNotificationManager() {
		   return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		  }
	 
	 public void removeNotification(){
		 NotificationManager notificationManager = getNotificationManager();
		 notificationManager.cancel(NOTIFICATION_CODE);
		 
	 }
	 
	 private void setStats(){
		 totalSessions += 1;
		 totalMoneyMade += money;
		 totalTimeSpent += pauseTime;
		 averageMoneyMade = totalMoneyMade / totalSessions;
		 averageSessionTime = totalTimeSpent / totalSessions;
		 if(pauseTime > longestSession)
			 longestSession = pauseTime;
		 if(money > highestPayout)
			 highestPayout = money;
		 
		 SharedPreferences.Editor editor = mySharedPref.edit();
		 editor.putInt("totalSessions", totalSessions);
		 editor.putString("totalMoneyMade", String.valueOf(totalMoneyMade));
		 editor.putString("averageMoneyMade", String.valueOf(averageMoneyMade));
		 editor.putLong("averageSessionTime", averageSessionTime);
		 editor.putLong("totalTimeSpent", totalTimeSpent);
		 editor.putLong("longestSession", longestSession);
		 editor.putString("highestPayout", String.valueOf(highestPayout));
		 editor.commit();
		 
		 makeToast("Results stored");
	 }
	 
	 private void getStats(){
			totalSessions = mySharedPref.getInt("totalSessions", 0);
			totalMoneyMade = Double.valueOf(mySharedPref.getString("totalMoneyMade", "0"));
			averageMoneyMade = Double.valueOf(mySharedPref.getString("averageMoneyMade", "0"));
			averageSessionTime = mySharedPref.getLong("averageSessionTime", 0);
			totalTimeSpent = mySharedPref.getLong("totalTimeSpent", 0);
			longestSession = mySharedPref.getLong("longestSession", 0);
			highestPayout = Double.valueOf(mySharedPref.getString("highestPayout", "0"));
	 }
	 
	 private void resetStats(){
		 SharedPreferences.Editor editor = mySharedPref.edit();
		 editor.putInt("totalSessions", 0);
		 editor.putString("totalMoneyMade", "0");
		 editor.putString("averageMoneyMade", "0");
		 editor.putLong("averageSessionTime", 0);
		 editor.putLong("totalTimeSpent", 0);
		 editor.putLong("longestSession", 0);
		 editor.putString("highestPayout", "0");
		 editor.commit();
		 makeToast("Your statistics have been reset.");
	 }
	 
	 private String getTimeFormat(long time){
		 String formattedTime = "";
		 long s, m, h;
		 String sStr, mStr, hStr, msStr;
		 
		 s = (long)(time/1000);
		 s = s % 60;
		 m = (long)((time/1000)/60);
		 m = m % 60;
		 h = (long)(((time/1000)/60)/60);
		 
		 sStr = Long.toString(s);
		 mStr = Long.toString(m);
		 hStr = Long.toString(h);
		 
		 msStr = String.valueOf(time);
	    	
		 if(msStr.length()==3){
			 msStr = "0"+msStr;
		 }
		 if(msStr.length()==2){
			 msStr = "00"+msStr;
		 }
		 if(milliseconds.length()<=1){
			 msStr = "000";
		 }
	      	
		 msStr = msStr.substring(msStr.length()-3, msStr.length()-1);
		 
		 if(h != 0)
			 formattedTime += hStr + "h ";
		 if(m != 0)
			 formattedTime += mStr + "m ";
		 if(s != 0)
			 formattedTime += sStr + "s ";
		 if(Integer.valueOf(msStr) != 0)
			 formattedTime += msStr + "ms";

		 return formattedTime;
		 
	 }
}
