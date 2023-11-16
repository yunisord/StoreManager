package com.example.storemanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class Store extends AppCompatActivity {
    BottomNavigationView Navbar;
    private final ImageView[] phArray = new ImageView[7];
    private ActivityResultLauncher<Intent> galleryLauncher;
    private final Button[] addButtons = new Button[7];
    private final Button[] editButtons = new Button[7];
    private final Button[] removeButtons = new Button[7];
    private final EditText[] ProductDescription = new EditText[7];
    private final EditText[] Product = new EditText[7];
    private final ImageView[] addDrawables = new ImageView[7];
    private View clickedView;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private Uri downloadUri;
    private Uri selectedImageUri;

    private static final String PREFS_NAME = "ImageURLPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        db = FirebaseFirestore.getInstance();

        Navbar = findViewById(R.id.Navbar);

        Navbar.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.QR_Navbar) {
                startQRScanner();
                return true;
            }
            return false;
        });

        for (int i = 0; i < 7; i++) {
            int addButtonId = getResources().getIdentifier("add" + (i + 1), "id", getPackageName());
            addButtons[i] = findViewById(addButtonId);

            // Set a click listener for the "Add" buttons
            addButtons[i].setOnClickListener(v -> {
                clickedView = v;
                // Call the method to upload image to storage
                uploadImageToFirebaseStorage(selectedImageUri, (int) v.getTag());
            });

            // Set a tag for each "Add" button to identify it later
            addButtons[i].setTag(i);
        }

        for (int i = 0; i < 7; i++) {
            int editButtonId = getResources().getIdentifier("edit" + (i + 1), "id", getPackageName());
            editButtons[i] = findViewById(editButtonId);

            // Set a click listener for the "Edit" buttons
            editButtons[i].setOnClickListener(v -> {
                clickedView = v;
                openGallery();
            });

            // Set a tag for each "Edit" button to identify it later
            editButtons[i].setTag(i);
        }


        for (int i = 0; i < 7; i++) {
            int imageViewId = getResources().getIdentifier("ph" + (i + 1), "id", getPackageName());
            phArray[i] = findViewById(imageViewId);
        }

        for (int i = 0; i < 7; i++) {
            int removeButtonId = getResources().getIdentifier("remove" + (i + 1), "id", getPackageName());
            removeButtons[i] = findViewById(removeButtonId);

            removeButtons[i].setTag(i);

            removeButtons[i].setOnClickListener(v -> {
                int removeIndex = (int) v.getTag();
                if (removeIndex >= 0 && removeIndex < phArray.length) {
                    phArray[removeIndex].setImageDrawable(null);
                    addButtons[removeIndex].setVisibility(View.VISIBLE);
                    editButtons[removeIndex].setVisibility(View.GONE);
                    removeButtons[removeIndex].setVisibility(View.GONE);
                    addDrawables[removeIndex].setVisibility(View.VISIBLE);

                    // Clear the saved image URL
                    saveImageUrlLocally(removeIndex, null);
                }
            });

            int addDrawableId = getResources().getIdentifier("add_drawable" + (i + 1), "id", getPackageName());
            addDrawables[i] = findViewById(addDrawableId);
        }

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                if (selectedImageUri != null && clickedView != null) {
                    int clickedIndex = (int) clickedView.getTag();
                    phArray[clickedIndex].setImageURI(selectedImageUri);
                    clickedView.setVisibility(View.GONE);
                    editButtons[clickedIndex].setVisibility(View.VISIBLE);
                    removeButtons[clickedIndex].setVisibility(View.VISIBLE);
                    phArray[clickedIndex].setVisibility(View.VISIBLE);
                    addDrawables[clickedIndex].setVisibility(View.GONE);

                    uploadImageToFirebaseStorage(selectedImageUri, clickedIndex);
                }
            }
        });

        // Load images from storage and display them
        loadImages();
    }

    private void uploadImageToFirebaseStorage(Uri imageUri, int clickedIndex) {
        String subfolderName = "product" + (clickedIndex + 1);
        String imageName = "image.jpg"; // Use a consistent image name

        StorageReference imageRef = storageRef.child("Products/" + subfolderName + "/" + imageName);

        imageRef.putFile(imageUri)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        imageRef.getDownloadUrl().addOnCompleteListener(downloadTask -> {
                            if (downloadTask.isSuccessful()) {
                                downloadUri = downloadTask.getResult();

                                // Save the download URL locally
                                saveImageUrlLocally(clickedIndex, downloadUri.toString());
                            }
                        });
                    } else {
                        Log.e("StoreActivity", "Error uploading image to Firebase Storage: " + task.getException());
                    }
                });
    }

    private void saveImageUrlLocally(int clickedIndex, String imageUrl) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("image_url_" + clickedIndex, imageUrl);
        editor.apply();
    }

    private void loadImages() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        for (int i = 0; i < phArray.length; i++) {
            String imageUrl = preferences.getString("image_url_" + i, null);

            if (imageUrl != null) {
                // Load the image using the URL
                // You can use a library like Glide or Picasso for efficient image loading
                Glide.with(this)
                        .load(imageUrl)
                        .into(phArray[i]);
            }
        }
    }

    private void openGallery() {
        int clickedIndex = (int) clickedView.getTag();
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void startQRScanner() {
        Intent intent = new Intent(Store.this, QR_Scanner.class);
        startActivity(intent);
    }
}

