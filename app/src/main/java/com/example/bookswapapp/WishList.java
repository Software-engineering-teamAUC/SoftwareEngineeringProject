package com.example.bookswapapp;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;

public class WishList extends AppCompatActivity {
    String username;
    ListView listView;
    ArrayAdapter<String> adapter;
    FirebaseFirestore db;
    List<String> wishList;
    ImageView profile_pic;
    Button Add;
    EditText Add_ISBN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wish_list);
        profile_pic = findViewById(R.id.MPP);
        SessionManager sessionManager = SessionManager.getInstance();
        username = sessionManager.getUsername();
        listView = findViewById(R.id.WishListView);
        db = FirebaseFirestore.getInstance();
        Add = findViewById(R.id.Add);
        Add_ISBN = findViewById(R.id.Add_ISBN);
        displayProfilePic();
        populate_listview();
        Add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String isbn = Add_ISBN.getText().toString().trim();

                if (isValidISBN(isbn)) {
                    // Append the valid ISBN to the wish_list
                    appendISBNToWishList(isbn);
                } else {
                    Toast.makeText(WishList.this, "Invalid ISBN format", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    void populate_listview()
    {
        db.collection("users").document(username).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                wishList = (List<String>) document.get("wish_list");

                                if (wishList != null) {
                                    adapter = new ArrayAdapter<>(WishList.this, R.layout.wish_list_item, R.id.wish_list_textbox, wishList);
                                    listView.setAdapter(adapter);
                                }
                            }
                        } else {
                        }
                    }
                });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedIsbn = adapter.getItem(position);
                if (selectedIsbn != null) {
                    removeISBNFromWishList(selectedIsbn);
                }
            }
        });
    }

    private void displayProfilePic() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference imageReference = storageReference.child("profile_picture_images/" + username + "/Profile_Image");

        imageReference.getDownloadUrl().addOnSuccessListener(uri -> {
            // Load the image into the ImageView using Picasso
            Picasso.get()
                    .load(uri)
                    .placeholder(R.drawable.baseline_person_outline_24) // Placeholder image while loading
                    .error(R.drawable.baseline_person_outline_24) // Error image if loading fails
                    .into(profile_pic);
        }).addOnFailureListener(e -> {
            Toast.makeText(WishList.this, "Failed to load profile picture", Toast.LENGTH_SHORT).show();
        });
    }
    private boolean isValidISBN(String isbn) {
        return (isbn.length() == 10 || isbn.length() == 13) && isbn.matches("\\d+");
    }
    private void appendISBNToWishList(String isbn) {
        db.collection("users").document(username).update("wish_list", FieldValue.arrayUnion(isbn))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Update the ListView to reflect the changes
                            adapter.add(isbn);
                            adapter.notifyDataSetChanged();
                            Add_ISBN.setText("");
                        } else {
                            Toast.makeText(WishList.this, "Failed to update wish list", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void removeISBNFromWishList(String isbn) {
        // Update Firestore to remove the ISBN from the "wish_list"
        db.collection("users").document(username)
                .update("wish_list", FieldValue.arrayRemove(isbn))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Remove the ISBN from the adapter and update the ListView
                            adapter.remove(isbn);
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(WishList.this, "Failed to remove ISBN from wish list", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


}
