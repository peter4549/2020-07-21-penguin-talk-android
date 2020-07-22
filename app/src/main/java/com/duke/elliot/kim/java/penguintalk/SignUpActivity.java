package com.duke.elliot.kim.java.penguintalk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.duke.elliot.kim.java.penguintalk.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

public class SignUpActivity extends AppCompatActivity {

    private final String TAG = "SignUpActivity";
    private final int REQUEST_CODE_PICK = 1000;

    private EditText editTextEmail;
    private EditText editTextName;
    private EditText editTextPassword;
    private ImageView imageViewProfile;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        String splashBackground = mFirebaseRemoteConfig.getString(getString(R.string.splash_background));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setStatusBarColor(Color.parseColor(splashBackground));

        imageViewProfile = findViewById(R.id.image_view_profile);
        imageViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent, REQUEST_CODE_PICK);
            }
        });

        editTextEmail = findViewById(R.id.edit_text_email);
        editTextName = findViewById(R.id.edit_text_name);
        editTextPassword = findViewById(R.id.edit_text_password);
        Button buttonSignUp = findViewById(R.id.button_sign_up);

        buttonSignUp.setBackgroundColor(Color.parseColor(splashBackground));

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (editTextEmail.getText() == null) {
                    Toast.makeText(SignUpActivity.this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (editTextName.getText() == null) {
                    Toast.makeText(SignUpActivity.this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (editTextPassword.getText() == null) {
                    Toast.makeText(SignUpActivity.this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (imageUri == null) {
                    Toast.makeText(SignUpActivity.this, "프로필 사진을 등록해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(
                                editTextEmail.getText().toString(),
                                editTextPassword.getText().toString())
                        .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    final String uid = task.getResult().getUser().getUid();

                                    UserProfileChangeRequest userProfileChangeRequest =
                                            new UserProfileChangeRequest.Builder()
                                                    .setDisplayName(editTextName.getText().toString())
                                                    .build();

                                    task.getResult().getUser().updateProfile(userProfileChangeRequest);

                                    FirebaseStorage.getInstance().getReference()
                                            .child("userImages").child(uid).putFile(imageUri)
                                            .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                                    String downloadUrl =
                                                            task.getResult().getMetadata().getReference().getDownloadUrl().toString();
                                                    UserModel user = new UserModel();

                                                    user.name = editTextName.getText().toString();
                                                    user.profilePictureUrl = downloadUrl;
                                                    user.uid = uid;

                                                    FirebaseDatabase.getInstance().getReference()
                                                            .child("users").child(uid).setValue(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            SignUpActivity.this.finish();
                                                        }
                                                    });
                                                }
                                            });


                                } else {
                                    Log.e(TAG, task.getException().toString());
                                }
                            }
                        });

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK && resultCode == RESULT_OK) {
            imageUri = data.getData();
            imageViewProfile.setImageURI(imageUri);
        }
    }
}
