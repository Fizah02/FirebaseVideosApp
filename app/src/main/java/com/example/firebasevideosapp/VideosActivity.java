package com.example.firebasevideosapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class VideosActivity extends AppCompatActivity {

    //UI views
    FloatingActionButton addVideosBtn;
    private RecyclerView videosRv;

    //array list
    private ArrayList<ModelVideo> videoArrayList;
    //adapter
    private AdapterVideo adapterVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videos);

        //change actionbar title
        setTitle("Videos");

        //init UI Views
        addVideosBtn = findViewById(R.id.addVideosBtn);
        videosRv = findViewById(R.id.videosRv);
        
        //function call,load videos
        loadVideosFromFirebase();

        //handle click
        addVideosBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //start activity to add videos
                startActivity(new Intent(VideosActivity.this,AddVideoActivity.class));
            }
        });
    }

    private void loadVideosFromFirebase() {
        //init array list before adding data into it
        videoArrayList = new ArrayList<>();

        //db reference
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Videos");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //clear list before adding data into it
                videoArrayList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    //get data
                    ModelVideo modelVideo = ds.getValue(ModelVideo.class);
                    //add model/data into list
                    videoArrayList.add(modelVideo);
                }
                //setup adapter
                adapterVideo = new AdapterVideo(VideosActivity.this,videoArrayList);
                //set adapter to recyclerview
                videosRv.setAdapter(adapterVideo);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}