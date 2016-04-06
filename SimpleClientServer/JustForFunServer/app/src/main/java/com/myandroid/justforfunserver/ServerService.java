package com.myandroid.justforfunserver;

import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.myandroid.justforfunserver.activities.MainActivity;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ServerService extends Service {

    private static final String LOG_TAG = MainActivity.class.getSimpleName() +
            "@" + ServerService.class.getSimpleName();
    private DBHelper dbHelper;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(LOG_TAG, "onStartCommand(Intent " + intent + ", int " + flags + ", int " + startId);

        BlockingQueue queue = new ArrayBlockingQueue(1024);

        //start thread that accepts new connections
        ConnectionsProducer producer = new ConnectionsProducer(getApplicationContext(), queue);
        new Thread(producer).start();

        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ConnectionsProducer.serverSocket != null) {
            try {
                ConnectionsProducer.serverSocket.close();
                Log.v(LOG_TAG, "ServerSocket closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.v(LOG_TAG, "Service Destroyed");
        deleteAllRecordsFromBD();
    }

    /**
     * Delete all records from BD when service is stopped
     */
    private void deleteAllRecordsFromBD() {
        dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DBHelper.TABLE_NAME, null, null);
        Log.v(LOG_TAG, "All records from BD deleted");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
