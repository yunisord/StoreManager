package com.example.storemanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    EditText inputemail, inputpass;
    Button li;
    FirebaseAuth mAuth;
    ProgressBar progressbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        mAuth = FirebaseAuth.getInstance();

        inputemail = findViewById(R.id.inputemail);
        inputpass = findViewById(R.id.inputpass);
        li = findViewById(R.id.li);
        progressbar = findViewById(R.id.progressbar); // Initialize the progressbar

        li.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressbar.setVisibility(View.VISIBLE);
                String inputEmail = inputemail.getText().toString();
                String inputPass = inputpass.getText().toString();

                if (TextUtils.isEmpty(inputEmail)) {
                    Toast.makeText(Login.this, "Enter email", Toast.LENGTH_SHORT).show();
                    progressbar.setVisibility(View.GONE);
                    return;
                }
                if (TextUtils.isEmpty(inputPass)) {
                    Toast.makeText(Login.this, "Enter password", Toast.LENGTH_SHORT).show();
                    progressbar.setVisibility(View.GONE);
                    return;
                }

                // Authenticate with Firebase using email and password
                mAuth.signInWithEmailAndPassword(inputEmail, inputPass)
                        .addOnCompleteListener(task -> {
                            progressbar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    // Check if the logged-in user is the admin
                                    if (isAdminUser(user)) {
                                        // Admin is authenticated
                                        Toast.makeText(getApplicationContext(), "Admin Login Successful", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(getApplicationContext(), Store.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        // Non-admin user, show an error
                                        Toast.makeText(Login.this, "Authentication failed. You are not an admin user.", Toast.LENGTH_SHORT).show();
                                        mAuth.signOut(); // Sign out the non-admin user
                                    }
                                }
                            } else {
                                Toast.makeText(Login.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
        private boolean isAdminUser(FirebaseUser user) {
            // Replace with the email of your predetermined admin account
            String adminEmail = "historiaya.acc@gmail.com";
            return adminEmail.equals(user.getEmail());
        }
}
