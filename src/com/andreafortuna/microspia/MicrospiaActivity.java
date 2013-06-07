package com.andreafortuna.microspia;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MicrospiaActivity extends Activity {
	
		private static int batteryLevel;	
	
		public static int getBatteryLevel() {
			return batteryLevel;
		}
		
	  private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
		    @Override
		    public void onReceive(Context arg0, Intent intent) {
		      // TODO Auto-generated method stub
		      int level = intent.getIntExtra("level", 0);
		      batteryLevel = level;
		    }
		  };
	
		  
	//Abilita modalitˆ nascosta
		  public void button_stealth_clicked (View v) {
			  final Context  currentContext = v.getContext();
			  
			 //chiedo conferma
			  AlertDialog.Builder builder = new AlertDialog.Builder(this);
			    builder.setMessage(getString(R.string.stealth_confirm))
			           .setCancelable(false)
			           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			               public void onClick(DialogInterface dialog, int id) {
			                    // Abilito modalitˆ nascosta
			            	   (Toast.makeText(currentContext, getString(R.string.hiding_toast), Toast.LENGTH_LONG)).show();
			            	   PackageManager p = getPackageManager();
			            	   p.setComponentEnabledSetting(getComponentName(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
			            	   
			            	   AlertDialog alertDialog = new AlertDialog.Builder(currentContext).create();  
			                   alertDialog.setTitle(getString(R.string.application_hidden));  
			                   alertDialog.setMessage(getString(R.string.restart_needed));  
			                   alertDialog.setButton(getString(R.string.back_home), new DialogInterface.OnClickListener() {
			                   	public void onClick(DialogInterface dialog, int item) {
			                   		finish();
			                       }
			                   });
			                   alertDialog.show();
			                   
			            	   
			               }
			           })
			           .setNegativeButton("No", new DialogInterface.OnClickListener() {
			               public void onClick(DialogInterface dialog, int id) {
			                    dialog.cancel();
			               }
			           });
			    AlertDialog alert = builder.create();
			    alert.show();
			  
			  
		  } 
		  
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
        //TEST
        Uri uri = Uri.parse("smsto:1234567890");   
        Intent it = new Intent(Intent.ACTION_SENDTO, uri);   
        it.putExtra("sms_body", "TESTO SMS");   
        startActivity(it); 
        
        //Verifico abilitazione GPS
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (! locationManager.isProviderEnabled(locationManager.GPS_PROVIDER)) {
        	//Notifico mancanza GPS
        	AlertDialog alertDialog = new AlertDialog.Builder(this).create();  
            alertDialog.setTitle(getString(R.string.gps_dialog_title));  
            alertDialog.setMessage(getString(R.string.gps_dialog_text));  
            alertDialog.setButton(getString(R.string.impostazioni_button), new DialogInterface.OnClickListener() {
            	public void onClick(DialogInterface dialog, int item) {
            		startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
                }
            });
            alertDialog.show();
        }
        
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }
}