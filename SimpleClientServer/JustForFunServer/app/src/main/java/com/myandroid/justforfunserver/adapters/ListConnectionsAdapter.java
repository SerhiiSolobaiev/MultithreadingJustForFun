package com.myandroid.justforfunserver.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.myandroid.justforfunserver.entities.ConnectionItem;
import com.myandroid.justforfunserver.R;
import com.myandroid.justforfunserver.views.TextProgressBar;

import java.util.ArrayList;

public class ListConnectionsAdapter extends ArrayAdapter<ConnectionItem> {

    private Context context;
    private int layoutResourceId;
    private ArrayList<ConnectionItem> data;

    private static final int COUNT_TYPE_ITEM = 2;
    private static final int TYPE_ITEM_IN_PROGRESS = 0;
    private static final int TYPE_ITEM_DONE = 1;

    public ListConnectionsAdapter(Context context, int resource, ArrayList<ConnectionItem> objects) {
        super(context, resource, objects);
        this.layoutResourceId = resource;
        this.context = context;
        this.data = objects;
    }

    @Override
    public int getViewTypeCount() {
        return COUNT_TYPE_ITEM;
    }

    @Override
    public int getItemViewType(int position) {
        //if progress done:
        return (data.get(position).getProgress() == 10)
                ? TYPE_ITEM_DONE : TYPE_ITEM_IN_PROGRESS;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        int type = getItemViewType(position);
        ViewHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.textProgressBar = (TextProgressBar) row.findViewById(R.id.textProgressBar);

            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        ConnectionItem item = data.get(position);

        holder.textProgressBar.setText(item.getId());
        holder.textProgressBar.setProgress(item.getProgress());
        if (type == 1) {
            holder.textProgressBar.setText(item.getId() + " " + getContext().getResources().getString(R.string.done));
        }
        return row;
    }

    static class ViewHolder {
        TextProgressBar textProgressBar;
    }
}
