package com.example.nexthire;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class JobAdapter extends ArrayAdapter<Job> {
    private Context context;
    private List<Job> jobList;
    private String currentUserName;
    private HashMap<String, String> savedJobsMap; // key -> firebase saveId, kept in sync by MainActivity

    public JobAdapter(@NonNull Context context, List<Job> jobList, String userName, HashMap<String, String> savedJobsMap) {
        super(context, R.layout.job_item, jobList);
        this.context = context;
        this.jobList = jobList;
        this.currentUserName = userName;
        this.savedJobsMap = savedJobsMap;
    }

    /**
     * Builds a consistent lookup key so saving, checking, and removing
     * all agree on identity for the same user + job combo.
     */
    public static String buildSaveKey(String username, String jobTitle, String companyName) {
        if (username == null) username = "";
        if (jobTitle == null) jobTitle = "";
        if (companyName == null) companyName = "";
        return username.trim().toLowerCase(Locale.ROOT) + "|" +
                jobTitle.trim().toLowerCase(Locale.ROOT) + "|" +
                companyName.trim().toLowerCase(Locale.ROOT);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.job_item, parent, false);
        }
        Job currentJob = jobList.get(position);

        TextView tvTitle = convertView.findViewById(R.id.tv_job_title);
        TextView tvCompany = convertView.findViewById(R.id.tv_company_name);
        TextView tvSalary = convertView.findViewById(R.id.tv_salary);
        TextView tvDistance = convertView.findViewById(R.id.tv_distance);
        TextView tvHiringTag = convertView.findViewById(R.id.tv_hiring_tag);
        ImageView btnBookmark = convertView.findViewById(R.id.btn_bookmark);
        Button btnView = convertView.findViewById(R.id.btn_view_job);

        tvTitle.setText(currentJob.getJobTitle());
        tvCompany.setText(currentJob.getCompanyName());
        tvSalary.setText(currentJob.getSalary());

        if (currentJob.getDistanceKm() >= 0) {
            tvDistance.setVisibility(View.VISIBLE);
            tvDistance.setText(String.format(Locale.getDefault(), "%.1f km away", currentJob.getDistanceKm()));
        } else {
            tvDistance.setVisibility(View.GONE);
        }

        if (position % 3 == 0) {
            tvHiringTag.setVisibility(View.VISIBLE);
            tvHiringTag.setText("Urgently hiring");
            tvHiringTag.setTextColor(Color.parseColor("#B45309"));
            tvHiringTag.setBackgroundColor(Color.parseColor("#FEF3C7"));
        } else if (position % 3 == 1) {
            tvHiringTag.setVisibility(View.VISIBLE);
            tvHiringTag.setText("Hiring multiple candidates");
            tvHiringTag.setTextColor(Color.parseColor("#0369A1"));
            tvHiringTag.setBackgroundColor(Color.parseColor("#E0F2FE"));
        } else {
            tvHiringTag.setVisibility(View.GONE);
        }

        String saveKey = buildSaveKey(currentUserName, currentJob.getJobTitle(), currentJob.getCompanyName());
        boolean isSaved = savedJobsMap != null && savedJobsMap.containsKey(saveKey);
        btnBookmark.setImageResource(isSaved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_border);

        btnBookmark.setOnClickListener(v -> {
            boolean nowSaved = savedJobsMap != null && savedJobsMap.containsKey(saveKey);
            if (nowSaved) {
                removeSavedJob(saveKey);
            } else {
                saveJob(currentJob, saveKey);
            }
        });

        btnView.setOnClickListener(v -> {
            Intent intent = new Intent(context, JobDetailActivity.class);
            intent.putExtra("SELECTED_JOB", currentJob);
            intent.putExtra("USER_NAME", currentUserName);
            context.startActivity(intent);
        });

        return convertView;
    }

    private void saveJob(Job job, String saveKey) {
        DatabaseReference dbSaved = FirebaseDatabase.getInstance().getReference("SavedJobs");
        String saveId = dbSaved.push().getKey();

        HashMap<String, Object> saveMap = new HashMap<>();
        saveMap.put("saveId", saveId);
        saveMap.put("username", currentUserName);
        saveMap.put("jobTitle", job.getJobTitle());
        saveMap.put("companyName", job.getCompanyName());
        saveMap.put("salary", job.getSalary());
        saveMap.put("description", job.getDescription());
        saveMap.put("scope", job.getScope());
        saveMap.put("requirements", job.getRequirements());
        saveMap.put("phone", job.getPhone());
        saveMap.put("email", job.getEmail());
        saveMap.put("website", job.getWebsite());
        saveMap.put("latitude", job.getLatitude());
        saveMap.put("longitude", job.getLongitude());

        if (saveId != null) {
            dbSaved.child(saveId).setValue(saveMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (savedJobsMap != null) savedJobsMap.put(saveKey, saveId);
                    notifyDataSetChanged();
                    Toast.makeText(context, "Job saved", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void removeSavedJob(String saveKey) {
        if (savedJobsMap == null) return;
        String firebaseId = savedJobsMap.get(saveKey);
        if (firebaseId == null) return;

        FirebaseDatabase.getInstance().getReference("SavedJobs").child(firebaseId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        savedJobsMap.remove(saveKey);
                        notifyDataSetChanged();
                        Toast.makeText(context, "Removed from saved jobs", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}