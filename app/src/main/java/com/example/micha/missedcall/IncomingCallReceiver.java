package com.example.micha.missedcall;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.TelephonyManager;

import java.io.InputStream;
import java.util.Date;


public class IncomingCallReceiver extends BroadcastReceiver {

    static int notificationId = 0;
    static String phoneNumber = "";
    static Uri contactPhoto = null;
    static boolean rang = false;
    static boolean receivedCall = true;
    final static String PRIVATE_NUMBER = "Blocked";


    private boolean isMissedCall(Context context, String ringNumber) {
        StringBuffer sb = new StringBuffer();
        boolean result = false;

        try {
            Thread.sleep(1000); // allow calllog to catch up. There must be a better way.
        } catch (InterruptedException e) {
            // don't care
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {

            Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    new String[] {CallLog.Calls.NUMBER,CallLog.Calls.TYPE,CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.NUMBER_PRESENTATION},
                    null, null, CallLog.Calls.DATE + " DESC");


            int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
            int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
            int presentationIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER_PRESENTATION);
            cursor.moveToFirst();
            String callNumber = cursor.getString(numberIndex);
            int callType = cursor.getInt(typeIndex);
            String callDate = cursor.getString(dateIndex);
            int durationValue = cursor.getInt(durationIndex);
            int presentation = cursor.getInt(presentationIndex);
            Date callDayTime = new Date(Long.valueOf(callDate + 1000*durationValue));
            Date now = new Date();

            boolean typeMatch = (callType == CallLog.Calls.MISSED_TYPE);

            if (presentation == CallLog.Calls.PRESENTATION_RESTRICTED || presentation == CallLog.Calls.PRESENTATION_UNKNOWN) {
                callNumber = PRIVATE_NUMBER;
            }
            boolean dateMatch =  ((now.getTime() - callDayTime.getTime()) < 3000);
            boolean numberMatch = ringNumber.equals(callNumber);

            result = typeMatch && dateMatch && numberMatch;


            cursor.close();
        }

        return result;
    }


    private String getContactDisplayNameByNumber(String number, Context context) {

        String name = "Unknown caller";

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        ContentResolver contentResolver = context.getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, null, null, null, null);

        contactPhoto = null;
        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                String uriAsString = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.PHOTO_URI));
                if (uriAsString != null) {
                    contactPhoto = Uri.parse(uriAsString);
                }
            }
        } catch (Exception e) {
           // don't care e.printStackTrace();
        } finally {
            if (contactLookup != null && !contactLookup.isClosed()) {
                contactLookup.close();
            }
        }

        return name;
    }



    private Bitmap getContactBitmapFromURI(Context context, Uri uri) {
        InputStream iStream;
        Bitmap result = null;
        try {
            iStream = context.getContentResolver().openInputStream(uri);
            result = BitmapFactory.decodeStream(iStream);
            if (iStream != null) {
                iStream.close();
            }
        } catch (Exception e) {
           // don't care e.printStackTrace();
        }

        return result; // BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact);
    }



    private void notify(String phoneNumber, Context mContext) {

        String name = getContactDisplayNameByNumber(phoneNumber, mContext);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext);
        mBuilder.setContentTitle("Oh shit!");
        mBuilder.setContentText(name + " " + phoneNumber);
        mBuilder.setSmallIcon(R.drawable.missed_call);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        if (contactPhoto != null) {
            Bitmap bitmap = getContactBitmapFromURI(mContext, contactPhoto);
            if (bitmap != null) {
                mBuilder.setLargeIcon(bitmap);
            }
        }

        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotificationManager != null) {
            mNotificationManager.notify(notificationId++, mBuilder.build());
        }
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context mContext, Intent intent) {

        // Get the current Phone State
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (state == null) {
            return;
        }

        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            rang = true;
            receivedCall = false;
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                phoneNumber = bundle.getString("incoming_number");
            }
            if (phoneNumber == null) {
                phoneNumber = PRIVATE_NUMBER;
            }
        }


        if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
            receivedCall = true;
        }


        // If phone is Idle (probably just disconnected)
        if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            if (rang && !receivedCall && isMissedCall(mContext, phoneNumber)){
                notify(phoneNumber, mContext);
            }
            rang = false;
            receivedCall = false;
        }
    }
}

