package com.example.nexthire;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "NEXTHIRE_DEBUG";

    private TextView tvProfileName, tvProfileGender, tvProfileBirthday, tvProfileUniversity, tvProfileEducation, tvProfileQualification, tvProfileExperience, tvProfileHardSkills, tvProfileSoftSkills;
    private TextView tvNoSavedJobs;
    private LinearLayout layoutIncompleteWarning;
    private NonScrollListView savedJobsListView;
    private ArrayList<Job> savedJobsObjectsList;
    private ArrayList<String> savedIdsList;
    private SavedJobsCustomAdapter adapter;

    private String currentUserId;
    private String currentUserEmail = "";
    private String displayRealName = "Loading...";

    private String currentUserGender = "Not Specified";
    private String currentUserBirthday = "Not Specified";
    private String currentUserUniversity = "Not Specified";
    private String currentUserEducation = "Not Specified";
    private String currentUserQualification = "Not Specified";
    private String currentUserExperience = "Not Specified";
    private String currentUserHardSkills = "Not Specified";
    private String currentUserSoftSkills = "Not Specified";

    private ImageView btnBackDashboard;
    private Button btnEditProfile, btnBannerUpdate, btnSignOut;
    private FirebaseAuth mAuth;
    private ImageView ivProfilePicture;
    private String currentUserPhotoUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Log.d(TAG, "[Profile] onCreate started");

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            currentUserEmail = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        } else {
            currentUserId = "default_user";
        }
        Log.d(TAG, "[Profile] currentUserId = " + currentUserId);

        layoutIncompleteWarning = findViewById(R.id.layout_incomplete_warning);
        btnBannerUpdate = findViewById(R.id.btn_banner_update);

        tvProfileName = findViewById(R.id.profile_user_name);
        tvProfileGender = findViewById(R.id.tv_profile_gender);
        tvProfileBirthday = findViewById(R.id.tv_profile_birthday);
        tvProfileUniversity = findViewById(R.id.tv_profile_university);
        tvProfileEducation = findViewById(R.id.tv_profile_education);
        tvProfileQualification = findViewById(R.id.tv_profile_qualification);
        tvProfileExperience = findViewById(R.id.tv_profile_experience);
        tvProfileHardSkills = findViewById(R.id.tv_profile_hard_skills);
        tvProfileSoftSkills = findViewById(R.id.tv_profile_soft_skills);
        tvNoSavedJobs = findViewById(R.id.tv_no_saved_jobs);
        btnSignOut = findViewById(R.id.btn_sign_out);
        ivProfilePicture = findViewById(R.id.iv_profile_picture);

        Log.d(TAG, "[Profile] ivProfilePicture is null? " + (ivProfilePicture == null));

        savedJobsListView = findViewById(R.id.saved_jobs_list_view);
        savedJobsObjectsList = new ArrayList<>();
        savedIdsList = new ArrayList<>();

        adapter = new SavedJobsCustomAdapter();
        savedJobsListView.setAdapter(adapter);

        loadUserProfileData();
        loadSavedJobsData();

        btnBackDashboard = findViewById(R.id.btn_back_to_dashboard);
        if (btnBackDashboard != null) {
            btnBackDashboard.setOnClickListener(v -> finish());
        }

        btnEditProfile = findViewById(R.id.btn_edit_profile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> openEditProfileScreen());
        }

        if (btnBannerUpdate != null) {
            btnBannerUpdate.setOnClickListener(v -> openEditProfileScreen());
        }

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                mAuth.signOut();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                Toast.makeText(ProfileActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "[Profile] onResume - currentUserPhotoUrl = [" + currentUserPhotoUrl + "]");
    }

    private void loadUserProfileData() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "[Profile] loadUserProfileData onDataChange fired. hasChild(currentUserId)=" + snapshot.hasChild(currentUserId));
                boolean userFound = false;

                if (snapshot.hasChild(currentUserId)) {
                    DataSnapshot userSnapshot = snapshot.child(currentUserId);
                    parseUserData(userSnapshot);
                    userFound = true;
                } else {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String dbId = ds.child("id").getValue(String.class);
                        String dbEmail = ds.child("email").getValue(String.class);

                        if ((dbId != null && dbId.equals(currentUserId)) ||
                                (dbEmail != null && !currentUserEmail.isEmpty() && dbEmail.equalsIgnoreCase(currentUserEmail))) {
                            parseUserData(ds);
                            userFound = true;
                            break;
                        }
                    }
                }

                Log.d(TAG, "[Profile] userFound = " + userFound);

                if (!userFound) {
                    if (!currentUserEmail.isEmpty()) {
                        String raw = currentUserEmail.split("@")[0];
                        displayRealName = raw.substring(0, 1).toUpperCase() + raw.substring(1);
                    } else {
                        displayRealName = "New User";
                    }
                    updateProfileUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "[Profile] loadUserProfileData onCancelled: " + error.getMessage());
                Toast.makeText(ProfileActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void parseUserData(DataSnapshot snapshot) {
        String nameFromDb = snapshot.child("name").getValue(String.class);
        if (nameFromDb == null) nameFromDb = snapshot.child("fullName").getValue(String.class);

        displayRealName = (nameFromDb != null && !nameFromDb.isEmpty()) ? nameFromDb : "No Name Set";

        String gender = snapshot.child("gender").getValue(String.class);
        String birthday = snapshot.child("birthday").getValue(String.class);
        String university = snapshot.child("university").getValue(String.class);
        String education = snapshot.child("education").getValue(String.class);
        String qualification = snapshot.child("qualification").getValue(String.class);
        String experience = snapshot.child("experience").getValue(String.class);
        String hardSkills = snapshot.child("hardSkills").getValue(String.class);
        String softSkills = snapshot.child("softSkills").getValue(String.class);
        String photoUrl = snapshot.child("photoUrl").getValue(String.class);

        Log.d(TAG, "[Profile] parseUserData: raw photoUrl from snapshot = [" + photoUrl + "]");

        currentUserPhotoUrl = (photoUrl != null) ? photoUrl : "";

        Log.d(TAG, "[Profile] parseUserData: currentUserPhotoUrl set to = [" + currentUserPhotoUrl + "]");

        currentUserGender = (gender != null && !gender.isEmpty()) ? gender : "Not Specified";
        currentUserBirthday = (birthday != null && !birthday.isEmpty()) ? birthday : "Not Specified";
        currentUserUniversity = (university != null && !university.isEmpty()) ? university : "Not Specified";
        currentUserEducation = (education != null && !education.isEmpty()) ? education : "Not Specified";
        currentUserQualification = (qualification != null && !qualification.isEmpty()) ? qualification : "Not Specified";
        currentUserExperience = (experience != null && !experience.isEmpty()) ? experience : "Not Specified";
        currentUserHardSkills = (hardSkills != null && !hardSkills.isEmpty()) ? hardSkills : "Not Specified";
        currentUserSoftSkills = (softSkills != null && !softSkills.isEmpty()) ? softSkills : "Not Specified";

        updateProfileUI();
    }

    private void loadSavedJobsData() {
        DatabaseReference dbSaved = FirebaseDatabase.getInstance().getReference("SavedJobs");
        dbSaved.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                savedJobsObjectsList.clear();
                savedIdsList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String user = ds.child("username").getValue(String.class);

                    if (currentUserId.equalsIgnoreCase(user) || displayRealName.equalsIgnoreCase(user)) {
                        String firebaseSaveId = ds.getKey();

                        String jobTitle = ds.child("jobTitle").getValue(String.class);
                        String company = ds.child("companyName").getValue(String.class);
                        String salary = ds.child("salary").getValue(String.class);
                        String description = ds.child("description").getValue(String.class);
                        String scope = ds.child("scope").getValue(String.class);
                        String requirements = ds.child("requirements").getValue(String.class);
                        String phone = ds.child("phone").getValue(String.class);
                        String email = ds.child("email").getValue(String.class);
                        String website = ds.child("website").getValue(String.class);

                        Double lat = ds.child("latitude").getValue(Double.class);
                        Double lng = ds.child("longitude").getValue(Double.class);
                        double latitude = (lat != null) ? lat : 0.0;
                        double longitude = (lng != null) ? lng : 0.0;

                        Job job = new Job(
                                jobTitle, company, salary, "Saved",
                                description, scope, requirements,
                                phone, email, website, latitude, longitude
                        );

                        savedJobsObjectsList.add(job);
                        savedIdsList.add(firebaseSaveId);
                    }
                }

                adapter.notifyDataSetChanged();

                if (savedJobsObjectsList.isEmpty()) {
                    if (tvNoSavedJobs != null) tvNoSavedJobs.setVisibility(View.VISIBLE);
                    if (savedJobsListView != null) savedJobsListView.setVisibility(View.GONE);
                } else {
                    if (tvNoSavedJobs != null) tvNoSavedJobs.setVisibility(View.GONE);
                    if (savedJobsListView != null) savedJobsListView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void openEditProfileScreen() {
        Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
        intent.putExtra("USER_NAME", displayRealName);
        intent.putExtra("USER_GENDER", currentUserGender);
        intent.putExtra("USER_BIRTHDAY", currentUserBirthday);
        intent.putExtra("USER_UNIVERSITY", currentUserUniversity);
        intent.putExtra("USER_EDUCATION", currentUserEducation);
        intent.putExtra("USER_QUALIFICATION", currentUserQualification);
        intent.putExtra("USER_EXPERIENCE", currentUserExperience);
        intent.putExtra("USER_HARD_SKILLS", currentUserHardSkills);
        intent.putExtra("USER_SOFT_SKILLS", currentUserSoftSkills);
        startActivity(intent);
    }

    private void updateProfileUI() {
        if (tvProfileName != null) tvProfileName.setText(displayRealName);
        if (tvProfileGender != null) tvProfileGender.setText(currentUserGender);
        if (tvProfileBirthday != null) tvProfileBirthday.setText(currentUserBirthday);
        if (tvProfileUniversity != null) tvProfileUniversity.setText(currentUserUniversity);
        if (tvProfileEducation != null) tvProfileEducation.setText(currentUserEducation);
        if (tvProfileQualification != null) tvProfileQualification.setText(currentUserQualification);
        if (tvProfileExperience != null) tvProfileExperience.setText(currentUserExperience);
        if (tvProfileHardSkills != null) tvProfileHardSkills.setText(currentUserHardSkills);
        if (tvProfileSoftSkills != null) tvProfileSoftSkills.setText(currentUserSoftSkills);

        Log.d(TAG, "[Profile] updateProfileUI called. ivProfilePicture null? " + (ivProfilePicture == null) + ", currentUserPhotoUrl empty? " + currentUserPhotoUrl.isEmpty());

        if (ivProfilePicture != null && !currentUserPhotoUrl.isEmpty()) {
            Log.d(TAG, "[Profile] Attempting Glide.load() with url = " + currentUserPhotoUrl);
            Glide.with(this)
                    .load(currentUserPhotoUrl)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            Log.d(TAG, "[Profile] Glide onLoadFailed for url=" + model + " exception=" + e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d(TAG, "[Profile] Glide onResourceReady for url=" + model);
                            return false;
                        }
                    })
                    .into(ivProfilePicture);
        } else {
            Log.d(TAG, "[Profile] Skipping Glide load - ivProfilePicture null or currentUserPhotoUrl empty");
        }

        if (currentUserGender.equals("Not Specified") || currentUserBirthday.equals("Not Specified") ||
                currentUserUniversity.equals("Not Specified") || currentUserEducation.equals("Not Specified") ||
                currentUserQualification.equals("Not Specified") || currentUserExperience.equals("Not Specified") ||
                currentUserHardSkills.equals("Not Specified") || currentUserSoftSkills.equals("Not Specified")) {

            if (layoutIncompleteWarning != null) layoutIncompleteWarning.setVisibility(View.VISIBLE);
        } else {
            if (layoutIncompleteWarning != null) layoutIncompleteWarning.setVisibility(View.GONE);
        }
    }

    private class SavedJobsCustomAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return savedJobsObjectsList.size();
        }

        @Override
        public Object getItem(int position) {
            return savedJobsObjectsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ProfileActivity.this).inflate(R.layout.saved_job_item, parent, false);
            }

            TextView tvTitle = convertView.findViewById(R.id.tv_saved_job_title);
            TextView tvCompany = convertView.findViewById(R.id.tv_saved_job_company);

            Job currentJob = savedJobsObjectsList.get(position);
            tvTitle.setText(currentJob.getJobTitle());
            tvCompany.setText(currentJob.getCompanyName());

            convertView.setOnClickListener(v -> {
                String[] options = {"View Details", "Remove Saved Job"};

                new AlertDialog.Builder(ProfileActivity.this)
                        .setTitle("Manage Saved Job")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                Intent intent = new Intent(ProfileActivity.this, JobDetailActivity.class);
                                intent.putExtra("SELECTED_JOB", currentJob);
                                intent.putExtra("USER_NAME", displayRealName);
                                startActivity(intent);
                            } else if (which == 1) {
                                String idToRemove = savedIdsList.get(position);
                                if (idToRemove != null) {
                                    FirebaseDatabase.getInstance().getReference("SavedJobs").child(idToRemove).removeValue()
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(ProfileActivity.this, "Job removed from saved list.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }
                        })
                        .show();
            });

            return convertView;
        }
    }
}