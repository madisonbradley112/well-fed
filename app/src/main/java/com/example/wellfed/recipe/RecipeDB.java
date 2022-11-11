package com.example.wellfed.recipe;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
import static com.google.android.gms.common.internal.safeparcel.SafeParcelable.NULL;

import android.util.Log;

import com.example.wellfed.ingredient.Ingredient;
import com.example.wellfed.ingredient.IngredientDB;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class RecipeDB {
    /**
     * Holds an instance of the Firebase Firestore database
     */
    private final FirebaseFirestore db;
    /**
     * Holds a collection to the Recipes stored in the FireStore db database
     */
    final CollectionReference recipesCollection;
    /**
     * Holds a collection to the Recipe Ingredients stored in the FireStore db database
     */
    private final IngredientDB ingredientDB;

    public interface OnAddRecipe {
        public void onAddRecipe(Recipe recipe, Boolean success);
    }

    public interface OnRecipeIngredientAdded {
        public void onRecipeIngredientAdd(Ingredient ingredient, int totalAdded);
    }

    /**
     * Create a RecipeDB object
     */
    public RecipeDB() {
        db = FirebaseFirestore.getInstance();
        this.recipesCollection = db.collection("Recipes");
        ingredientDB = new IngredientDB();
    }


    /**
     * Adds a recipe to the Recipe collection in db and any ingredients
     * not already in Recipe Ingredients. This method will set the Recipe id
     * to the corresponding document id in the collection
     *
     * @param recipe A Recipe object we want to add to the collection of Recipes
     *               and whose id we want to set
     * @return Returns the id of the Recipe document
     * @throws InterruptedException If any transaction in the method was not complete
     */
    //todo it works but hacky
    public void addRecipe(Recipe recipe, OnAddRecipe listener) throws InterruptedException {

        ArrayList<HashMap<String, Object>> recipeIngredients = new ArrayList<>();
        HashMap<String, Object> recipeMap = new HashMap<>();

        // add all the ingredients and store in recipeIngredients
        AtomicInteger added = new AtomicInteger();
        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            Ingredient ingredient = recipe.getIngredients().get(i);
            ingredientDB.getIngredient(ingredient, (foundIngredient, success) -> {
                if (foundIngredient != null) {
                    DocumentReference doc = ingredientDB.getDocumentReference(foundIngredient);
                    Task<DocumentSnapshot> task = doc.get();
                    HashMap<String, Object> ingredientMap = new HashMap<>();
                    ingredient.setId(foundIngredient.getId());
                    ingredientMap.put("ingredientRef", ingredientDB.getDocumentReference(ingredient));
                    ingredientMap.put("amount", ingredient.getAmount());
                    ingredientMap.put("unit", ingredient.getUnit());
                    added.addAndGet(1);
                    recipeIngredients.add(ingredientMap);
                    if (added.get() == recipe.getIngredients().size()) {
                        addRecipeHelper(recipeMap, recipeIngredients, recipe, listener);
                    }

                } else {
                    ingredientDB.addIngredient(ingredient, (addedIngredient, success1) -> {
                        if (addedIngredient == null) {
                            listener.onAddRecipe(null, false);
                            return;
                        }
                        ingredient.setId(addedIngredient.getId());
                        HashMap<String, Object> ingredientMap = new HashMap<>();
                        ingredientMap.put("ingredientRef", ingredientDB.getDocumentReference(addedIngredient));
                        ingredientMap.put("amount", ingredient.getAmount());
                        ingredientMap.put("unit", ingredient.getUnit());
                        added.addAndGet(1);
                        recipeIngredients.add(ingredientMap);
                        if (added.get() == recipe.getIngredients().size()) {
                            addRecipeHelper(recipeMap, recipeIngredients, recipe, listener);
                        }
                    });
                }
            });
        }

    }

    public void addRecipeHelper(HashMap<String, Object> recipeMap, ArrayList<HashMap<String, Object>> ingredients,
                                Recipe recipe, OnAddRecipe listener) {
        recipeMap.put("ingredients", ingredients);
        recipeMap.put("title", recipe.getTitle());
        recipeMap.put("servings", recipe.getServings());
        recipeMap.put("category", recipe.getCategory());
        recipeMap.put("comments", recipe.getComments());
        recipeMap.put("photograph", recipe.getPhotograph());
        recipeMap.put("preparation-time", recipe.getPrepTimeMinutes());

        recipesCollection
                .add(recipeMap)
                .addOnSuccessListener(addedSnapshot -> {
                    recipe.setId(addedSnapshot.getId());
                    listener.onAddRecipe(recipe, true);
                })
                .addOnFailureListener(failure -> {
                    listener.onAddRecipe(null, false);
                });
    }


    /**
     * Deletes a recipe document with the id  from the collection
     * of recipes
     *
     * @param id The id of recipe document we want to delete
     * @throws InterruptedException If the transaction in the method was not complete
     */
    public void delRecipe(String id) throws InterruptedException {
        CountDownLatch deleteLatch = new CountDownLatch(1);

        if (id.equals(NULL)) {
            Log.d("Delete Recipe", "The Recipe does not have an id");
            return;
        }

        DocumentReference recipeToDelete = recipesCollection.document(id);

        db.runTransaction((Transaction.Function<Void>) transaction -> {

                    transaction.delete(recipeToDelete);

                    return null;
                })
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "onSuccess: ");
                    deleteLatch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "onFailure: ");
                    deleteLatch.countDown();
                });

        deleteLatch.await();
    }

    /**
     * Deletes a recipe document with the id  from the recipe collection (AND Ingredients)
     * of recipes
     *
     * @param id The id of recipe document we want to delete
     * @throws InterruptedException If the transaction in the method was not complete
     */
    public void delRecipeAndIngredient(String id) throws InterruptedException {
        CountDownLatch deleteLatch = new CountDownLatch(1);

        if (id.equals(NULL)) {
            Log.d("Delete Recipe", "The Recipe does not have an id");
            return;
        }

        //TODO: delete recipe along with its associated ingredient here
    }

    /**
     * Updates the corresponding recipe document in the collection with the fields of the
     * recipe object.
     *
     * @param recipe A Recipe object whose changes we want to push to the collection of Recipes
     * @throws InterruptedException If the transaction in the method was not complete
     */
