package com.example.nexthire;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class NotificationAdapter extends BaseAdapter {

    private Context context;
    private List<NotificationItem> notificationList;
    private LayoutInflater inflater;

    public NotificationAdapter(Context context, List<NotificationItem> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return notificationList.size();
    }

    @Override
    public Object getItem(int position) {
        return notificationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.notification_list_item, parent, false);
            holder = new ViewHolder();
            holder.tvTitle = convertView.findViewById(R.id.tv_notif_title);
            holder.tvBody = convertView.findViewById(R.id.tv_notif_body);
            holder.tvTimestamp = convertView.findViewById(R.id.tv_notif_timestamp);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        NotificationItem item = notificationList.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvBody.setText(item.getBody());
        holder.tvTimestamp.setText(item.getTimestamp());

        if ("false".equals(item.getRead())) {
            holder.tvTitle.setAlpha(1.0f);
            holder.tvBody.setAlpha(1.0f);
        } else {
            holder.tvTitle.setAlpha(0.6f);
            holder.tvBody.setAlpha(0.6f);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView tvTitle;
        TextView tvBody;
        TextView tvTimestamp;
    }
}