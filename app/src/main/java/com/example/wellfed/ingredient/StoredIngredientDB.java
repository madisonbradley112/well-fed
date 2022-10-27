package com.example.wellfed.ingredient;

import static android.content.ContentValues.TAG;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoredIngredientDB {
    private FirebaseFirestore db;
    private CollectionReference collection;
    private CollectionReference ingredients;

    public StoredIngredientDB() {
        this.db = FirebaseFirestore.getInstance();
        this.collection = db.collection("StoredIngredients");
        this.ingredients = db.collection("Ingredients");
    }

    public String addStoredIngredient(StoredIngredient storedIngredient) {
        // Some of the fields of a StoredIngredient may be empty.
        Map<String, Object> ingredient = new HashMap<>();
        ingredient.put("Categories", storedIngredient.getCategory());
        ingredient.put("Description", storedIngredient.getDescription());

        String id = this.ingredients.document().getId();
        this.ingredients
                .document(id)
                .set(ingredient)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot written with ID: " + id);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

        // add it to stored ingredients now
        Map<String, Object> stored = new HashMap<>();
        stored.put("unit", storedIngredient.getUnit());
        stored.put("amount", storedIngredient.getAmount());
        stored.put("location", storedIngredient.getLocation());
        stored.put("best-before", storedIngredient.getBestBefore());
        DocumentReference documentReference = db.document("Ingredients/"+id);
        stored.put("ingredient", documentReference);

        this.collection
                .document(id)
                .set(stored)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot written with ID: " + id);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

        return id;
    }

    public void updateStoredIngredient(String id, StoredIngredient storedIngredient) {

        Map<String, Object> ingredient = new HashMap<>();
        ingredient.put("category", storedIngredient.getCategory());
        ingredient.put("description", storedIngredient.getDescription());

        this.ingredients
                .document(id)
                .set(ingredient)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot updated with ID: " + id);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                });

        DocumentReference storedDocument = collection.document(id);
        // update object in stored ingredients now
        Map<String, Object> stored = new HashMap<>();
        stored.put("unit", storedIngredient.getUnit());
        stored.put("amount", storedIngredient.getAmount());
        stored.put("location", storedIngredient.getLocation());
        stored.put("best-before", storedIngredient.getBestBefore());

        this.collection
                .document(id)
                .set(stored)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot updated with ID: " + id);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                });

    }

    public void removeFromStorage(String id) {
        // removes ingredient from storage
        this.collection
                .document(id)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting document", e);
                    }
                });
    }

    public StoredIngredient getStoredIngredient(String name) {
        StoredIngredient obtainedIngredient = new StoredIngredient("temp");
        this.collection
                .whereEqualTo("description", name)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                obtainedIngredient.setBestBefore((Date) document.get("best-before"));
                                obtainedIngredient.setLocation((String) document.get("location"));
                                obtainedIngredient.setAmount((Integer) document.get("amount"));
                                obtainedIngredient.setUnit((String) document.get("unit"));

                                DocumentReference ingredient =
                                        document.getDocumentReference("ingredient");

                                ingredient
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot document = task.getResult();
                                                    if (document.exists()) {
                                                        obtainedIngredient.setDescription((String) document.get("description"));
                                                        obtainedIngredient.setCategory((String) document.get("category"));
                                                    } else {
                                                        Log.d(TAG, "No such document");
                                                    }
                                                } else {
                                                    Log.d(TAG, "get failed with ", task.getException());
                                                }
                                            }
                                        });
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());

                        }
                    }
                });
        return obtainedIngredient;
    }

    public ArrayList<StoredIngredient> getStoredIngredients() {
        ArrayList<StoredIngredient> newIngredientList = new ArrayList<>();
        this.collection
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                StoredIngredient obtainedIngredient = new StoredIngredient("temp");
                                obtainedIngredient.setBestBefore((Date) document.get("best-before"));
                                obtainedIngredient.setLocation((String) document.get("location"));
                                obtainedIngredient.setAmount((Integer) document.get("amount"));
                                obtainedIngredient.setUnit((String) document.get("unit"));

                                DocumentReference ingredient =
                                        document.getDocumentReference("ingredient");

                                ingredient
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot document = task.getResult();
                                                    if (document.exists()) {
                                                        obtainedIngredient.setDescription((String) document.get("description"));
                                                        obtainedIngredient.setCategory((String) document.get("category"));
                                                    } else {
                                                        Log.d(TAG, "No such document");
                                                    }
                                                } else {
                                                    Log.d(TAG, "get failed with ", task.getException());
                                                }
                                            }
                                        });
                                newIngredientList.add(obtainedIngredient);
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());

                        }
                    }
                });
        return newIngredientList;
    }
}