//    public void editRecipe(Recipe recipe) throws InterruptedException {
//        String id = recipe.getId();
//
//        CountDownLatch editLatch = new CountDownLatch(1);
//
//        ArrayList<DocumentReference> recipeIngredientDocuments = new ArrayList<>();
//
//        for (Ingredient i : recipe.getIngredients()) {
//            if (i.getId() == null) {
//                DocumentReference newDocumentReference = recipeIngredientDB.getDocumentReference(recipeIngredientDB.addRecipeIngredient(i));
//                recipeIngredientDocuments.add(newDocumentReference);
//            } else {
//                try {
//                    recipeIngredientDB.getRecipeIngredient(i.getId());
//                } catch (Exception err) {
//                    Log.d(TAG, "addRecipe: Failed to get recipe");
//                }
//            }
//        }
//
//        Map<String, Object> recipeMap = new HashMap<>();
//
//        recipeMap.put("title", recipe.getTitle());
//        recipeMap.put("comments", recipe.getComments());
//        recipeMap.put("category", recipe.getCategory());
//        recipeMap.put("prep-time-minutes", recipe.getPrepTimeMinutes());
//        recipeMap.put("servings", recipe.getServings());
//        recipeMap.put("photograph", recipe.getPhotograph());
//        recipeMap.put("ingredients", recipeIngredientDocuments);
//
//
//        DocumentReference newRecipe = recipesCollection.document(id);
//
//        db.runTransaction((Transaction.Function<Void>) transaction -> {
//
//                    transaction.update(newRecipe, recipeMap);
//
//                    return null;
//                })
//                .addOnSuccessListener(unused -> {
//                    Log.d(TAG, "onSuccess: ");
//                    editLatch.countDown();
//                })
//                .addOnFailureListener(e -> {
//                    Log.d(TAG, "onFailure: ");
//                    editLatch.countDown();
//                });
//
//        editLatch.await();
//    }

    /**
     * @param id A String with the id of the document who recipe we want
     * @return The Recipe object that corresponds to the document in the collection
     * If it does not exist then return null
     * @throws InterruptedException If the transaction in the method was not complete
     */
//    public Recipe getRecipe(String id) throws InterruptedException {
//        CountDownLatch getLatch = new CountDownLatch(1);
//
//        Recipe recipe = new Recipe(null);
//        recipe.setId(id);
//        DocumentReference recipeDocument = recipesCollection.document(id);
//        final DocumentSnapshot[] recipeSnapshot = new DocumentSnapshot[1];
//        db.runTransaction((Transaction.Function<Void>) transaction -> {
//
//                    recipeSnapshot[0] = transaction.get(recipeDocument);
//
//                    return null;
//                })
//                .addOnSuccessListener(unused -> {
//                    Log.d(TAG, "onSuccess: ");
//                    getLatch.countDown();
//                })
//                .addOnFailureListener(e -> {
//                    Log.d(TAG, "onFailure: ");
//                    getLatch.countDown();
//                });
//
//        getLatch.await();
//
//        if (!recipeSnapshot[0].exists()) {
//            return null;
//        }
//
//
//        recipe = getRecipe(recipeSnapshot[0]);
//
//        return recipe;
//    }

    /**
     * Gets a recipe from its DocumentSnapshot
     *
     * @param recipeSnapshot The DocumentSnapshot of the recipe we want to get
     * @return The Recipe object that corresponds to the document in the collection
     */
