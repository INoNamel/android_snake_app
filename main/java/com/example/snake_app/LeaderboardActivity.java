package com.example.snake_app;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class LeaderboardActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private LeaderboardAdapter mAdapter;
    private ArrayList<Score> mScores;

    private FirebaseFirestore db;
    private ListenerRegistration registration;

    private NetworkInfo activeNetworkInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        db = FirebaseFirestore.getInstance();
        Button back = findViewById(R.id.leaderboard_back);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        showLeaderboard();
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });
    }

    private void showLeaderboard() {
        loadLeaderboard();
        if(isNetworkAvailable()) {
            startLeaderboardListener();
        } else {
            Toast.makeText(this, "no internet connection", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager != null)
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void loadLeaderboard() {
        this.mScores = new ArrayList<>();
        mAdapter = new LeaderboardAdapter(mScores);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void startLeaderboardListener() {
        Query query = db.collection("leaderboard").orderBy("score", Query.Direction.DESCENDING).limit(10);
        registration = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot values, @Nullable FirebaseFirestoreException e) {
                mScores.clear();
                if (values != null)
                    for (DocumentSnapshot snap: values.getDocuments()) {
                        mScores.add(new Score(snap.getString("user"), Objects.requireNonNull(snap.getLong("score")).intValue()));
                    }
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void stopLeaderboardListener() {
        if(registration != null)
            registration.remove();
    }

    private void stop() {
        stopLeaderboardListener();
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stop();
    }
}
