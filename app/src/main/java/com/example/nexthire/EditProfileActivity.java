package com.example.nexthire;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class EditProfileActivity extends AppCompatActivity {
    private static final String TAG = "NEXTHIRE_DEBUG";
    private Spinner spinnerGender, spinnerEducation;
    private EditText etBirthday, etUniversity, etQualification, etExperience, etHardSkills, etSoftSkills;
    private TextView btnSave, btnCancel;
    private ImageView ivProfilePicture;
    private TextView tvChangePhoto;
    private String gender, birthday, university, education, qualification, experience, hardSkills, softSkills;
    private DatabaseReference dbUsersRef;
    private String currentUserId = "";
    private Uri selectedImageUri = null;
    private Uri cameraImageUri = null;
    private String[] genderOptions = {"Male", "Female"};
    private String[] educationOptions = {"SPM", "STPM", "Diploma", "Bachelor's Degree", "Master's Degree", "PhD"};
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        Log.d(TAG, "onCreate started");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }
        Log.d(TAG, "currentUserId = " + currentUserId);

        if (!currentUserId.isEmpty()) {
            dbUsersRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
        } else {
            Log.d(TAG, "No user ID - finishing activity");
            Toast.makeText(this, "User session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        spinnerGender = findViewById(R.id.spinner_gender);
        spinnerEducation = findViewById(R.id.spinner_education);
        etBirthday = findViewById(R.id.et_edit_birthday);
        etUniversity = findViewById(R.id.et_edit_university);
        etQualification = findViewById(R.id.et_edit_qualification);
        etExperience = findViewById(R.id.et_edit_experience);
        etHardSkills = findViewById(R.id.et_edit_hard_skills);
        etSoftSkills = findViewById(R.id.et_edit_soft_skills);
        ivProfilePicture = findViewById(R.id.iv_profile_picture);
        tvChangePhoto = findViewById(R.id.tv_change_photo);
        btnSave = findViewById(R.id.btn_save_profile);
        btnCancel = findViewById(R.id.btn_cancel_edit);

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genderOptions);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        ArrayAdapter<String> educationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, educationOptions);
        educationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEducation.setAdapter(educationAdapter);

        gender = getIntent().getStringExtra("USER_GENDER");
        birthday = getIntent().getStringExtra("USER_BIRTHDAY");
        university = getIntent().getStringExtra("USER_UNIVERSITY");
        education = getIntent().getStringExtra("USER_EDUCATION");
        qualification = getIntent().getStringExtra("USER_QUALIFICATION");
        experience = getIntent().getStringExtra("USER_EXPERIENCE");
        hardSkills = getIntent().getStringExtra("USER_HARD_SKILLS");
        softSkills = getIntent().getStringExtra("USER_SOFT_SKILLS");

        if (gender != null && !gender.equals("Not Specified")) {
            int position = getIndex(genderOptions, gender);
            if (position != -1) spinnerGender.setSelection(position);
        }
        if (education != null && !education.equals("Not Specified")) {
            int position = getIndex(educationOptions, education);
            if (position != -1) spinnerEducation.setSelection(position);
        }
        if (birthday != null && !birthday.equals("Not Specified")) etBirthday.setText(birthday);
        if (university != null && !university.equals("Not Specified")) etUniversity.setText(university);
        if (qualification != null && !qualification.equals("Not Specified")) etQualification.setText(qualification);
        if (experience != null && !experience.equals("Not Specified")) etExperience.setText(experience);
        if (hardSkills != null && !hardSkills.equals("Not Specified")) etHardSkills.setText(hardSkills);
        if (softSkills != null && !softSkills.equals("Not Specified")) etSoftSkills.setText(softSkills);

        etBirthday.setOnClickListener(v -> showDatePickerDialog());

        loadExistingProfilePhoto();
        setupImagePickers();
        tvChangePhoto.setOnClickListener(v -> showImageSourceDialog());
        ivProfilePicture.setOnClickListener(v -> showImageSourceDialog());

        btnSave.setOnClickListener(v -> {
            Log.d(TAG, "===== SAVE BUTTON CLICKED =====");
            String newGender = spinnerGender.getSelectedItem().toString();
            String newBirthday = etBirthday.getText().toString().trim();
            String newUniversity = etUniversity.getText().toString().trim();
            String newEducation = spinnerEducation.getSelectedItem().toString();
            String newQualification = etQualification.getText().toString().trim();
            String newExperience = etExperience.getText().toString().trim();
            String newHardSkills = etHardSkills.getText().toString().trim();
            String newSoftSkills = etSoftSkills.getText().toString().trim();

            Log.d(TAG, "gender=[" + newGender + "] birthday=[" + newBirthday + "] university=[" + newUniversity + "]");
            Log.d(TAG, "education=[" + newEducation + "] qualification=[" + newQualification + "] experience=[" + newExperience + "]");
            Log.d(TAG, "hardSkills=[" + newHardSkills + "] softSkills=[" + newSoftSkills + "]");

            if (newBirthday.isEmpty() || newUniversity.isEmpty() || newQualification.isEmpty() ||
                    newExperience.isEmpty() || newHardSkills.isEmpty() || newSoftSkills.isEmpty()) {
                Log.d(TAG, "VALIDATION FAILED - at least one field is empty");
                Toast.makeText(this, "Please fill in all fields to complete your profile", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Validation passed - building profileMap");
            HashMap<String, Object> profileMap = new HashMap<>();
            profileMap.put("gender", newGender);
            profileMap.put("birthday", newBirthday);
            profileMap.put("university", newUniversity);
            profileMap.put("education", newEducation);
            profileMap.put("qualification", newQualification);
            profileMap.put("experience", newExperience);
            profileMap.put("hardSkills", newHardSkills);
            profileMap.put("softSkills", newSoftSkills);

            Log.d(TAG, "selectedImageUri = " + selectedImageUri);
            if (selectedImageUri != null) {
                Log.d(TAG, "Calling uploadPhotoThenSaveProfile");
                uploadPhotoThenSaveProfile(profileMap);
            } else {
                Log.d(TAG, "Calling saveProfileFields directly (no photo)");
                saveProfileFields(profileMap);
            }
        });

        btnCancel.setOnClickListener(v -> finish());
        Log.d(TAG, "onCreate finished - listeners attached");
    }

    private int getIndex(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equalsIgnoreCase(value)) {
                return i;
            }
        }
        return -1;
    }

    private void showDatePickerDialog() {
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now());

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select your birthday")
                .setCalendarConstraints(constraintsBuilder.build())
                .build();

        datePicker.addOnPositiveButtonClickListener(selectionInMillis -> {

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selectionInMillis);

            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH) + 1;
            int year = calendar.get(Calendar.YEAR);

            String date = day + "/" + month + "/" + year;
            etBirthday.setText(date);
        });

        datePicker.show(getSupportFragmentManager(), "BIRTHDAY_PICKER");
    }

    private void loadExistingProfilePhoto() {
        dbUsersRef.child("photoUrl").get().addOnSuccessListener(snapshot -> {
            if (selectedImageUri != null) {
                Log.d(TAG, "Skipping old photo load - user already selected a new photo: " + selectedImageUri);
                return;
            }
            String photoUrl = snapshot.getValue(String.class);
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(this).load(photoUrl).placeholder(R.drawable.ic_person_placeholder).into(ivProfilePicture);
            }
        });
    }

    private void setupImagePickers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                Log.d(TAG, "Gallery picked: " + selectedImageUri);
                Glide.with(this).load(selectedImageUri).into(ivProfilePicture);
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraImageUri != null) {
                selectedImageUri = cameraImageUri;
                Log.d(TAG, "Camera photo taken: " + selectedImageUri);
                Glide.with(this).load(selectedImageUri).into(ivProfilePicture);
            }
        });

        cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Update Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndLaunch();
                    } else {
                        launchGallery();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir = getExternalFilesDir("Pictures");
            File photoFile = File.createTempFile("PROFILE_" + timeStamp, ".jpg", storageDir);
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            Toast.makeText(this, "Unable to open camera. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void uploadPhotoThenSaveProfile(HashMap<String, Object> profileMap) {
        Log.d(TAG, "uploadPhotoThenSaveProfile() started");
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();
        btnSave.setEnabled(false);
        CloudinaryUploader.uploadPhoto(this, selectedImageUri, new CloudinaryUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                Log.d(TAG, "Cloudinary upload SUCCESS: " + imageUrl);
                runOnUiThread(() -> {
                    profileMap.put("photoUrl", imageUrl);
                    saveProfileFields(profileMap);
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.d(TAG, "Cloudinary upload FAILED: " + errorMessage);
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(EditProfileActivity.this, "Photo upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveProfileFields(HashMap<String, Object> profileMap) {
        Log.d(TAG, "saveProfileFields() started - writing to Firebase");
        dbUsersRef.updateChildren(profileMap).addOnCompleteListener(task -> {
            btnSave.setEnabled(true);
            Log.d(TAG, "Firebase updateChildren complete. isSuccessful=" + task.isSuccessful());
            if (task.isSuccessful()) {
                Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Log.d(TAG, "Firebase write FAILED: " + task.getException());
                Toast.makeText(EditProfileActivity.this, "Failed to save changes. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}