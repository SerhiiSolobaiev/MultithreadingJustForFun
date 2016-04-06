package com.myandroid.justforfunserver;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.myandroid.justforfunserver.activities.MainActivity;

import java.util.concurrent.BlockingQueue;

public class ConnectionsConsumer implements Runnable {

    private static final String LOG_TAG = MainActivity.class.getSimpleName() +
            "@" + ServerService.class.getSimpleName();

    private BlockingQueue queue = null;
    private Context context;

    public ConnectionsConsumer(Context context, BlockingQueue queue) {
        this.context = context;
        this.queue = queue;
    }

    @Override
    public void run() {
        String number = null;

        try {
            //take element from queue
            number = (String) queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        for (int i = 1; i <= 10; i++) {
            try {
                //some simulation of the work
                int time = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(
                        context).getString("timeThread", "1000"));
                Thread.sleep(time);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //update current progress
            cv.put(DBHelper.CURRENT_PROGRESS, i);
            db.updateWithOnConflict(DBHelper.TABLE_NAME, cv,
                    DBHelper.ID_CONNECTION + " = " + number, null, SQLiteDatabase.CONFLICT_REPLACE);
            Log.v(LOG_TAG, "number = " + number + " CURRENT_PROGRESS = " + i);
            sendMessageToActivity(String.valueOf(number), i);
        }
    }

    /**
     * Send message to MainActivity about updating progress
     *
     * @param connection Connection which updating
     * @param progress   Current progress
     */
    private void sendMessageToActivity(String connection, int progress) {
        Intent intent = new Intent("ServiceMessage");
        intent.putExtra("connection", connection);
        intent.putExtra("progress", progress);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        Log.v(LOG_TAG, "Message @@@ connection = " + connection + "" +
                ", progress = " + progress + " @@@ to activity sent");
    }
}