//    private Recipe getRecipe(DocumentSnapshot recipeSnapshot) {
//        Recipe recipe = new Recipe(recipeSnapshot.getString("title"));
//        recipe.setId(recipeSnapshot.getId());
//        recipe.setCategory(recipeSnapshot.getString("category"));
//        recipe.setComments(recipeSnapshot.getString("comments"));
//        recipe.setPhotograph(recipeSnapshot.getString("photograph"));
////        TODO: manpreet fix
////        recipe.setPhotograph(recipeSnapshot.getString("photograph"));
//        recipe.setPrepTimeMinutes(Objects.requireNonNull(recipeSnapshot.getLong("prep-time-minutes")).intValue());
//        recipe.setServings(Objects.requireNonNull(recipeSnapshot.getLong("servings")).intValue());
//
//        List<DocumentReference> recipeIngredients = (List<DocumentReference>) Objects.requireNonNull(
//                recipeSnapshot.get("ingredients"));
//
//        for (DocumentReference ingredient : recipeIngredients) {
//            try {
//                recipe.addIngredient(recipeIngredientDB.getRecipeIngredient(ingredient.getId()));
//            } catch (Exception err) {
//                Log.d(TAG, "addRecipe: Failed to get recipe");
//            }
//        }
//
//        return recipe;
//    }

    /**
     * Makes an ArrayList of Recipes out of all the documents in the collection of Recipes
     *
     * @return ArrayList The List of all Recipes contained in the
     * database
     * @throws InterruptedException If the transaction in the method was not complete
     */
//    public ArrayList<Recipe> getRecipes() throws InterruptedException {
//        Log.d(TAG, "getRecipes:");
//        CountDownLatch recipesLatch = new CountDownLatch(1);
//        final QuerySnapshot[] recipesSnapshot = new QuerySnapshot[1];
//        Task<QuerySnapshot> task = recipesCollection.get()
//                .addOnSuccessListener(value -> {
//                    recipesSnapshot[0] = value;
//                    Log.d(TAG, "getRecipes: OnSuccess");
//                    recipesLatch.countDown();
//                }).addOnFailureListener(e -> {
//                    Log.d(TAG, "getRecipes: OnFailure");
//                    recipesLatch.countDown();
//                });
//        recipesLatch.await();
//        Log.d(TAG, "getRecipes:3");
//        ArrayList<Recipe> recipes = new ArrayList<>();
//
//        for (QueryDocumentSnapshot recipeSnapshot : recipesSnapshot[0]) {
//            recipes.add(this.getRecipe(recipeSnapshot));
//        }
//
//        return recipes;
//    }

    /**
     * Get the DocumentReference from Recipes collection for the given id
     *
     * @param id The String of the document in Recipes collection we want
     * @return DocumentReference of the Recipe
     */
    public DocumentReference getDocumentReference(String id) {
        return recipesCollection.document(id);
    }


    public Recipe snapshotToRecipe(DocumentSnapshot doc) {
        Recipe recipe = new Recipe(doc.getString("title"));
        recipe.setPrepTimeMinutes(Integer.parseInt(doc.getString("preparation-time")));
        recipe.setServings(Integer.parseInt(doc.getString("servings")));
        recipe.setCategory(doc.getString("category"));
        recipe.setComments(doc.getString("comments"));
        recipe.setPhotograph(doc.getString("photograph"));

        List<HashMap<String, Object>> ingredients = (List<HashMap<String, Object>>) doc.getData().get("ingredients");
        for (HashMap<String, Object> ingredientMap : ingredients) {
            DocumentReference ingredientRef = (DocumentReference) ingredientMap.get("ingredientRef");
            ingredientDB.getIngredient(ingredientRef, (foundIngredient, success) -> {
                if (foundIngredient != null) {
                    foundIngredient.setUnit((String) ingredientMap.get("unit"));
                    foundIngredient.setAmount((Double) ingredientMap.get("amount"));
                    recipe.addIngredient(foundIngredient);
                }
            });
        }
        return recipe;
    }
}

