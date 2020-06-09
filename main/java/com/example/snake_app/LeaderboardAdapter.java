package com.example.snake_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {
    private ArrayList<Score> scores;

    public LeaderboardAdapter(ArrayList<Score> scores) {
        this.scores = scores;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.score_item, parent, false);
        return new LeaderboardViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        Score score = getScore(position);

        holder.textViewPlace.setText(String.valueOf(position+1));
        holder.textViewUser.setText(score.getUser());
        holder.textViewScore.setText(String.valueOf(score.getScore()));
    }

    @Override
    public int getItemCount() {
        return scores.size();
    }

    private Score getScore(int position) {
        return scores.get(position);
    }

    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUser, textViewScore, textViewPlace;

        private LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewPlace = itemView.findViewById(R.id.scorePlace);
            textViewUser = itemView.findViewById(R.id.scoreName);
            textViewScore = itemView.findViewById(R.id.scoreValue);
        }
    }
}
