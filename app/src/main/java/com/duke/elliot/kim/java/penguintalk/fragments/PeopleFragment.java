package com.duke.elliot.kim.java.penguintalk.fragments;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.duke.elliot.kim.java.penguintalk.SelectFriendsActivity;
import com.duke.elliot.kim.java.penguintalk.chat.MessageActivity;
import com.duke.elliot.kim.java.penguintalk.model.UserModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class PeopleFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_people);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        recyclerView.setAdapter(new PeopleRecyclerViewAdapter());

        FloatingActionButton floatingActionButton = view.findViewById(R.id.floating_action_button);
        floatingActionButton.setOnClickListener( _view -> {
            startActivity(new Intent(view.getContext(), SelectFriendsActivity.class));
        });

        return view;
    }

    class PeopleRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        List<UserModel> users;

        PeopleRecyclerViewAdapter() {
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

            ViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.image_view_profile);
                textViewId = view.findViewById(R.id.text_view_id);
                textViewComment = view.findViewById(R.id.text_view_comment);
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_friend, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            Glide.with(holder.itemView.getContext())
                    .load(users.get(position).profileImageUrl)
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
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }
}
