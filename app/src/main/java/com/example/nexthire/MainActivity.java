package com.example.nexthire;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import android.location.Location;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {
    private TextView tvWelcomeUser, tvApplied, tvApproved, tvRejected;
    private ListView jobListView;
    private ArrayList<Job> allJobsList;
    private ArrayList<Job> dashboardDisplayList;
    private JobAdapter adapter;
    private LinearLayout btnHome, btnNoti, btnProfile;
    private ImageView btnSearchClick;
    private EditText etSearchJob;
    private String incomingName = "User";
    private String currentUserId = "";
    private static final String CHANNEL_ID = "nexthire_fcm_channel";
    private HashMap<String, String> previousStatusMap = new HashMap<>();
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> locationPermissionLauncher;

    // key ("username|jobTitle|companyName") -> Firebase SavedJobs push key.
    // Kept in sync with the database so every JobAdapter can instantly show
    // the correct bookmark icon state without a per-row database call.
    private HashMap<String, String> savedJobsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvWelcomeUser = findViewById(R.id.tv_welcome_user);
        tvApplied = findViewById(R.id.tv_stat_applied);
        tvApproved = findViewById(R.id.tv_stat_approved);
        tvRejected = findViewById(R.id.tv_stat_rejected);
        jobListView = findViewById(R.id.job_list_view);
        btnHome = findViewById(R.id.nav_home_layout);
        btnNoti = findViewById(R.id.nav_noti_layout);
        btnProfile = findViewById(R.id.nav_profile_layout);
        etSearchJob = findViewById(R.id.et_search_job);
        btnSearchClick = findViewById(R.id.btn_search_click);

        LinearLayout btnNearMe = findViewById(R.id.btn_near_me);
        btnNearMe.setOnClickListener(v -> checkLocationPermissionAndFindJobs());

        // Stat cards click listeners
        LinearLayout statAppliedLayout = findViewById(R.id.stat_applied_layout);
        LinearLayout statApprovedLayout = findViewById(R.id.stat_approved_layout);
        LinearLayout statRejectedLayout = findViewById(R.id.stat_rejected_layout);
        statAppliedLayout.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ApplicationListActivity.class);
            intent.putExtra("STATUS", "Pending");
            intent.putExtra("USER_NAME", incomingName);
            startActivity(intent);
        });
        statApprovedLayout.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ApplicationListActivity.class);
            intent.putExtra("STATUS", "Approved");
            intent.putExtra("USER_NAME", incomingName);
            startActivity(intent);
        });
        statRejectedLayout.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ApplicationListActivity.class);
            intent.putExtra("STATUS", "Rejected");
            intent.putExtra("USER_NAME", incomingName);
            startActivity(intent);
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }
        tvWelcomeUser.setText("Loading...");
        if (!currentUserId.isEmpty()) {
            DatabaseReference dbUserRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
            dbUserRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String nameFromDb = snapshot.child("name").getValue(String.class);
                        if (nameFromDb != null && !nameFromDb.isEmpty()) {
                            incomingName = nameFromDb;
                            tvWelcomeUser.setText("Welcome, " + incomingName);
                            if (jobListView.getAdapter() != null) {
                                adapter = new JobAdapter(MainActivity.this, dashboardDisplayList, incomingName, savedJobsMap);
                                jobListView.setAdapter(adapter);
                            }
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        DatabaseReference dbApps = FirebaseDatabase.getInstance().getReference("Applications");
        dbApps.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int countApplied = 0;
                int countApproved = 0;
                int countRejected = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String applicant = ds.child("applicantName").getValue(String.class);
                    if (incomingName.equalsIgnoreCase(applicant)) {
                        countApplied++;
                        String status = ds.child("status").getValue(String.class);
                        String jobTitle = ds.child("jobTitle").getValue(String.class);
                        String appKey = ds.getKey();
                        if ("Approved".equalsIgnoreCase(status)) {
                            countApproved++;
                        } else if ("Rejected".equalsIgnoreCase(status)) {
                            countRejected++;
                        }
                        if (previousStatusMap.containsKey(appKey)) {
                            String oldStatus = previousStatusMap.get(appKey);
                            if (oldStatus != null && status != null && !oldStatus.equalsIgnoreCase(status)
                                    && ("Approved".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status))) {
                                triggerStatusNotification(status, jobTitle);
                            }
                        }
                        previousStatusMap.put(appKey, status);
                    }
                }
                tvApplied.setText(String.valueOf(countApplied));
                tvApproved.setText(String.valueOf(countApproved));
                tvRejected.setText(String.valueOf(countRejected));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Keeps savedJobsMap in sync with Firebase so every job card's bookmark
        // icon reflects real saved state, and refreshes the adapter when it changes.
        DatabaseReference dbSavedJobs = FirebaseDatabase.getInstance().getReference("SavedJobs");
        dbSavedJobs.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                savedJobsMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String user = ds.child("username").getValue(String.class);
                    String title = ds.child("jobTitle").getValue(String.class);
                    String company = ds.child("companyName").getValue(String.class);
                    if (incomingName.equalsIgnoreCase(user)) {
                        String key = JobAdapter.buildSaveKey(user, title, company);
                        savedJobsMap.put(key, ds.getKey());
                    }
                }
                if (adapter != null) adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        findNearbyJobs();
                    } else {
                        Toast.makeText(this, "Location permission is needed to find nearby jobs", Toast.LENGTH_SHORT).show();
                    }
                });

        // =========================================================================
        // DATA JOBS - SEMUA ADA!
        // =========================================================================
        allJobsList = new ArrayList<>();
        allJobsList.add(new Job(
                "Data Analyst", "PETRONAS", "RM 4,500 - RM 6,000", "Available",
                "Join PETRONAS's Group Digital Insights division to leverage cutting-edge big data technologies.",
                "• Advanced Dashboard Design\n• Operational Telemetry Analysis\n• Cross-Functional Data Sync\n• Predictive Insights Reporting",
                "• Bachelor's Degree in Data Science\n• Proficient in SQL, Python\n• Open to Fresh Graduates",
                "60142939864", "hr@petronas.com.my", "https://www.petronas.com", 3.1579, 101.7116));
        allJobsList.add(new Job(
                "Petroleum Engineer", "PETRONAS", "RM 7,000 - RM 11,000", "Available",
                "PETRONAS Upstream is seeking an ambitious Petroleum Engineer to spearhead exploration projects.",
                "• Reservoir Simulation Modeling\n• Well Intervention Design\n• Real-Time Production Auditing\n• On-Site Offshore Coordination",
                "• Registered with BEM\n• Bachelor's Degree in Petroleum Engineering\n• 2+ years experience",
                "60142939864", "hr@petronas.com.my", "https://www.petronas.com", 3.1579, 101.7116));
        allJobsList.add(new Job(
                "HSE Officer", "PETRONAS", "RM 4,000 - RM 5,500", "Available",
                "Become the primary guardian of workplace safety within PETRONAS facilities.",
                "• Strict Regulatory Compliance\n• Routine Site Inspections\n• Emergency Preparedness Training\n• Incident Diagnostics",
                "• Green Book certified\n• Bachelor's Degree in OSH\n• HIRARC knowledge",
                "60142939864", "hr@petronas.com.my", "https://www.petronas.com", 3.1579, 101.7116));
        allJobsList.add(new Job(
                "Software Architect", "PETRONAS", "RM 9,000 - RM 13,000", "Available",
                "PETRONAS Digital is looking for an expert Software Architect to revolutionize our digital energy ecosystem.",
                "• Infrastructure Blueprinting\n• Multi-Cloud Strategy\n• Technical Governance\n• Tech Stack Modernization",
                "• 5+ years experience\n• AWS/Azure Certified\n• Expert in Go, Java, or Node.js",
                "60142939864", "hr@petronas.com.my", "https://www.petronas.com", 3.1579, 101.7116));
        allJobsList.add(new Job(
                "Procurement Executive", "PETRONAS", "RM 3,800 - RM 5,000", "Available",
                "Join our global Supply Chain Management division as a Corporate Procurement Executive.",
                "• Large Value Negotiation\n• Vendor Lifecycle Auditing\n• Expenditure Governance\n• Supply Continuity Tracking",
                "• Bachelor's Degree in Supply Chain\n• SAP/ERP knowledge\n• Strong negotiation skills",
                "60142939864", "hr@petronas.com.my", "https://www.petronas.com", 3.1579, 101.7116));
        allJobsList.add(new Job(
                "Corporate Lawyer", "PETRONAS", "RM 6,500 - RM 9,000", "Available",
                "PETRONAS Legal Affairs is looking for an analytical Corporate Lawyer.",
                "• Joint Venture Structuring\n• Regulatory Compliance Review\n• Dispute Strategy Coordination\n• Internal Policy Advisory",
                "• LLB degree\n• Called to Malaysian Bar\n• 3+ years experience",
                "60142939864", "hr@petronas.com.my", "https://www.petronas.com", 3.1579, 101.7116));
        allJobsList.add(new Job(
                "Management Trainee", "Maybank", "RM 3,800 - RM 5,000", "Available",
                "The Maybank Global Maybank Apprentice Programme (GMAP) for high-potential future leaders.",
                "• Executive Rotational Cycles\n• Strategic Innovation Sprints\n• Financial Analysis Projects\n• Operations Leadership",
                "• Bachelor's Degree with CGPA 3.50+\n• Leadership experience\n• Strong communication skills",
                "60142939864", "hr@maybank.com", "https://www.maybank.com", 3.1473, 101.6994));
        allJobsList.add(new Job(
                "Credit Risk Analyst", "Maybank", "RM 4,500 - RM 6,500", "Available",
                "Join Maybank's Group Risk Management division to defend the bank's credit portfolio.",
                "• Financial Modeling Analysis\n• Collateral Assessment\n• Credit Rating Allocation\n• Macro Portfolio Scanning",
                "• Bachelor's in Finance/Economics\n• Excel/SAS skills\n• Strong analytical skills",
                "60142939864", "hr@maybank.com", "https://www.maybank.com", 3.1473, 101.6994));
        allJobsList.add(new Job(
                "Cybersecurity Specialist", "Maybank", "RM 8,000 - RM 12,000", "Available",
                "Protect Malaysia's largest digital banking platform from advanced persistent threats.",
                "• Real-Time Threat Hunting\n• Penetration Testing Audits\n• Incident Response Command\n• Secure Code Integration",
                "• CEH/CISSP certified\n• 3+ years experience\n• Linux/Network expertise",
                "60142939864", "hr@maybank.com", "https://www.maybank.com", 3.1473, 101.6994));
        allJobsList.add(new Job(
                "Investment Banker", "Maybank", "RM 6,000 - RM 9,500", "Available",
                "Maybank Investment Bank is looking for an analytical Corporate Advisory Investment Banker.",
                "• Deal Packaging Structure\n• Due Diligence Execution\n• Prospectus Authoring\n• Market Advisory Tracking",
                "• Bachelor's in Accounting/Finance\n• CFA Level 1/2 preferred\n• Strong quantitative skills",
                "60142939864", "hr@maybank.com", "https://www.maybank.com", 3.1473, 101.6994));
        allJobsList.add(new Job(
                "Customer Relationship Manager", "Maybank", "RM 4,000 - RM 5,800", "Available",
                "Join Maybank Premier Wealth Management as a dedicated Relationship Manager.",
                "• Portfolio Wealth Allocation\n• Client Acquisition Strategies\n• Relationship Pipeline Preservation\n• Compliance Document Review",
                "• FIMM/IPPC licensed\n• 2+ years wealth management\n• Strong communication skills",
                "60142939864", "hr@maybank.com", "https://www.maybank.com", 3.1473, 101.6994));
        allJobsList.add(new Job(
                "UI/UX Designer", "Maybank", "RM 5,000 - RM 7,200", "Available",
                "Maybank's Digital Product Team is looking for an innovative UI/UX Designer.",
                "• User Journey Mapping\n• Usability Testing Coordination\n• Design System Governance\n• Cross-Functional Development Sync",
                "• Portfolio required\n• Figma/Adobe XD expertise\n• 2+ years experience",
                "60142939864", "hr@maybank.com", "https://www.maybank.com", 3.1473, 101.6994));
        allJobsList.add(new Job(
                "HR Executive", "Maxis", "RM 3,500 - RM 4,500", "Available",
                "Maxis People & Organization department is seeking an energetic HR Executive.",
                "• End-to-End Sourcing\n• Onboarding Acceleration\n• Performance Evaluation Audits\n• Training Blueprint Development",
                "• Bachelor's in HR Management\n• HRIS knowledge\n• Strong interpersonal skills",
                "60142939864", "hr@maxis.com.my", "https://www.maxis.com.my", 3.1570, 101.7120));
        allJobsList.add(new Job(
                "Network Engineer", "Maxis", "RM 4,500 - RM 6,000", "Available",
                "Join the core engineering team scaling Malaysia's premier 5G telecommunications infrastructure.",
                "• Core Infrastructure Maintenance\n• 5G Base Station Tuning\n• Network Scalability Execution\n• 24/7 Incident Remediation",
                "• CCNA/CCNP certified\n• Bachelor's in Telecom/Network Engineering\n• BGP/OSPF knowledge",
                "60142939864", "hr@maxis.com.my", "https://www.maxis.com.my", 3.1570, 101.7120));
        allJobsList.add(new Job(
                "Product Manager", "Maxis", "RM 7,500 - RM 10,500", "Available",
                "Maxis Home Fibre is seeking a commercial Product Manager to design consumer broadband offerings.",
                "• Product Portfolio Roadmap\n• Data-Driven Pricing Architecture\n• Go-To-Market Alignment\n• Competitor Market Analysis",
                "• 3+ years product management\n• Bachelor's in Business/Marketing\n• Agile experience",
                "60142939864", "hr@maxis.com.my", "https://www.maxis.com.my", 3.1570, 101.7120));
        allJobsList.add(new Job(
                "Cloud Engineer", "Maxis", "RM 6,000 - RM 8,500", "Available",
                "Maxis Business is looking for a Cloud Engineer to build enterprise-grade hybrid cloud architectures.",
                "• Enterprise Cloud Migration\n• Infrastructure Automation\n• Container Management\n• Cloud Cost Optimizations",
                "• AWS/Azure certified\n• Bachelor's in IT/Engineering\n• Linux/Terraform expertise",
                "60142939864", "hr@maxis.com.my", "https://www.maxis.com.my", 3.1570, 101.7120));
        allJobsList.add(new Job(
                "Retail Store Manager", "Maxis", "RM 3,800 - RM 5,000", "Available",
                "Take complete leadership ownership of a flagship Maxis Centre retail storefront.",
                "• Frontline Performance Management\n• Customer Experience Metrics\n• Inventory Management Audits\n• Operations Governance Compliance",
                "• 2+ years retail management\n• Diploma/Bachelor's degree\n• Strong leadership skills",
                "60142939864", "hr@maxis.com.my", "https://www.maxis.com.my", 3.1570, 101.7120));
        allJobsList.add(new Job(
                "Social Media Content Specialist", "Maxis", "RM 3,500 - RM 4,800", "Available",
                "Maxis Brand and Marketing Communications division is seeking a creative Social Media Content Specialist.",
                "• Short-Form Video Creation\n• Campaign Content Alignment\n• Community Engagement Tracking\n• Performance Metrics Analytics",
                "• Digital portfolio required\n• Bachelor's in Media/Comms\n• CapCut/Adobe Premiere skills",
                "60142939864", "hr@maxis.com.my", "https://www.maxis.com.my", 3.1570, 101.7120));

        // =========================================================================
        // DASHBOARD DISPLAY JOBS (7 featured jobs)
        // =========================================================================
        dashboardDisplayList = new ArrayList<>();
        dashboardDisplayList.add(new Job(
                "Android Developer", "Google Malaysia", "RM 8,500 - RM 12,000", "Available",
                "Join Google's dynamic mobile core team to design and develop large-scale consumer applications.",
                "• Codebase Review & Documentation\n• Feature Development\n• Automation Testing\n• Collaborative Sync",
                "• Bachelor's in Computer Science\n• Java/Kotlin expertise\n• Firebase/Git knowledge",
                "60142939864", "hr@google.com", "https://careers.google.com", 3.1386, 101.6841));
        dashboardDisplayList.add(new Job(
                "E-Commerce Operations Lead", "Shopee", "RM 5,000 - RM 7,000", "Available",
                "Shopee is looking for a driven Operations Lead to head our regional logistics and fulfillment division.",
                "• Pipeline Optimization\n• Vendor Allocation\n• Data Analysis\n• Team Leadership",
                "• 2+ years e-commerce/logistics\n• Bachelor's in Logistics/Business\n• Excel/SQL skills",
                "60142939864", "hr@shopee.com.my", "https://shopee.com.my", 3.1593, 101.6143));
        dashboardDisplayList.add(new Job(
                "Backend Engineer (Go)", "Grab", "RM 7,500 - RM 10,000", "Available",
                "Build scalable microservices that power millions of real-time ride-hailing bookings across Southeast Asia.",
                "• Microservices Architecture\n• Database Scaling\n• API Integration\n• Cloud Deployment",
                "• Go/Java/C++ expertise\n• Docker/Kubernetes knowledge\n• MySQL/Redis experience",
                "60142939864", "hr@grab.com", "https://grab.careers", 3.1118, 101.6641));
        dashboardDisplayList.add(new Job(
                "Software QA Tester", "Touch 'n Go Digital", "RM 4,200 - RM 5,800", "Available",
                "Join the team securing Malaysia's largest e-wallet ecosystem as a Software QA Tester.",
                "• Test Automation\n• Bug Life Cycle\n• Performance Testing\n• Security Checks",
                "• Selenium/Appium skills\n• Bachelor's in IT\n• Agile/STLC knowledge",
                "60142939864", "hr@tngdigital.com.my", "https://www.touchngo.com.my", 3.1592, 101.6631));
        dashboardDisplayList.add(new Job(
                "Firmware Engineer", "Intel Malaysia", "RM 6,000 - RM 8,500", "Available",
                "Write hardware-abstracted code to initialize and control next-generation microprocessor computing cores.",
                "• Firmware Authoring\n• Hardware Debugging\n• Protocol Execution\n• Code Optimization",
                "• C/Assembly expertise\n• Degree in Embedded Systems/EE\n• x86/ARM architecture knowledge",
                "60142939864", "hr@intel.com", "https://www.intel.com", 5.3414, 100.2821));
        dashboardDisplayList.add(new Job(
                "Digital Marketing Manager", "Zalora", "RM 5,200 - RM 6,800", "Available",
                "Lead our growth-marketing and customer acquisition funnels as a Digital Marketing Manager.",
                "• Campaign Execution\n• Budget Allocation\n• Conversion Tracking\n• A/B Testing",
                "• 3+ years performance marketing\n• Google Ads/Meta Ads expertise\n• Google Analytics knowledge",
                "60142939864", "hr@zalora.com.my", "https://www.zalora.com.my", 3.1495, 101.6166));
        dashboardDisplayList.add(new Job(
                "AI Research Scientist", "Aerodyne Group", "RM 9,000 - RM 14,000", "Available",
                "Pioneer advanced autonomous flight computing models as an AI Research Scientist.",
                "• Model Architecture\n• Network Optimization\n• Dataset Pipeline\n• Academic Output",
                "• Master's/PhD in AI/Data Science\n• Python/PyTorch/TensorFlow\n• CUDA/GPU experience",
                "60142939864", "hr@aerodyne.group", "https://aerodyne.group", 2.9431, 101.6998));

        // Setup adapter
        adapter = new JobAdapter(this, dashboardDisplayList, incomingName, savedJobsMap);
        jobListView.setAdapter(adapter);

        // Search
        btnSearchClick.setOnClickListener(v -> {
            String keyword = etSearchJob.getText().toString().trim().toLowerCase();
            if (keyword.isEmpty()) {
                adapter = new JobAdapter(MainActivity.this, dashboardDisplayList, incomingName, savedJobsMap);
                jobListView.setAdapter(adapter);
                Toast.makeText(MainActivity.this, "Displaying all featured jobs", Toast.LENGTH_SHORT).show();
            } else {
                ArrayList<Job> tempFiltered = new ArrayList<>();
                for (Job item : dashboardDisplayList) {
                    if (item.getCompanyName().toLowerCase().contains(keyword) || item.getJobTitle().toLowerCase().contains(keyword)) {
                        tempFiltered.add(item);
                    }
                }
                for (Job item : allJobsList) {
                    if (item.getCompanyName().toLowerCase().contains(keyword) || item.getJobTitle().toLowerCase().contains(keyword)) {
                        tempFiltered.add(item);
                    }
                }
                adapter = new JobAdapter(MainActivity.this, tempFiltered, incomingName, savedJobsMap);
                jobListView.setAdapter(adapter);
                if (tempFiltered.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No jobs found for: " + keyword, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Bottom navigation
        btnHome.setOnClickListener(v -> {
            etSearchJob.setText("");
            adapter = new JobAdapter(MainActivity.this, dashboardDisplayList, incomingName, savedJobsMap);
            jobListView.setAdapter(adapter);
        });
        btnNoti.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
            intent.putExtra("USER_NAME", incomingName);
            startActivity(intent);
        });
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            intent.putExtra("USER_NAME", incomingName);
            startActivity(intent);
        });
    }

    // ===== Class-level methods (moved out of onCreate) =====
    private void triggerStatusNotification(String status, String jobTitle) {
        String title;
        String body;
        if ("Approved".equalsIgnoreCase(status)) {
            title = "Application Approved! 🎉";
            body = "Your application for " + jobTitle + " has been approved!";
        } else {
            title = "Application Update";
            body = "Your application for " + jobTitle + " was not successful this time.";
        }
        saveNotificationToFirebase(title, body);
        sendLocalNotification(title, body);
    }

    private void checkLocationPermissionAndFindJobs() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            findNearbyJobs();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void findNearbyJobs() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        Toast.makeText(this, "Couldn't get your location. Try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ArrayList<Job> combined = new ArrayList<>();
                    combined.addAll(dashboardDisplayList);
                    for (Job job : allJobsList) {
                        if (!combined.contains(job)) combined.add(job);
                    }

                    for (Job job : combined) {
                        float[] results = new float[1];
                        Location.distanceBetween(
                                location.getLatitude(), location.getLongitude(),
                                job.getLatitude(), job.getLongitude(),
                                results);
                        double distanceKm = results[0] / 1000.0;
                        job.setDistanceKm(distanceKm);
                    }

                    final double MAX_DISTANCE_KM = 26.0;
                    ArrayList<Job> nearbyOnly = new ArrayList<>();
                    for (Job job : combined) {
                        if (job.getDistanceKm() <= MAX_DISTANCE_KM) {
                            nearbyOnly.add(job);
                        }
                    }

                    Collections.sort(nearbyOnly, Comparator.comparingDouble(Job::getDistanceKm));

                    adapter = new JobAdapter(MainActivity.this, nearbyOnly, incomingName, savedJobsMap);
                    jobListView.setAdapter(adapter);

                    if (nearbyOnly.isEmpty()) {
                        Toast.makeText(this, "No jobs found within 26 km of you", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Showing " + nearbyOnly.size() + " job(s) within 26 km", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveNotificationToFirebase(String title, String body) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("Notifications")
                    .child(userId)
                    .push();
            String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date());
            HashMap<String, String> notificationMap = new HashMap<>();
            notificationMap.put("title", title);
            notificationMap.put("body", body);
            notificationMap.put("timestamp", timestamp);
            notificationMap.put("read", "false");
            dbRef.setValue(notificationMap);
        }
    }

    private void sendLocalNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "NextHire Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for job application status updates");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        notificationManager.notify(1001, builder.build());
    }
}