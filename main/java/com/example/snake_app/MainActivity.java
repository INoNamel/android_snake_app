package com.example.snake_app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static FirebaseAuth mAuth;
    private Button signup, login, logout, startGame, leaderBoard;
    private LinearLayout loginLayout;
    private EditText password, email;
    private TextView userName;
    private ImageView userAvatar;
    private FirebaseAuth.AuthStateListener mAuthListener;
    FirebaseFirestore db;
    private static int highestScore;

    private NetworkInfo activeNetworkInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        password = findViewById(R.id.pwdText);
        email = findViewById(R.id.emailText);
        userName = findViewById(R.id.userName);
        userAvatar = findViewById(R.id.userAvatar);

        signup = findViewById(R.id.signup);
        login = findViewById(R.id.login);
        logout = findViewById(R.id.logout);
        startGame = findViewById(R.id.start_game);
        leaderBoard = findViewById(R.id.leaderBoard);
        loginLayout = findViewById(R.id.loginLayout);

        db = FirebaseFirestore.getInstance();
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { signUp(email.getText().toString(), password.getText().toString()); }
        });
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { signIn(email.getText().toString(), password.getText().toString()); }
        });
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSignOut();
            }
        });
        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent SnakeActivity = new Intent(MainActivity.this, GameActivity.class);
                startActivityForResult(SnakeActivity, 1);
            }
        });
        leaderBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent leaderBoard = new Intent(MainActivity.this, LeaderboardActivity.class);
                startActivity(leaderBoard);
            }
        });

        run();
    }

    private void run() {
        signOut();
        startAuthListener();
        if(!isNetworkAvailable())
            Toast.makeText(this, "no internet connection", Toast.LENGTH_SHORT).show();
    }

    private void startAuthListener() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    updateUI(user);
                } else {
                    updateUI(null);
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        signOut();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                int newScore = data.getIntExtra("score", DEFAULT_KEYS_DISABLE);
                Toast.makeText(this, "score :"+ newScore, Toast.LENGTH_SHORT).show();
                updateScore(newScore);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "No data sent :(", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateScore(int newScore) {
        if(mAuth.getCurrentUser() != null && newScore > getUserScore())
            db.collection("leaderboard").document(mAuth.getCurrentUser().getUid()).update("score", newScore);
    }

    private void updateUI(@Nullable FirebaseUser currentUser) {
        if(currentUser == null) {
            loginLayout.setVisibility(View.VISIBLE);
            signup.setVisibility(View.VISIBLE);
            login.setVisibility(View.VISIBLE);
            logout.setVisibility(View.GONE);
            userAvatar.setVisibility(View.GONE);
            userName.setText(null);
        } else {
            loginLayout.setVisibility(View.GONE);
            signup.setVisibility(View.GONE);
            login.setVisibility(View.GONE);
            logout.setVisibility(View.VISIBLE);
            userName.setText(currentUser.getEmail());
            userAvatar.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager != null)
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void signIn(String email, String password) {
        if (validateForm())
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    updateUI(mAuth.getCurrentUser());
                    getUserScore();
                    Toast.makeText(MainActivity.this, "Successfully logged in", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
                }
            });

    }

    private void signUp(String email, String password) {
        if (validateForm())
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    updateUI(mAuth.getCurrentUser());
                    completeSignUp();
                    getUserScore();
                    Toast.makeText(MainActivity.this, "New user created", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                }
                }
            });

    }

    private void completeSignUp() {
        if(mAuth.getCurrentUser() != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("score", 0);
            data.put("user", mAuth.getCurrentUser().getEmail());
            db.collection("leaderboard").document(mAuth.getCurrentUser().getUid()).set(data);
        }
    }

    private void signOut() {
        mAuth.signOut();
    }

    private void performSignOut() {
        if(mAuth.getCurrentUser() != null) {
            signOut();
            updateUI(null);
            Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Log in first", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateForm() {
        String emailInput = email.getEditableText().toString();
        String pwdInput = password.getEditableText().toString();
        if (emailInput.isEmpty()) {
            email.setError("empty");
            return false;
        } else if (pwdInput.isEmpty()) {
            password.setError("empty");
            return false;
        } else if (pwdInput.length() < 6) {
            password.setError("at least 6 chars");
            return false;
        } else {
            email.setError(null);
            return true;
        }
    }

    private int getUserScore() {
        if(mAuth.getCurrentUser() != null) {
            db.collection("leaderboard").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    highestScore = Objects.requireNonNull(documentSnapshot.getLong("score")).intValue();
                }
            });
        }
        return highestScore;
    }

}
