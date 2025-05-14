package com.example.lumew.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lumew.R;
import com.example.lumew.models.Notification;
import com.example.lumew.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notifications;
    private final Context context;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification, int position);
        void onNotificationMarkAsRead(Notification notification, int position);
    }

    public NotificationAdapter(Context context, OnNotificationClickListener listener) {
        this.notifications = new ArrayList<>();
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        if (notifications == null || position >= notifications.size()) {
            return;
        }

        Notification notification = notifications.get(position);
        holder.bind(notification);

        holder.itemView.setOnClickListener(v -> {
            if (notification.isUpdating()) {
                return;
            }

            if (listener != null) {
                listener.onNotificationClick(notification, position);
            }

            if (!notification.isRead()) {
                markAsRead(notification, position);
            }
        });
    }

    private void markAsRead(Notification notification, int position) {
        // Optimistic UI update
        notification.setRead(true);
        notification.setUpdating(true);
        notifyItemChanged(position);

        if (listener != null) {
            listener.onNotificationMarkAsRead(notification, position);
        }
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    public void updateNotifications(List<Notification> newNotifications) {
        if (newNotifications == null) {
            this.notifications = new ArrayList<>();
        } else {
            this.notifications = new ArrayList<>(newNotifications);
        }
        notifyDataSetChanged();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView message;
        private final TextView time;
        private final View readIndicator;
        private final ProgressBar progressBar;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            message = itemView.findViewById(R.id.notification_message);
            time = itemView.findViewById(R.id.notification_time);
            readIndicator = itemView.findViewById(R.id.read_indicator);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }

        public void bind(Notification notification) {
            if (notification == null) return;

            title.setText(notification.getTitle() != null ? notification.getTitle() : "");
            message.setText(notification.getMessage() != null ? notification.getMessage() : "");
            time.setText(notification.getTimeAgo() != null ? notification.getTimeAgo() : "");

            progressBar.setVisibility(notification.isUpdating() ? View.VISIBLE : View.GONE);
            readIndicator.setVisibility(!notification.isRead() && !notification.isUpdating() ? View.VISIBLE : View.GONE);
        }
    }
}