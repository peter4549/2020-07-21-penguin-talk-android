package com.duke.elliot.kim.java.penguintalk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class SplashActivity extends AppCompatActivity {

    private final String TAG = "SplashActivity";
    private FirebaseRemoteConfig firebaseRemoteConfig;
    private LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        linearLayout = findViewById(R.id.linear_layout_activity_splash);

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config);
        firebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult() != null)
                                Log.d(TAG, "Config params updated: " + task.getResult());
                        } else
                            Toast.makeText(SplashActivity.this, "Fetch failed",
                                    Toast.LENGTH_SHORT).show();
                        displayMessage();
                    }
                });
    }

    void displayMessage() {
        String splashBackground = firebaseRemoteConfig.getString("splash_background");
        boolean messageCaps = firebaseRemoteConfig.getBoolean("splash_message_caps");
        String splashMessage = firebaseRemoteConfig.getString("splash_message");

        linearLayout.setBackgroundColor(Color.parseColor(splashBackground));

        if (messageCaps) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(splashMessage)
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    });

            builder.create().show();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}
