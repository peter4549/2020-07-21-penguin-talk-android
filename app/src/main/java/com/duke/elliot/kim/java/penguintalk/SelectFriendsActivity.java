package com.duke.elliot.kim.java.penguintalk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.duke.elliot.kim.java.penguintalk.chat.MessageActivity;
import com.duke.elliot.kim.java.penguintalk.model.ChatModel;
import com.duke.elliot.kim.java.penguintalk.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class SelectFriendsActivity extends AppCompatActivity {

    ChatModel chatModel = new ChatModel();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friends);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_select_friends);
        recyclerView.setAdapter(new SelectFriendsRecyclerViewAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManagerWrapper(this));
        Button button = findViewById(R.id.button_create_chat_room);
        button.setOnClickListener(view -> {
            String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            chatModel.users.put(myUid, true);

            FirebaseDatabase.getInstance().getReference().child("chat_rooms").push().setValue(chatModel);
        });
    }

    class SelectFriendsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        List<UserModel> users;

        SelectFriendsRecyclerViewAdapter() {
            final String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            users = new ArrayList<>();
            FirebaseDatabase.getInstance().getReference()
                    .child("users").addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    UserModel user = snapshot.getValue(UserModel.class);

                    if (!user.uid.equals(currentUid)) {
                        users.add(0, user);
                        notifyItemInserted(0);
                    }
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
            ImageView imageView;
            TextView textViewId;
            TextView textViewComment;
            CheckBox checkBox;

            ViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.image_view_profile);
                textViewId = view.findViewById(R.id.text_view_id);
                textViewComment = view.findViewById(R.id.text_view_comment);
                checkBox = view.findViewById(R.id.check_box);
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_friend_select, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            Glide.with(holder.itemView.getContext())
                    .load(users.get(position).profilePictureUrl)
                    .apply(new RequestOptions().circleCrop())
                    .into(((ViewHolder)holder).imageView);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(view.getContext(), MessageActivity.class);
                    intent.putExtra("otherUid", users.get(position).uid);
                    ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(),
                            R.anim.anim_from_right, R.anim.anim_to_left);
                    startActivity(intent, activityOptions.toBundle());
                }
            });

            ((ViewHolder)holder).textViewId.setText(users.get(position).name);
            if (users.get(position).comment != null)
                ((ViewHolder)holder).textViewComment.setText(users.get(position).comment);

            ((ViewHolder) holder).checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        chatModel.users.put(users.get(position).uid, true);
                    } else {
                        chatModel.users.remove(users.get(position));
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }
}
