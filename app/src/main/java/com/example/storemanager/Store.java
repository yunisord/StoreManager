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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class Store extends AppCompatActivity {
    private final ImageView[] phArray = new ImageView[7];
    private final Button[] editButtons = new Button[7];
    private final Button[] addButtons = new Button[7];
    private final ImageView[] addDrawables = new ImageView[7];
    private final EditText[] Product = new EditText[7];
    private final EditText[] ProductDescription = new EditText[7];
    private View clickedView;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference productsCollection = db.collection("Products");
    private Uri downloadUri;
    private Uri selectedImageUri;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef = storage.getReference();

    // Declare preferences variable
    private SharedPreferences preferences;

    private static final String PREFS_NAME = "ImageURLPrefs";
    private static final String PRODUCT_PREFS_NAME = "ProductPrefs";
    private static final String PRODUCT_KEY_PREFIX = "product_";
    private static final String PRODUCT_DESCRIPTION_KEY_PREFIX = "product_description_";

    private int clickedIndex;
    private BottomNavigationView Navbar;


    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null && clickedView != null) {
                        int clickedIndex = (int) clickedView.getTag();
                        phArray[clickedIndex].setImageURI(selectedImageUri);
                        clickedView.setVisibility(View.GONE);
                        editButtons[clickedIndex].setVisibility(View.VISIBLE);
                        phArray[clickedIndex].setVisibility(View.VISIBLE);
                        addDrawables[clickedIndex].setVisibility(View.GONE);

                        uploadImageToFirebaseStorage(selectedImageUri, clickedIndex);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        db = FirebaseFirestore.getInstance();

        // Initialize preferences
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

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
            int editButtonId = getResources().getIdentifier("edit" + (i + 1), "id", getPackageName());
            editButtons[i] = findViewById(editButtonId);

            // Set a click listener for the "Edit" buttons
            editButtons[i].setOnClickListener(v -> {
                clickedView = v;
                openGallery();
            });
            editButtons[i].setTag(i);
        }

        for (int i = 0; i < 7; i++) {
            int addButtonId = getResources().getIdentifier("add" + (i + 1), "id", getPackageName());
            addButtons[i] = findViewById(addButtonId);

            addButtons[i].setOnClickListener(v -> {
                int clickedIndex = (int) v.getTag();
                String imageUrl = preferences.getString("image_url_" + clickedIndex, null);
                String productName = Product[clickedIndex].getText().toString();
                String productDescription = ProductDescription[clickedIndex].getText().toString();

                if (imageUrl != null && !productName.isEmpty() && !productDescription.isEmpty()) {
                    String documentName = "Product" + (clickedIndex + 1); // Adjust the document name as needed
                    addProductToFirestore(documentName, imageUrl, productName, productDescription);
                } else {
                    showToast("Please provide all required information.");
                }
            });
            addButtons[i].setTag(i);
        }


        for (int i = 0; i < 7; i++) {
            int imageViewId = getResources().getIdentifier("ph" + (i + 1), "id", getPackageName());
            phArray[i] = findViewById(imageViewId);
        }

        for (int i = 0; i < 7; i++) {
            int addDrawableId = getResources().getIdentifier("add_drawable" + (i + 1), "id", getPackageName());
            addDrawables[i] = findViewById(addDrawableId);
        }
        for (int i = 0; i < 7; i++) {
            int productId = getResources().getIdentifier("Product" + (i + 1), "id", getPackageName());
            int productDescriptionId = getResources().getIdentifier("ProductDescription" + (i + 1), "id", getPackageName());

            Product[i] = findViewById(productId);
            ProductDescription[i] = findViewById(productDescriptionId);
        }

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

    private void addProductToFirestore(String documentName, String imageUrl, String productName, String productDescription) {
        Map<String, Object> productData = new HashMap<>();
        productData.put("image_url", imageUrl);
        productData.put("product_name", productName);
        productData.put("product_description", productDescription);

        productsCollection.document(documentName)
                .set(productData)
                .addOnSuccessListener(aVoid -> {
                    showToast("Product added to Firestore successfully!");
                    saveProductLocally(clickedIndex, productName);
                    saveProductDescriptionLocally(clickedIndex, productDescription);
                })
                .addOnFailureListener(e -> {
                    showToast("Error adding product to Firestore: " + e.getMessage());
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
                addDrawables[i].setVisibility(View.GONE);

                // Fetch and display product and product description from Firestore
                fetchProductDataFromFirestore(i);
            }
        }
    }

    private void fetchProductDataFromFirestore(int index) {
        String documentName = "Product" + (index + 1); // Adjust the document name as needed

        // Fetch data from Firestore
        productsCollection.document(documentName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String productName = documentSnapshot.getString("product_name");
                        String productDescription = documentSnapshot.getString("product_description");

                        // Update UI with fetched data
                        if (productName != null) {
                            Product[index].setText(productName);
                        }

                        if (productDescription != null) {
                            ProductDescription[index].setText(productDescription);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Error fetching product data from Firestore: " + e.getMessage());
                });
    }
    private void saveProductLocally(int clickedIndex, String productName) {
        SharedPreferences productPrefs = getSharedPreferences(PRODUCT_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = productPrefs.edit();
        editor.putString(PRODUCT_KEY_PREFIX + clickedIndex, productName);
        editor.apply();
    }

    private void saveProductDescriptionLocally(int clickedIndex, String productDescription) {
        SharedPreferences productPrefs = getSharedPreferences(PRODUCT_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = productPrefs.edit();
        editor.putString(PRODUCT_DESCRIPTION_KEY_PREFIX + clickedIndex, productDescription);
        editor.apply();
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}


