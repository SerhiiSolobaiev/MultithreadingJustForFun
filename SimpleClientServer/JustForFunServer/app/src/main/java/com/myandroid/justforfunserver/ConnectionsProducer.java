package com.myandroid.justforfunserver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.myandroid.justforfunserver.activities.MainActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Producer waits for new connection and add it to blocking queue
 */
public class ConnectionsProducer implements Runnable {

    private static final String LOG_TAG = MainActivity.class.getSimpleName() +
            "@" + ServerService.class.getSimpleName();

    private Context context;

    private BlockingQueue queue = null;
    public static ServerSocket serverSocket;
    public static Executor executor;

    //number of the connection
    private int count = 0;
    //message to notification
    private String message;


    ConnectionsProducer(Context context, BlockingQueue queue) {
        this.context = context;
        this.queue = queue;
    }

    @Override
    public void run() {

        //number of threads that work simultaneously
        int numberThreads = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(
                context).getString("numberThreads", "1"));
        executor = Executors.newFixedThreadPool(numberThreads);

        try {
            final int SocketServerPORT = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(
                    context).getString("port", "8080"));

            serverSocket = new ServerSocket(SocketServerPORT);
            Log.v(LOG_TAG, "port = " + serverSocket.getLocalPort());

            while (true) {
                Log.v(LOG_TAG, "Waiting for connection...");
                Socket socket = serverSocket.accept();
                count++;
                message += "#" + count + " from " + socket.getInetAddress()
                        + ":" + socket.getPort() + "\n";

                Log.v(LOG_TAG, "Accepted message from client = " + message);

                SocketServerReplyThread socketServerReplyThread =
                        new SocketServerReplyThread(socket, count);
                socketServerReplyThread.run();

                writeConnectionToBD(String.valueOf(count));

                createNotificationAboutNewConnection(socket.getInetAddress().toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write connection to DB for working when activity is destroyed
     *
     * @param id ID connection(in my case = number)
     */
    private void writeConnectionToBD(String id) {
        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.ID_CONNECTION, id);
        cv.put(DBHelper.CURRENT_PROGRESS, 0);
        long result = db.insertWithOnConflict(DBHelper.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        Log.v(LOG_TAG, "db.insert(DBHelper.TABLE_NAME, null, cv) = " + id);

        //if insertion is successful put it to queue
        if (result != -1) {
            putConnectionToQueue(id);
        }
    }

    /**
     * Put connection to queue and start ConnectionConsumer (thread that updating progress)
     *
     * @param id ID connection
     */
    private void putConnectionToQueue(String id) {
        try {
            queue.put(id);
            executor.execute(new ConnectionsConsumer(context, queue));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create notification about new incoming connection
     *
     * @param clientIP IP of the incoming connection
     */
    private void createNotificationAboutNewConnection(String clientIP) {
        NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationID = 0;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);

        mBuilder.setSmallIcon(R.drawable.ic_server_network);
        mBuilder.setLargeIcon(Utility.drawableToBitmap(
                ContextCompat.getDrawable(context, R.drawable.ic_server_network)));
        mBuilder.setAutoCancel(true);
        mBuilder.setContentTitle(context.getResources().getString(R.string.notification_title));
        mBuilder.setContentText(
                context.getResources().getString(R.string.notification_text) + clientIP);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        mBuilder.setContentIntent(pendingIntent);

        mNotificationManager.notify(notificationID, mBuilder.build());
        Log.v(LOG_TAG, "Notification created");
    }

    /**
     * Response for client about successful connection
     */
    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket;
        int count;

        SocketServerReplyThread(Socket socket, int count) {
            this.hostThreadSocket = socket;
            this.count = count;
        }

        @Override
        public void run() {
            OutputStream outputStream;
            String textViewMessageReply = "Hello from Server-Service=), you are #" + count;

            try {
                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(textViewMessageReply);
                printStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
