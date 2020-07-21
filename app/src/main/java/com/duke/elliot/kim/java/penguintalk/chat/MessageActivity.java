package com.duke.elliot.kim.java.penguintalk.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.duke.elliot.kim.java.penguintalk.R;
import com.duke.elliot.kim.java.penguintalk.model.ChatModel;
import com.duke.elliot.kim.java.penguintalk.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MessageActivity extends AppCompatActivity {

    private final String TAG = "MessageActivity";

    private Button buttonSend;
    private EditText editTextMessage;
    private RecyclerView recyclerViewMessage;
    private String otherUid;
    private String uid;
    private String chatRoomId;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        buttonSend = findViewById(R.id.button_send);
        editTextMessage = findViewById(R.id.edit_text_message);
        recyclerViewMessage = findViewById(R.id.recycler_view_message);

        otherUid = getIntent().getStringExtra("otherUid");
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatModel chat = new ChatModel();
                chat.users.put(uid, true);
                chat.users.put(otherUid, true);

                if (chatRoomId == null) {
                    buttonSend.setEnabled(false);
                    FirebaseDatabase.getInstance().getReference()
                            .child("chat_rooms").push().setValue(chat)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    checkChatRoom();
                                }
                            });
                } else {
                    if (!editTextMessage.getText().toString().equals("")) {
                        ChatModel.Comment comment = new ChatModel.Comment();
                        comment.message = editTextMessage.getText().toString();
                        comment.uid = uid;
                        comment.timestamp = ServerValue.TIMESTAMP;
                        FirebaseDatabase.getInstance().getReference()
                                .child("chat_rooms").child(chatRoomId).child("comments")
                                .push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                editTextMessage.setText("");
                            }
                        });
                    } else
                        Toast.makeText(MessageActivity.this, "메시지를 입력해주세요.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        checkChatRoom();
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.anim_from_left, R.anim.anim_to_right);
    }

    void checkChatRoom() {
        FirebaseDatabase.getInstance().getReference()
                .child("chat_rooms").orderByChild("users/" + uid)
                .equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot: snapshot.getChildren()) {
                    ChatModel chat = dataSnapshot.getValue(ChatModel.class);
                    if (chat.users.containsKey(otherUid)) {
                        chatRoomId = dataSnapshot.getKey();
                        buttonSend.setEnabled(true);
                        recyclerViewMessage.setLayoutManager(new LinearLayoutManager(MessageActivity.this));
                        recyclerViewMessage.setAdapter(new RecyclerViewAdapter());

                        if (!editTextMessage.getText().toString().equals("")) {
                            ChatModel.Comment comment = new ChatModel.Comment();
                            comment.message = editTextMessage.getText().toString();
                            comment.uid = uid;
                            comment.timestamp = ServerValue.TIMESTAMP;
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chat_rooms").child(chatRoomId).child("comments")
                                    .push().setValue(comment);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        List<ChatModel.Comment> comments;
        UserModel user;

        public RecyclerViewAdapter() {
            comments = new ArrayList<>();

            FirebaseDatabase.getInstance().getReference().child("users").child(otherUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    user = snapshot.getValue(UserModel.class);
                    getMessageList();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });


        }

        private void getMessageList() {
            FirebaseDatabase.getInstance().getReference()
                    .child("chat_rooms").child(chatRoomId).child("comments")
                    .addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                            comments.add(snapshot.getValue(ChatModel.Comment.class));
                            notifyItemInserted(comments.size());
                            recyclerViewMessage.scrollToPosition(comments.size() - 1);
                        }

                        @Override
                        public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        }

                        @Override
                        public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                        }

                        @Override
                        public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewMessage;
            TextView textViewName;
            ImageView imageViewProfile;
            LinearLayout linearLayoutMessage;
            LinearLayout linearLayoutItemView;
            TextView textViewTimestamp;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewMessage = itemView.findViewById(R.id.text_view_message);
                textViewName = itemView.findViewById(R.id.text_view_name);
                imageViewProfile = itemView.findViewById(R.id.image_view_profile);
                linearLayoutMessage = itemView.findViewById(R.id.linear_layout_message);
                linearLayoutItemView = itemView.findViewById(R.id.linear_layout_item_view);
                textViewTimestamp = itemView.findViewById(R.id.text_view_timestamp);
            }
        }

        @NonNull
        @Override
        public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_message, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
            if (comments.get(position).uid.equals(uid)) {
                holder.textViewMessage.setText(comments.get(position).message);
                holder.textViewMessage.setBackgroundResource(R.drawable.chat_bubble_right);
                holder.linearLayoutMessage.setVisibility(View.GONE);
                holder.textViewMessage.setTextSize(24);
                holder.linearLayoutItemView.setGravity(Gravity.END);
            } else {
                Glide.with(holder.imageViewProfile.getContext())
                        .load(user.profilePictureUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(holder.imageViewProfile);
                holder.textViewName.setText(user.name);
                holder.linearLayoutMessage.setVisibility(View.VISIBLE);
                holder.textViewMessage.setBackgroundResource(R.drawable.chat_bubble_left);
                holder.textViewMessage.setText(comments.get(position).message);
                holder.textViewMessage.setTextSize(24);
                holder.linearLayoutItemView.setGravity(Gravity.START);
            }

            long unixTime = (long) comments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String timestamp = simpleDateFormat.format(date);
            holder.textViewTimestamp.setText(timestamp);
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }
    }
}
