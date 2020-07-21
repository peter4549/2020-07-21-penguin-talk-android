package com.duke.elliot.kim.java.penguintalk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;

import com.duke.elliot.kim.java.penguintalk.fragments.ChatFragment;
import com.duke.elliot.kim.java.penguintalk.fragments.PeopleFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_people:
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.frame_layout, new PeopleFragment()).commit();
                        return true;
                    case R.id.menu_chat:
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.frame_layout, new ChatFragment()).commit();
                        return true;
                }

                return false;
            }
        });


    }

    @Override
    protected void onDestroy() {
        FirebaseAuth.getInstance().signOut();
        super.onDestroy();
    }
}
