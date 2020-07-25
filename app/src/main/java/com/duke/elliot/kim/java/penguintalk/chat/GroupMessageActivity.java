package com.duke.elliot.kim.java.penguintalk.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Layout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.duke.elliot.kim.java.penguintalk.LinearLayoutManagerWrapper;
import com.duke.elliot.kim.java.penguintalk.R;
import com.duke.elliot.kim.java.penguintalk.model.ChatModel;
import com.duke.elliot.kim.java.penguintalk.model.NotificationModel;
import com.duke.elliot.kim.java.penguintalk.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
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

public class GroupMessageActivity extends AppCompatActivity {

    Map<String, UserModel> users = new HashMap<>();
    String destinationRoom;
    String uid;
    EditText editText;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault());
    private DatabaseReference databaseReference;
    private ChildEventListener childEventListener;

    private RecyclerView recyclerView;

    List<ChatModel.Comment> comments = new ArrayList<>();

    int peopleCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_message);
        destinationRoom = getIntent().getStringExtra("destinationRoom");
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        editText = findViewById(R.id.edit_text_message);

        FirebaseDatabase.getInstance().getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot item : snapshot.getChildren()) {
                    users.put(item.getKey(), item.getValue(UserModel.class));
                }
                init();
                recyclerView = findViewById(R.id.recycler_view_group_chat);
                recyclerView.setLayoutManager(new LinearLayoutManagerWrapper(GroupMessageActivity.this));
                recyclerView.setAdapter(new GroupMessageRecyclerViewAdapter());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    void init() {
        Button button = findViewById(R.id.button_send);
        button.setOnClickListener( view -> {
            ChatModel.Comment comment = new ChatModel.Comment();
            comment.uid = uid;
            comment.message = editText.getText().toString();
            comment.timestamp = ServerValue.TIMESTAMP;
            FirebaseDatabase.getInstance().getReference()
                    .child("chat_rooms").child(destinationRoom)
                    .push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {

                        FirebaseDatabase.getInstance().getReference()
                                .child("chat_rooms").child(destinationRoom).child("users")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        Map<String, Boolean> map = (Map<String, Boolean>) snapshot.getValue();

                                        for (String item : map.keySet()) {
                                            if (item.equals(uid)) {
                                                continue;
                                            }
                                            sendCloudMessage(users.get(item).pushToken);
                                        }
                                        editText.setText("");
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                    }
                }
            });
        });
    }

    void sendCloudMessage(String pushToken) {
        Gson gson = new Gson();
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        NotificationModel notification = new NotificationModel();
        notification.to = pushToken;
        notification.notification.title = userName;
        notification.notification.text = editText.getText().toString();

        notification.data.title = userName;
        notification.data.text = editText.getText().toString();

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

    void setReadCount(final int position, final TextView textView) {
        if (peopleCount == 0) {
            FirebaseDatabase.getInstance().getReference()
                    .child("chat_rooms").child(destinationRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Map<String, Boolean> users = (Map<String, Boolean>) snapshot.getValue();
                    peopleCount = users.size();
                    int count = peopleCount - comments.get(position).readUsers.size();

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
        } else {
            int count = peopleCount - comments.get(position).readUsers.size();

            if (count > 0) {
                textView.setVisibility(View.VISIBLE);
                textView.setText(String.valueOf(count));
            } else {
                textView.setVisibility(View.INVISIBLE);
            }
        }
    }

    class GroupMessageRecyclerViewAdapter extends RecyclerView.Adapter<GroupMessageRecyclerViewAdapter.GroupMessageViewHolder> {

        public GroupMessageRecyclerViewAdapter() {
            getMessageList();
        }

        private void getMessageList() {
            databaseReference = FirebaseDatabase.getInstance().getReference()
                    .child("chat_rooms").child(destinationRoom).child("comments");

            childEventListener = databaseReference.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    String key = snapshot.getKey();
                    Map<String, Object> readUsersMap = new HashMap<>();
                    ChatModel.Comment comment = snapshot.getValue(ChatModel.Comment.class);

                    comment.readUsers.put(uid, true);
                    readUsersMap.put(key, comment);
                    comments.add(comment);

                    FirebaseDatabase.getInstance().getReference()
                            .child("chat_rooms").child(destinationRoom).child("comments")
                            .updateChildren(readUsersMap)
                            .addOnCompleteListener(task -> {
                                notifyItemInserted(comments.size() - 1);
                                recyclerView.scrollToPosition(comments.size() - 1);
                            });
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

        @NonNull
        @Override
        public GroupMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_message, parent, false);
            return new GroupMessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GroupMessageViewHolder holder, int position) {
            if (comments.get(position).uid.equals(uid)) {
                holder.textViewMessage.setText(comments.get(position).message);
                holder.textViewMessage.setBackgroundResource(R.drawable.chat_bubble_right);
                holder.linearLayoutMessage.setVisibility(View.GONE);
                holder.textViewMessage.setTextSize(24);
                holder.linearLayoutItemView.setGravity(Gravity.END);
                setReadCount(position, holder.textViewReadCountStart);
            } else {
                Glide.with(holder.imageViewProfile.getContext())
                        .load(users.get(comments.get(position).uid).profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(holder.imageViewProfile);
                holder.textViewName.setText(users.get(comments.get(position).uid).name);
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

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class GroupMessageViewHolder extends RecyclerView.ViewHolder {

            TextView textViewMessage;
            TextView textViewName;
            ImageView imageViewProfile;
            LinearLayout linearLayoutMessage;
            LinearLayout linearLayoutItemView;
            TextView textViewTimestamp;
            TextView textViewReadCountEnd;
            TextView textViewReadCountStart;

            public GroupMessageViewHolder(View view) {
                super(view);
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
    }
}
