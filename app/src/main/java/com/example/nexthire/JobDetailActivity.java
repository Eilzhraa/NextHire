package com.example.nexthire;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;

public class JobDetailActivity extends AppCompatActivity {
    private TextView tvTitleTop, tvCompanyTop, tvSalary, tvDesc, tvScope, tvReq, btnSaveJob;
    private ImageButton btnWA, btnEmail, btnWeb;
    private Button btnMap, btnApply;
    private ImageView btnBack;
    private Job selectedJob;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        selectedJob = (Job) getIntent().getSerializableExtra("SELECTED_JOB");
        currentUserName = getIntent().getStringExtra("USER_NAME");

        tvTitleTop = findViewById(R.id.detail_job_title_top);
        tvCompanyTop = findViewById(R.id.detail_company_name_top);
        tvSalary = findViewById(R.id.detail_salary);
        tvDesc = findViewById(R.id.detail_job_description);
        tvScope = findViewById(R.id.detail_job_scope);
        tvReq = findViewById(R.id.detail_job_requirements);
        btnSaveJob = findViewById(R.id.btn_save_job);
        btnBack = findViewById(R.id.btn_back);
        btnWA = findViewById(R.id.btn_whatsapp);
        btnEmail = findViewById(R.id.btn_email);
        btnWeb = findViewById(R.id.btn_website);
        btnMap = findViewById(R.id.btn_view_location);
        btnApply = findViewById(R.id.btn_apply);

        if (selectedJob != null) {
            tvTitleTop.setText(selectedJob.getJobTitle());
            tvCompanyTop.setText(selectedJob.getCompanyName() + " • Kuala Lumpur");
            tvSalary.setText(selectedJob.getSalary());
            tvDesc.setText(selectedJob.getDescription());
            tvScope.setText(selectedJob.getScope());
            tvReq.setText(selectedJob.getRequirements());
        }

        btnBack.setOnClickListener(v -> finish());

        btnSaveJob.setOnClickListener(v -> {
            if (selectedJob == null) return;
            android.widget.PopupMenu popup = new android.widget.PopupMenu(JobDetailActivity.this, btnSaveJob);
            popup.getMenu().add("Save Job");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Save Job")) {
                    checkIfAlreadySavedThenSave();
                    return true;
                }
                return false;
            });
            popup.show();
        });

        btnWA.setOnClickListener(v -> {
            String phoneNumber = selectedJob.getPhone();
            String jobTitle = selectedJob.getJobTitle();
            String companyName = selectedJob.getCompanyName();
            String message = "Hi, I am " + currentUserName + ".\n\n" +
                    "I am writing to express my interest in the " + jobTitle +
                    " position at " + companyName + ".\n\n" +
                    "I have attached my resume for your consideration.\n\n" +
                    "I would appreciate the opportunity to discuss my application further.\n\n" +
                    "Thank you.\n\n" +
                    "Best regards,\n" +
                    currentUserName;
            String url = "https://wa.me/" + phoneNumber + "?text=" + Uri.encode(message);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        btnEmail.setOnClickListener(v -> {
            String subject = "Job Application Inquiry - " + selectedJob.getJobTitle();
            String body = "Dear Hiring Manager,\n\n" +
                    "I am writing to express my interest in the " + selectedJob.getJobTitle() +
                    " position at " + selectedJob.getCompanyName() + ".\n\n" +
                    "I have attached my resume for your consideration.\n\n" +
                    "I look forward to hearing from you.\n\n" +
                    "Best regards,\n" +
                    currentUserName;

            String uriString = "mailto:" + Uri.encode(selectedJob.getEmail())
                    + "?subject=" + Uri.encode(subject)
                    + "&body=" + Uri.encode(body);
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uriString));
            startActivity(Intent.createChooser(intent, "Send Email"));
        });

        btnWeb.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(selectedJob.getWebsite()));
            startActivity(intent);
        });

        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(JobDetailActivity.this, MapsActivity.class);
            intent.putExtra("LATITUDE", selectedJob.getLatitude());
            intent.putExtra("LONGITUDE", selectedJob.getLongitude());
            intent.putExtra("COMPANY_NAME", selectedJob.getCompanyName());
            startActivity(intent);
        });

        btnApply.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Are you sure to apply now?");
            builder.setMessage("Please crosscheck your resume with the job requirements.");
            builder.setPositiveButton("Apply Now", (dialog, which) -> {
                DatabaseReference db = FirebaseDatabase.getInstance().getReference("Applications");
                String appId = db.push().getKey();
                HashMap<String, String> appMap = new HashMap<>();
                appMap.put("applicationId", appId);
                appMap.put("applicantName", currentUserName);
                appMap.put("jobTitle", selectedJob.getJobTitle());
                appMap.put("companyName", selectedJob.getCompanyName());
                appMap.put("status", "Pending");
                if (appId != null) {
                    db.child(appId).setValue(appMap).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "✅ Application sent for " + selectedJob.getJobTitle() + " at " + selectedJob.getCompanyName() + "!", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();
        });
    }

    private void checkIfAlreadySavedThenSave() {
        DatabaseReference dbSaved = FirebaseDatabase.getInstance().getReference("SavedJobs");

        dbSaved.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean alreadySaved = false;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String savedUser = ds.child("username").getValue(String.class);
                    String savedTitle = ds.child("jobTitle").getValue(String.class);
                    String savedCompany = ds.child("companyName").getValue(String.class);

                    if (currentUserName != null && currentUserName.equalsIgnoreCase(savedUser)
                            && selectedJob.getJobTitle().equalsIgnoreCase(savedTitle)
                            && selectedJob.getCompanyName().equalsIgnoreCase(savedCompany)) {
                        alreadySaved = true;
                        break;
                    }
                }

                if (alreadySaved) {
                    Toast.makeText(JobDetailActivity.this, "You've already saved this job.", Toast.LENGTH_SHORT).show();
                } else {
                    saveJobToFirebase(dbSaved);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(JobDetailActivity.this, "Error checking saved jobs: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveJobToFirebase(DatabaseReference dbSaved) {
        String saveId = dbSaved.push().getKey();
        HashMap<String, Object> saveMap = new HashMap<>();
        saveMap.put("saveId", saveId);
        saveMap.put("username", currentUserName);
        saveMap.put("jobTitle", selectedJob.getJobTitle());
        saveMap.put("companyName", selectedJob.getCompanyName());
        saveMap.put("salary", selectedJob.getSalary());
        saveMap.put("description", selectedJob.getDescription());
        saveMap.put("scope", selectedJob.getScope());
        saveMap.put("requirements", selectedJob.getRequirements());
        saveMap.put("phone", selectedJob.getPhone());
        saveMap.put("email", selectedJob.getEmail());
        saveMap.put("website", selectedJob.getWebsite());
        saveMap.put("latitude", selectedJob.getLatitude());
        saveMap.put("longitude", selectedJob.getLongitude());

        if (saveId != null) {
            dbSaved.child(saveId).setValue(saveMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(JobDetailActivity.this, "Job saved successfully.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}