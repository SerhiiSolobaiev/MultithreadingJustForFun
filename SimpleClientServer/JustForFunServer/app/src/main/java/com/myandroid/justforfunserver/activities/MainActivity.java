package com.myandroid.justforfunserver.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.myandroid.justforfunserver.entities.ConnectionItem;
import com.myandroid.justforfunserver.DBHelper;
import com.myandroid.justforfunserver.adapters.ListConnectionsAdapter;
import com.myandroid.justforfunserver.R;
import com.myandroid.justforfunserver.ServerService;
import com.myandroid.justforfunserver.Utility;
import com.zl.reik.dilatingdotsprogressbar.DilatingDotsProgressBar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private TextView textViewInfoIP, textViewWaiting;
    private ToggleButton buttonStartStop;
    private ListView listViewConnections;
    private DilatingDotsProgressBar mDilatingDotsProgressBar;
    private ArrayList<ConnectionItem> listConnections;
    private ListConnectionsAdapter adapter;

    private SharedPreferences sharedPreferences;
    private static final String APP_PREFERENCES = "MainPreferences";
    private static final String APP_PREFERENCES_STATE_START_BUTTON = "buttonStartStop";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        initViews();
        addValuesToListConnectios();
        startStopServiceListener();
    }

    private void addValuesToListConnectios() {
        listConnections = new ArrayList<>();

        SQLiteDatabase db = new DBHelper(this).getWritableDatabase();
        Cursor c = db.query(DBHelper.TABLE_NAME, null, null, null, null, null, null);

        if (c.moveToFirst()) {
            do {
                String id = c.getString(c.getColumnIndex(DBHelper.ID_CONNECTION));
                int currentProgress = c.getInt(c.getColumnIndex(DBHelper.CURRENT_PROGRESS));

                listConnections.add(new ConnectionItem(id, currentProgress));
            } while (c.moveToNext());
        } else {
            Log.d(LOG_TAG, "DB is empty");
        }
        c.close();

        adapter = new ListConnectionsAdapter(this, R.layout.list_view_item, listConnections);
        listViewConnections.setAdapter(adapter);
        isShownWaitingTextView();

        Log.v(LOG_TAG, "setAdapter(adapter) done");
    }

    private void initViews() {
        textViewInfoIP = (TextView) findViewById(R.id.textView_infoIP);
        textViewWaiting = (TextView) findViewById(R.id.textView_waiting);
        buttonStartStop = (ToggleButton) findViewById(R.id.button_start);
        listViewConnections = (ListView) findViewById(R.id.listView_connections);
        mDilatingDotsProgressBar = (DilatingDotsProgressBar) findViewById(R.id.service_running);

        //set initial states
        textViewInfoIP.setText(Utility.getIpAddress());
        buttonStartStop.setChecked(
                sharedPreferences.getBoolean(APP_PREFERENCES_STATE_START_BUTTON, false));
        if (buttonStartStop.isChecked()) {
            mDilatingDotsProgressBar.showNow();
        }
    }

    /**
     * Start or Stop Service
     */
    public void startStopServiceListener() {
        buttonStartStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonStartStop.isChecked()) {
                    startService(new Intent(getBaseContext(), ServerService.class));
                    mDilatingDotsProgressBar.showNow();
                    adapter.clear();
                    isShownWaitingTextView();

                } else {
                    stopService(new Intent(getBaseContext(), ServerService.class));
                    mDilatingDotsProgressBar.hideNow();

                    //not necessary
                    LocalBroadcastManager.getInstance(getApplicationContext())
                            .unregisterReceiver(messageFromService);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(messageFromService, new IntentFilter("ServiceMessage"));
    }

    /**
     * Receive message from service
     */
    private BroadcastReceiver messageFromService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(LOG_TAG, "messageFromService received:\n"
                    + intent.getStringExtra("connection") + " " + intent.getIntExtra("progress", 0));
            String connection = intent.getStringExtra("connection");
            int progress = intent.getIntExtra("progress", 0);

            ConnectionItem item = new ConnectionItem(connection, progress);
            if (listConnections.contains(item)) {
                listConnections.set(Integer.valueOf(connection) - 1,
                        new ConnectionItem(connection, progress));
            } else {
                listConnections.add(new ConnectionItem(connection, progress));
            }
            adapter.notifyDataSetChanged();
            isShownWaitingTextView();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(messageFromService);
    }

    /**
     * TextView "Waiting" is displayed only if there is no connection
     */
    private void isShownWaitingTextView() {
        if (listConnections.isEmpty())
            textViewWaiting.setVisibility(View.VISIBLE);
        else
            textViewWaiting.setVisibility(View.GONE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(APP_PREFERENCES_STATE_START_BUTTON, buttonStartStop.isChecked());
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.clear:
                adapter.clear();
                isShownWaitingTextView();
                DBHelper dbHelper = new DBHelper(this);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete(DBHelper.TABLE_NAME, null, null);
                Log.v(LOG_TAG, "All records from BD deleted");
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
