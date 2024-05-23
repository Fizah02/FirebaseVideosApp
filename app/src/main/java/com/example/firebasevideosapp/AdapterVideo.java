package com.example.firebasevideosapp;

import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.text.format.DateFormat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;

public class AdapterVideo extends RecyclerView.Adapter<AdapterVideo.HolderVideo> {

    //context
    private Context context;
    //array list
    private ArrayList<ModelVideo> videoArrayList;

    //constructor
    public AdapterVideo(Context context, ArrayList<ModelVideo> videoArrayList){
        this.context = context;
        this.videoArrayList = videoArrayList;
    }

    @NonNull
    @Override
    public HolderVideo onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout row_video.xml
        View view = LayoutInflater.from(context).inflate(R.layout.row_video,parent,false);
        return new HolderVideo(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderVideo holder, int position) {
        /*--Get, format, set data, handle clicks etc--*/

        //Get data
        ModelVideo modelVideo = videoArrayList.get(position);

        String id = modelVideo.getId();
        String title = modelVideo.getTitle();
        String timestamp = modelVideo.getTimestamp();
        String videoUrl = modelVideo.getVideoUrl();

        //format timestamp e.g. 07/09/2020 02:08 PM
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String formattedDateTime = DateFormat.format("dd/MM/yyyy K:mm a",calendar).toString();

        //set data
        holder.titleTv.setText(title);
        holder.timeTv.setText(formattedDateTime);
        setVideoUrl(modelVideo,holder);

        //handle click,download video
        holder.downloadFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadVideo(modelVideo);
            }
        });

        //handle click,delete video
        holder.deleteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show alert dialog,confirm to delete
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete")
                                .setMessage("Are you sure you want to delete video: "+title+"?")
                                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                //confirmed to delete
                                                deleteVideo(modelVideo);

                                            }
                                        })
                                                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        //cancelled deleting,dismiss dialog
                                                        dialogInterface.dismiss();

                                                    }
                                                })
                                                        .show();

            }
        });
    }

    private void setVideoUrl(ModelVideo modelVideo, HolderVideo holder) {
        //show progress
        holder.progressBar.setVisibility(View.VISIBLE);

        //get video url
        String videoUrl = modelVideo.getVideoUrl();

        //Media controller for play,pause,seekbar,timer etc
        MediaController mediaController = new MediaController(context);
        mediaController.setAnchorView(holder.videoView);

        Uri videoUri = Uri.parse(videoUrl);
        holder.videoView.setMediaController(mediaController);
        holder.videoView.setVideoURI(videoUri);

        holder.videoView.requestFocus();
        holder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                //video is ready to play
                mediaPlayer.start();
            }
        });

        holder.videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                //to check if buffering,rendering etc
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START: {
                        //rendering started
                        holder.progressBar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START: {
                        //buffering started
                        holder.progressBar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:{
                        //buffering ended
                        holder.progressBar.setVisibility(View.GONE);
                        return true;
                    }
                }
                return false;
            }
        });

        holder.videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.start(); //restart video if completed
            }
        });
    }

    private void deleteVideo(ModelVideo modelVideo) {
        String videoId = modelVideo.getId(); //will be used to delete from firebase database
        String videoUrl = modelVideo.getVideoUrl(); //will be used to delete from firebase storage

        //1) Delete from firebase storage
        StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl);
        reference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //deleted from firebase storage

                        //2) Delete from firebase database
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Videos");
                        databaseReference.child(videoId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        //deleted from firebase database
                                        Toast.makeText(context, "Video deleted successfully...", Toast.LENGTH_SHORT).show();

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //failed deleting from firebase database
                                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed deleting from firebase storage,show error message
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void downloadVideo(ModelVideo modelVideo) {
        String videoUrl = modelVideo.getVideoUrl(); //url of video,will be used to download video

        //get video reference using video url
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl);
        storageReference.getMetadata()
                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        //get file/video basic info e.g. title,type
                        String fileName = storageMetadata.getName(); //file name in firebase storage e.g. video/mp4
                        String fileType = storageMetadata.getContentType(); //file type in firebase storage
                        String fileDirectory = Environment.DIRECTORY_DOWNLOADS; //video will be saved in this folder i.e. Downloads

                        //init DownloadManager
                        DownloadManager downloadManager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);

                        //get uri of file to be downloaded
                        Uri uri = Uri.parse(videoUrl);

                        //create download request,new request for each download - yes we can download multiple files at once
                        DownloadManager.Request request = new DownloadManager.Request(uri);

                        //notification visibility
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        //set destination path
                        request.setDestinationInExternalPublicDir(""+fileDirectory,""+fileName+".mp4");

                        //add request to queue - can be multiple requests so is adding to queue
                        downloadManager.enqueue(request);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed getting info
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }

    @Override
    public int getItemCount() {
        return videoArrayList.size(); //return size of list
    }

    //View holder class,holds,inits the UI views
    class HolderVideo extends RecyclerView.ViewHolder{

        //UI Views of row_video.xml
        VideoView videoView;
        TextView titleTv,timeTv;
        ProgressBar progressBar;
        FloatingActionButton deleteFab,downloadFab;

        public HolderVideo(@NonNull View itemView){
            super(itemView);

            //init UI Views of row_video.xml
            videoView = itemView.findViewById(R.id.videoView);
            titleTv = itemView.findViewById(R.id.titleTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            progressBar = itemView.findViewById(R.id.progressBar);
            deleteFab = itemView.findViewById(R.id.deleteFab);
            downloadFab = itemView.findViewById(R.id.downloadFab);

        }
    }
}
