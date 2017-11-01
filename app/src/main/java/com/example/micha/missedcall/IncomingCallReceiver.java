package com.example.micha.missedcall;


import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.NotificationCompat;
import android.telephony.TelephonyManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class IncomingCallReceiver extends BroadcastReceiver {

    static int notificationId = 0;
    static String phoneNumber = "";
    static Uri contactPhoto = null;
    static boolean rang = false;
    static boolean receivedCall = true;


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
                contactPhoto = Uri.parse(contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.PHOTO_URI)));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }



    private Bitmap getContactBitmapFromURI(Context context, Uri uri) {
        InputStream iStream = null;
        Bitmap result = null;
        try {
            Uri displayPhotoUri = uri;
            iStream = context.getContentResolver().openInputStream(displayPhotoUri);
            result = BitmapFactory.decodeStream(iStream);
            if (iStream != null) {
                iStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            mBuilder.setLargeIcon(bitmap);
        }


        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(notificationId++, mBuilder.build());
    }

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
            phoneNumber = bundle.getString("incoming_number");
            if (phoneNumber == null) {
                phoneNumber = "Blocked";
            }
        }


        if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
            receivedCall = true;
        }


        // If phone is Idle (probably just disconnected)
        if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            if (rang && !receivedCall){
                notify(phoneNumber, mContext);
            }
            rang = false;
            receivedCall = false;
        }
    }
}

