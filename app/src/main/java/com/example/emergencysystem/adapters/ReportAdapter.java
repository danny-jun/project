package com.example.emergencysystem.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emergencysystem.EmergencyReport;
import com.example.emergencysystem.R;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private final List<EmergencyReport> reportList;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public ReportAdapter(List<EmergencyReport> reportList, Context context) {
        this.reportList = reportList;
        this.context = context;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        EmergencyReport report = reportList.get(position);

        if (report == null) return;

        holder.txtEmergencyType.setText(report.getEmergencyType() != null ?
                report.getEmergencyType() : "Emergency");

        holder.txtDateTime.setText(report.getFormattedDate() != null ?
                report.getFormattedDate() : "Unknown date");

        String status = report.getStatus() != null ? report.getStatus() : "pending";
        String statusEmoji = getStatusEmoji(status);
        holder.txtStatus.setText(statusEmoji + " " + status);

        String severity = report.getSeverity() != null ? report.getSeverity() : "Medium";
        holder.txtSeverity.setText(severity);

        int severityColor = getSeverityColor(severity);
        holder.txtSeverity.setTextColor(severityColor);

        // Handle media indicators (simple text version)
        updateMediaSummary(holder, report);

        // Load thumbnail if available
        loadThumbnail(holder, report);

        if (report.isPanicAlert()) {
            holder.cardView.setCardBackgroundColor(0x30F44336); // Light red
        } else {
            holder.cardView.setCardBackgroundColor(0xFFFFFFFF); // White
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position);
            }
        });
    }

    private void updateMediaSummary(ReportViewHolder holder, EmergencyReport report) {
        int imageCount = (report.getImageUrls() != null) ? report.getImageUrls().size() : 0;
        int videoCount = (report.getVideoUrls() != null) ? report.getVideoUrls().size() : 0;

        if (imageCount > 0 || videoCount > 0) {
            StringBuilder summary = new StringBuilder();
            if (imageCount > 0) {
                summary.append("📷 ").append(imageCount);
            }
            if (videoCount > 0) {
                if (imageCount > 0) summary.append("  ");
                summary.append("🎬 ").append(videoCount);
            }
            holder.txtMediaSummary.setText(summary.toString());
            holder.txtMediaSummary.setVisibility(View.VISIBLE);
        } else {
            holder.txtMediaSummary.setVisibility(View.GONE);
        }
    }

    private void loadThumbnail(ReportViewHolder holder, EmergencyReport report) {
        // Try to load first image or video thumbnail
        String thumbnailUrl = null;
        
        if (report.getImageUrls() != null && !report.getImageUrls().isEmpty()) {
            thumbnailUrl = report.getImageUrls().get(0);
        } else if (report.getVideoUrls() != null && !report.getVideoUrls().isEmpty()) {
            String videoUrl = report.getVideoUrls().get(0);
            // Generate Cloudinary video thumbnail
            if (videoUrl.contains("cloudinary.com")) {
                thumbnailUrl = videoUrl.replace("/upload/", "/upload/so_0,w_160,h_160,c_fill/") + ".jpg";
            } else {
                thumbnailUrl = videoUrl;
            }
        }

        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            holder.imgThumbnail.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .centerCrop()
                    .override(160, 160)
                    .into(holder.imgThumbnail);
        } else {
            holder.imgThumbnail.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reportList != null ? reportList.size() : 0;
    }

    private String getStatusEmoji(String status) {
        if (status == null) return "📋";
        switch (status.toLowerCase()) {
            case "pending": return "⏳";
            case "responded": return "🚑";
            case "completed": return "✅";
            default: return "📋";
        }
    }

    private int getSeverityColor(String severity) {
        if (severity == null) return context.getResources().getColor(android.R.color.darker_gray);

        switch (severity.toLowerCase()) {
            case "low":
                return context.getResources().getColor(android.R.color.holo_green_dark);
            case "medium":
                return context.getResources().getColor(android.R.color.holo_orange_dark);
            case "high":
                return context.getResources().getColor(android.R.color.holo_red_light);
            case "critical":
                return context.getResources().getColor(android.R.color.holo_red_dark);
            default:
                return context.getResources().getColor(android.R.color.darker_gray);
        }
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imgThumbnail;
        TextView txtEmergencyType, txtDateTime, txtStatus, txtSeverity, txtMediaSummary;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardReport);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            txtEmergencyType = itemView.findViewById(R.id.txtEmergencyType);
            txtDateTime = itemView.findViewById(R.id.txtDateTime);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtSeverity = itemView.findViewById(R.id.txtSeverity);
            txtMediaSummary = itemView.findViewById(R.id.txtMediaSummary);
        }
    }
}