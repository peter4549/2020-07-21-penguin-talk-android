package com.duke.elliot.kim.java.penguintalk.fragments;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.duke.elliot.kim.java.penguintalk.R;
import com.duke.elliot.kim.java.penguintalk.chat.GroupMessageActivity;
import com.duke.elliot.kim.java.penguintalk.chat.MessageActivity;
import com.duke.elliot.kim.java.penguintalk.model.ChatModel;
import com.duke.elliot.kim.java.penguintalk.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public class ChatFragment extends Fragment {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd. HH:mm", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_chat);
        recyclerView.setAdapter(new ChatRecyclerViewAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));

        return view;
    }

    class ChatRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<ChatModel> chatList = new ArrayList<>();
        private List<String> keys = new ArrayList<>();
        private String uid;
        private ArrayList<String> otherUsers = new ArrayList<>();

        ChatRecyclerViewAdapter() {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseDatabase.getInstance().getReference()
                    .child("chat_rooms").orderByChild("users/" + uid).equalTo(true)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    chatList.clear();
                    for (DataSnapshot item: snapshot.getChildren()) {
                        chatList.add(item.getValue(ChatModel.class));
                        keys.add(item.getKey());
                    }

                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_chat, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
            String otherUid = null;

            final ViewHolder viewHolder = (ViewHolder) holder;

            for (String user: chatList.get(position).users.keySet()) {
                if(!user.equals(uid)) {
                    otherUid = user;
                    otherUsers.add(otherUid);
                }
            }

            FirebaseDatabase.getInstance().getReference().child("users").child(otherUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    UserModel user = snapshot.getValue(UserModel.class);
                    Glide.with(viewHolder.itemView.getContext())
                            .load(user.profileImageUrl)
                            .apply(new RequestOptions().circleCrop())
                            .into(viewHolder.imageView);

                    viewHolder.textViewTitle.setText(user.name);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            Map<String, ChatModel.Comment> map = new TreeMap<>(Collections.reverseOrder());
            map.putAll(chatList.get(position).comments);
            if (map.keySet().toArray().length > 0) {
                String lastMessageKey = (String) map.keySet().toArray()[0];
                viewHolder.textViewLastMessage.setText(chatList.get(position).comments.get(lastMessageKey).message);

                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                long unixTime = (long) chatList.get(position).comments.get(lastMessageKey).timestamp;
                Date date = new Date(unixTime);
                viewHolder.textViewTimestamp.setText(simpleDateFormat.format(date));
            }

            viewHolder.imageView.setOnClickListener(view -> {
                Intent intent = null;

                if (chatList.get(position).users.size() > 2) {
                    intent = new Intent(view.getContext(), GroupMessageActivity.class);
                    intent.putExtra("destinationRoom", keys.get(position));
                } else {
                    intent = new Intent(view.getContext(), MessageActivity.class);
                    intent.putExtra("otherUid", otherUsers.get(position));
                }

                ActivityOptions activityOptions =
                        ActivityOptions.makeCustomAnimation(view.getContext(),
                                R.anim.anim_from_right, R.anim.anim_to_left);
                startActivity(intent, activityOptions.toBundle());
            });
        }

        @Override
        public int getItemCount() {
            return chatList.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView textViewTitle;
            TextView textViewLastMessage;
            TextView textViewTimestamp;

            ViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.image_view);
                textViewTitle = view.findViewById(R.id.text_view_title);
                textViewLastMessage = view.findViewById(R.id.text_view_last_message);
                textViewTimestamp = view.findViewById(R.id.text_view_timestamp);
            }
        }
    }
}
