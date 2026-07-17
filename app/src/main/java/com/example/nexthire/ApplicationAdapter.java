package com.example.nexthire;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class ApplicationAdapter extends BaseAdapter {

    private Context context;
    private List<Application> applicationList;
    private LayoutInflater inflater;

    public ApplicationAdapter(Context context, List<Application> applicationList) {
        this.context = context;
        this.applicationList = applicationList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return applicationList.size();
    }

    @Override
    public Object getItem(int position) {
        return applicationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.application_list_item, parent, false);
            holder = new ViewHolder();
            holder.tvJobTitle = convertView.findViewById(R.id.tv_app_job_title);
            holder.tvCompany = convertView.findViewById(R.id.tv_app_company);
            holder.tvStatus = convertView.findViewById(R.id.tv_app_status);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Application app = applicationList.get(position);

        holder.tvJobTitle.setText(app.getJobTitle());
        holder.tvCompany.setText(app.getCompanyName());

        String status = app.getStatus();
        holder.tvStatus.setText(status);

        if (status.equals("Approved")) {
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else if (status.equals("Rejected")) {
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        } else {
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
        }

        return convertView;
    }

    static class ViewHolder {
        TextView tvJobTitle;
        TextView tvCompany;
        TextView tvStatus;
    }
}