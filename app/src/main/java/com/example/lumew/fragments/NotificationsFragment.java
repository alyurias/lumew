package com.example.lumew.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.lumew.R;
import com.example.lumew.adapters.NotificationAdapter;
import com.example.lumew.models.Notification;
import com.example.lumew.utils.FirebaseUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView emptyTextView;
    private ImageView emptyImageView;
    private Button markAllAsReadButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FirebaseUtil firebaseUtil;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        firebaseUtil = FirebaseUtil.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }
        currentUserId = currentUser.getUid();

        // Initialize views
        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        progressBar = view.findViewById(R.id.notifications_progress_bar);
        emptyView = view.findViewById(R.id.empty_notifications_view);
        emptyTextView = emptyView.findViewById(R.id.empty_text_view);
        emptyImageView = emptyView.findViewById(R.id.empty_image_view);
        markAllAsReadButton = view.findViewById(R.id.mark_all_as_read_button);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        // Set up empty view
        try {
            Typeface typeface = Typeface.createFromAsset(requireContext().getAssets(), "fonts/din_light.ttf");
            emptyTextView.setTypeface(typeface);
        } catch (Exception e) {
            emptyTextView.setTypeface(Typeface.DEFAULT);
        }
        emptyImageView.setContentDescription(getString(R.string.notifications_icon));

        // Setup RecyclerView
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(requireContext(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Initial state
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        markAllAsReadButton.setVisibility(View.GONE);

        // Load notifications
        loadNotifications();

        // Setup pull to refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshNotifications);

        // Mark all as read button
        markAllAsReadButton.setOnClickListener(v -> markAllNotificationsAsRead());
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        firebaseUtil.getNotifications(currentUserId, new FirebaseUtil.NotificationListCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                notificationList = notifications;
                adapter.updateNotifications(notificationList);

                if (notificationList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    markAllAsReadButton.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    markAllAsReadButton.setVisibility(View.VISIBLE);
                    updateMarkAllAsReadButtonState();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Error loading notifications", Toast.LENGTH_SHORT).show();

                // Show empty view on error
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                markAllAsReadButton.setVisibility(View.GONE);
            }
        });
    }

    private void updateMarkAllAsReadButtonState() {
        boolean hasUnreadNotifications = false;
        for (Notification notification : notificationList) {
            if (!notification.isRead()) {
                hasUnreadNotifications = true;
                break;
            }
        }
        markAllAsReadButton.setEnabled(hasUnreadNotifications);
        markAllAsReadButton.setAlpha(hasUnreadNotifications ? 1.0f : 0.5f);
    }

    private void refreshNotifications() {
        loadNotifications();
    }

    private void markAllNotificationsAsRead() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseUtil.markAllNotificationsAsRead(currentUserId, new FirebaseUtil.ResultCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                for (Notification notification : notificationList) {
                    notification.setRead(true);
                }
                adapter.notifyDataSetChanged();
                updateMarkAllAsReadButtonState();
                Toast.makeText(getContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to mark notifications as read", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onNotificationClick(Notification notification, int position) {
        // Handle notification click
    }

    @Override
    public void onNotificationMarkAsRead(Notification notification, int position) {
        firebaseUtil.toggleNotificationReadStatus(notification.getId(), notification.isRead(), new FirebaseUtil.ResultCallback() {
            @Override
            public void onSuccess() {
                notification.setRead(!notification.isRead());
                adapter.notifyItemChanged(position);
                updateMarkAllAsReadButtonState();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to update notification", Toast.LENGTH_SHORT).show();
            }
        });
    }
}