package com.example.nexthire;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ApplicationListActivity extends AppCompatActivity {

    private ListView listView;
    private TextView tvTitle, tvEmptyState;
    private ImageView btnBack;
    private String status;
    private String currentUserName;
    private ArrayList<Application> applicationList;
    private ApplicationAdapter adapter;
    private DatabaseReference dbRef;
    private ValueEventListener valueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_list);

        status = getIntent().getStringExtra("STATUS");
        if (status == null) status = "Pending";

        currentUserName = getIntent().getStringExtra("USER_NAME");
        if (currentUserName == null || currentUserName.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.getDisplayName() != null) {
                currentUserName = currentUser.getDisplayName();
            } else {
                currentUserName = "User";
            }
        }

        tvTitle = findViewById(R.id.tv_app_list_title);
        listView = findViewById(R.id.app_list_view);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        btnBack = findViewById(R.id.btn_back);

        tvTitle.setText(status + " Applications");

        btnBack.setOnClickListener(v -> finish());

        applicationList = new ArrayList<>();
        adapter = new ApplicationAdapter(this, applicationList);
        listView.setAdapter(adapter);

        loadApplications();
    }

    private void loadApplications() {
        dbRef = FirebaseDatabase.getInstance().getReference("Applications");

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                applicationList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String applicantName = ds.child("applicantName").getValue(String.class);
                    String jobTitle = ds.child("jobTitle").getValue(String.class);
                    String companyName = ds.child("companyName").getValue(String.class);
                    String appStatus = ds.child("status").getValue(String.class);

                    if (applicantName != null && applicantName.equals(currentUserName)) {
                        if (appStatus != null && appStatus.equals(status)) {
                            Application app = new Application(applicantName, jobTitle, companyName, appStatus);
                            applicationList.add(app);
                        }
                    }
                }

                adapter.notifyDataSetChanged();

                if (applicationList.isEmpty()) {
                    tvEmptyState.setVisibility(TextView.VISIBLE);
                    listView.setVisibility(TextView.GONE);
                } else {
                    tvEmptyState.setVisibility(TextView.GONE);
                    listView.setVisibility(TextView.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        dbRef.addValueEventListener(valueEventListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbRef != null && valueEventListener != null) {
            dbRef.removeEventListener(valueEventListener);
        }
    }
}