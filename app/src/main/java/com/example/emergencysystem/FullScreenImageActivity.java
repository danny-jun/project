package com.example.emergencysystem;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

public class FullScreenImageActivity extends AppCompatActivity {

    private ImageView imageView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        imageView = findViewById(R.id.fullScreenImageView);
        progressBar = findViewById(R.id.progressBar);
        ImageView btnClose = findViewById(R.id.btnClose);

        // Get image URL from intent
        String imageUrl = getIntent().getStringExtra("IMAGE_URL");

        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load full-resolution image
        progressBar.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_broken_image)
                .into(imageView);

        progressBar.setVisibility(View.GONE);

        // Close button
        btnClose.setOnClickListener(v -> finish());

        // Click anywhere to close
        imageView.setOnClickListener(v -> finish());
    }
}
