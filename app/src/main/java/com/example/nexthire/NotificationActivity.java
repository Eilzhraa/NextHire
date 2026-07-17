package com.example.nexthire;

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
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private ListView notificationListView;
    private TextView tvEmptyState;
    private ImageView btnBack;
    private List<NotificationItem> notificationList;
    private NotificationAdapter adapter;
    private DatabaseReference dbRef;
    private ValueEventListener valueEventListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        notificationListView = findViewById(R.id.notification_list_view);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(this, notificationList);
        notificationListView.setAdapter(adapter);

        notificationListView.setOnItemClickListener((parent, view, position, id) -> {
            NotificationItem item = notificationList.get(position);
            markAsRead(item);
        });

        loadNotifications();
    }

    private void loadNotifications() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            tvEmptyState.setVisibility(TextView.VISIBLE);
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference("Notifications").child(currentUserId);

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String title = ds.child("title").getValue(String.class);
                    String body = ds.child("body").getValue(String.class);
                    String timestamp = ds.child("timestamp").getValue(String.class);
                    String read = ds.child("read").getValue(String.class);

                    if (title != null && body != null) {
                        NotificationItem item = new NotificationItem(title, body, timestamp, read);
                        item.setKey(ds.getKey());
                        notificationList.add(item);
                    }
                }

                adapter.notifyDataSetChanged();

                if (notificationList.isEmpty()) {
                    tvEmptyState.setVisibility(TextView.VISIBLE);
                    notificationListView.setVisibility(TextView.GONE);
                } else {
                    tvEmptyState.setVisibility(TextView.GONE);
                    notificationListView.setVisibility(TextView.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        dbRef.addValueEventListener(valueEventListener);
    }

    private void markAsRead(NotificationItem item) {
        if ("false".equals(item.getRead())) {
            item.setRead("true");
            adapter.notifyDataSetChanged();
            dbRef.child(item.getKey()).child("read").setValue("true");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbRef != null && valueEventListener != null) {
            dbRef.removeEventListener(valueEventListener);
        }
    }
}