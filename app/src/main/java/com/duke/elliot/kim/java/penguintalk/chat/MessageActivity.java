package com.duke.elliot.kim.java.penguintalk.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Predicate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.textclassifier.TextLinks;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.duke.elliot.kim.java.penguintalk.LinearLayoutManagerWrapper;
import com.duke.elliot.kim.java.penguintalk.R;
import com.duke.elliot.kim.java.penguintalk.model.ChatModel;
import com.duke.elliot.kim.java.penguintalk.model.NotificationModel;
import com.duke.elliot.kim.java.penguintalk.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MessageActivity extends AppCompatActivity {

    private Button buttonSend;
    private EditText editTextMessage;
    private RecyclerView recyclerViewMessage;
    private String otherUid;
    private String uid;
    private String chatRoomId;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault());
    private UserModel otherUser;
    private DatabaseReference databaseReference;
    private ChildEventListener childEventListener;

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
                            .addOnSuccessListener(aVoid -> checkChatRoom());
                } else {
                    if (!editTextMessage.getText().toString().equals("")) {
                        ChatModel.Comment comment = new ChatModel.Comment();
                        comment.message = editTextMessage.getText().toString();
                        comment.uid = uid;
                        comment.timestamp = ServerValue.TIMESTAMP;
                        comment.readUsers.put(uid, true);
                        FirebaseDatabase.getInstance().getReference()
                                .child("chat_rooms").child(chatRoomId).child("comments")
                                .push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                sendCloudMessage();
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
        databaseReference.removeEventListener(childEventListener);
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

                    if (chat == null)
                        return;

                    if (chat.users.containsKey(otherUid)) {
                        chatRoomId = dataSnapshot.getKey();
                        buttonSend.setEnabled(true);
                        recyclerViewMessage.setLayoutManager(new LinearLayoutManagerWrapper(MessageActivity.this));
                        recyclerViewMessage.setAdapter(new MessageRecyclerViewAdapter());

                        if (!editTextMessage.getText().toString().equals("")) {
                            ChatModel.Comment comment = new ChatModel.Comment();
                            comment.message = editTextMessage.getText().toString();
                            comment.uid = uid;
                            comment.timestamp = ServerValue.TIMESTAMP;
                            comment.readUsers.put(uid, true);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chat_rooms").child(chatRoomId).child("comments")
                                    .push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    editTextMessage.setText("");
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    void sendCloudMessage() {
        Gson gson = new Gson();
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        NotificationModel notification = new NotificationModel();
        notification.to = otherUser.pushToken;
        notification.notification.title = userName;
        notification.notification.text = editTextMessage.getText().toString();

        notification.data.title = userName;
        notification.data.text = editTextMessage.getText().toString();

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf8"), gson.toJson(notification));
        Request request = new Request.Builder().header("Content-Type", "application/json")
                .addHeader("Authorization","key=AAAAqN4dTHU:APA91bGYSOt-gxSnnkWLGdEsrqli0kGqaQgUnDLftKWjAFOD1VZdz0UTHw4Y7Nv07_Eaj1y5IWMzMm5eVQNjqc8qZVxeYtiakVLNllyamPmgxS-2kmRfhPXe8YbpGZkBmQ5MpttKlKQg")
                .url("https://fcm.googleapis.com/fcm/send")
                .post(requestBody)
                .build();

        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }
        });

    }

    class MessageRecyclerViewAdapter extends RecyclerView.Adapter<MessageRecyclerViewAdapter.ViewHolder> {

        List<ChatModel.Comment> comments;

        MessageRecyclerViewAdapter() {
            comments = new ArrayList<>();

            FirebaseDatabase.getInstance().getReference()
                    .child("users").child(otherUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    otherUser = snapshot.getValue(UserModel.class);
                    getMessageList();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });


        }

        private void getMessageList() {
            databaseReference = FirebaseDatabase.getInstance().getReference()
                    .child("chat_rooms").child(chatRoomId).child("comments");

            childEventListener = databaseReference.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    String key = snapshot.getKey();
                    Map<String, Object> readUsersMap = new HashMap<>();
                    ChatModel.Comment comment = snapshot.getValue(ChatModel.Comment.class);
                    Log.d("THISBEFOREPUT", comment.readUsers.keySet().toString());
                    comment.readUsers.put(uid, true);
                    Log.d("THISTHISWHY", comment.readUsers.keySet().toString());
                    Log.d("THISTHISWHYHI", comment.message);
                    Log.d("MYID", uid);
                    readUsersMap.put(key, comment);
                    comments.add(comment);

                    FirebaseDatabase.getInstance().getReference()
                            .child("chat_rooms").child(chatRoomId).child("comments")
                            .updateChildren(readUsersMap)
                            .addOnCompleteListener(task -> {
                                notifyItemInserted(comments.size() - 1);
                                recyclerViewMessage.scrollToPosition(comments.size() - 1);
                            });

                    /* 이 조건을, 기존과 같으면~ 으로 할 것.
                    if (!comments.get(comments.size() - 1).readUsers.containsKey(uid)) {

                    } else {
                        notifyItemInserted(comments.size() - 1);
                        recyclerViewMessage.scrollToPosition(comments.size() - 1);
                    }

                     */
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    ChatModel.Comment comment = snapshot.getValue(ChatModel.Comment.class);
                    comments.set(comments.size() - 1, comment);
                    notifyItemChanged(comments.size() - 1);
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
            TextView textViewReadCountEnd;
            TextView textViewReadCountStart;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewMessage = itemView.findViewById(R.id.text_view_message);
                textViewName = itemView.findViewById(R.id.text_view_name);
                imageViewProfile = itemView.findViewById(R.id.image_view_profile);
                linearLayoutMessage = itemView.findViewById(R.id.linear_layout_message);
                linearLayoutItemView = itemView.findViewById(R.id.linear_layout_item_view);
                textViewTimestamp = itemView.findViewById(R.id.text_view_timestamp);
                textViewReadCountEnd = itemView.findViewById(R.id.text_view_read_count_end);
                textViewReadCountStart = itemView.findViewById(R.id.text_view_read_count_start);
            }
        }

        @NonNull
        @Override
        public MessageRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_message, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageRecyclerViewAdapter.ViewHolder holder, int position) {
            if (comments.get(position).uid.equals(uid)) {
                holder.textViewMessage.setText(comments.get(position).message);
                holder.textViewMessage.setBackgroundResource(R.drawable.chat_bubble_right);
                holder.linearLayoutMessage.setVisibility(View.GONE);
                holder.textViewMessage.setTextSize(24);
                holder.linearLayoutItemView.setGravity(Gravity.END);
                setReadCount(position, holder.textViewReadCountStart);
            } else {
                Glide.with(holder.imageViewProfile.getContext())
                        .load(otherUser.profilePictureUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(holder.imageViewProfile);
                holder.textViewName.setText(otherUser.name);
                holder.linearLayoutMessage.setVisibility(View.VISIBLE);
                holder.textViewMessage.setBackgroundResource(R.drawable.chat_bubble_left);
                holder.textViewMessage.setText(comments.get(position).message);
                holder.textViewMessage.setTextSize(24);
                holder.linearLayoutItemView.setGravity(Gravity.START);
                setReadCount(position, holder.textViewReadCountEnd);
            }

            long unixTime = (long) comments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String timestamp = simpleDateFormat.format(date);
            holder.textViewTimestamp.setText(timestamp);
        }

        void setReadCount(final int position, final TextView textView) {
            FirebaseDatabase.getInstance().getReference()
                    .child("chat_rooms").child(chatRoomId).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Map<String, Boolean> users = (Map<String, Boolean>) snapshot.getValue();
                    int count = users.size() - comments.get(position).readUsers.size();

                    if (count > 0) {
                        textView.setVisibility(View.VISIBLE);
                        textView.setText(String.valueOf(count));
                    } else {
                        textView.setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }
    }
}

