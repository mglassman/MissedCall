package com.example.micha.missedcall;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;

/**
 * Created by micha on 10/28/2017.
 */

public class IncomingCallReceiver extends BroadcastReceiver {

    static boolean ring = false;
    static boolean callReceived = false;
    static String callerPhoneNumber = "Unknown";
    static int notificationId = 0;

    public String getContactDisplayNameByNumber(String number, Context context) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = "Incoming call from";

        ContentResolver contentResolver = context.getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, null, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                // this.id =
                // contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                // String contactId =
                // contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            } else {
                name = "Unknown number";
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }

    private void notify(String phoneNumber, Context mContext) {

        String name = getContactDisplayNameByNumber(phoneNumber, mContext);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext);
        mBuilder.setContentTitle("Oh shit!");
        mBuilder.setContentText(name + " " + phoneNumber);
        mBuilder.setSmallIcon(R.drawable.missed_call);
        mBuilder.setPriority(Notification.PRIORITY_HIGH);
        mBuilder.setColor(Color.BLACK);

        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(notificationId++, mBuilder.build());
    }

    @Override
    public void onReceive(Context mContext, Intent intent) {


        // Get the current Phone State
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (state == null)
            return;

        // If phone state "Ringing"
        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            ring = true;
            callReceived = false;
            // Get the Caller's Phone Number
            Bundle bundle = intent.getExtras();
            callerPhoneNumber = bundle.getString("incoming_number");
        }

        // If incoming call is received
        if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            callReceived = true;
        }

        // If phone is Idle
        if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            // If phone was ringing(ring=true) and not received(callReceived=false) , then it is a missed call
            if (ring && !callReceived) {
                notify(callerPhoneNumber, mContext);
            }
            ring = false;
            callReceived = false;
        }

    }
}

