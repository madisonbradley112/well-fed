package com.example.wellfed.mealplan;

import com.example.wellfed.common.DBConnection;
import com.example.wellfed.ingredient.Ingredient;
import com.example.wellfed.ingredient.IngredientDB;
import com.example.wellfed.recipe.Recipe;
import com.example.wellfed.recipe.RecipeDB;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class is used to access the meal plan database.
 * It contains methods to add, remove, and update meal plans.
 * It also contains methods to add, remove, and update recipes in meal plans.
 */
public class MealPlanDB {
    /**
     * Declares tag for logging purposes.
     */
    private final static String TAG = "MealPlanDB";

    /**
     * Holds the instance of the Firebase FireStore DB.
     */
    private FirebaseFirestore db;

    /**
     * Holds the instance of the RecipeDB.
     */
    private RecipeDB recipeDB;

    /**
     * Holds the instance of the StorageIngredientDB.
     */
    private IngredientDB ingredientDB;

    /**
     * Holds the CollectionReference for the users MealPlan collection.
     */
    private CollectionReference mealPlanCollection;

    /**
     * This interface is used to handle the result of
     * adding MealPlan to the db.
     */
    public interface OnAddMealPlanListener {
        /**
         * Called when addMealPlan returns a result.
         *
         * @param mealPlan the MealPlan object added to the db,
         *                 null if the add operation failed.
         * @param success  true if the add operation was successful,
         *                 false otherwise.
         */
        void onAddMealPlanResult(MealPlan mealPlan, boolean success);
    }

    /**
     * This interface is used to handle the result of
     * deleting the chosen MealPlan object from the db.
     */
    public interface OnDeleteMealPlanListener {
        /**
         * Called when deleteMealPlan returns a result.
         *
         * @param mealPlan the MealPlan object deleted from the db,
         *                 null if the delete operation failed.
         * @param success  true if the delete operation was successful,
         *                 false otherwise.
         */
        void onDeleteMealPlanResult(MealPlan mealPlan, boolean success);
    }

    /**
     * This interface is used to handle the result of
     * finding the MealPlan object in the db.
     */
    public interface OnGetMealPlanListener {
        /**
         * Called when getMealPlan returns a result.
         *
         * @param mealPlan the MealPlan object found in the db,
         *                 null if the get operation failed.
         * @param success  true if the get operation was successful,
         *                 false otherwise.
         */
        void onGetMealPlanResult(MealPlan mealPlan, boolean success);
    }

    /**
     * This interface is used to handle the result of
     * updating the chosen MealPlan object in the db.
     */
    public interface OnUpdateMealPlanListener {
        /**
         * Called when updateMealPlan returns a result.
         *
         * @param mealPlan the MealPlan object updated in the db,
         *                 null if the update operation failed.
         * @param success  true if the update operation was successful,
         *                 false otherwise.
         */
        void onUpdateMealPlanResult(MealPlan mealPlan, boolean success);
    }

    /**
     * Constructor for class MealPlanDB, initializes declared fields.
     * @param connection the DBConnection object used to access the db.
     */
    public MealPlanDB(DBConnection connection) {
        // Creates new instances of RecipeDB and storageIngredientDB
        // based on current user connection.
        recipeDB = new RecipeDB(connection);
        ingredientDB = new IngredientDB(connection);

        // Gets the instance of the Firebase FireStore DB based
        // on current user connection.
        this.db = connection.getDB();

        // Gets the current user's MealPlan collection from db,
        // create one if the collection DNE.
        this.mealPlanCollection = connection.getCollection("MealPlan");
    }

    /**
     * This method is used to add a MealPlan object to the db.
     * @param mealPlan the MealPlan object to be added to the db.
     * @param listener the OnAddMealPlanListener object to handle the result.
     */
    public void addMealPlan(MealPlan mealPlan, OnAddMealPlanListener listener) throws Exception {
        // Store each ingredient as a HashMap with fields:
        // ingredientRef, amount & unit.
        ArrayList<HashMap<String, Object>> mealPlanIngredients = new ArrayList<>();
        for (Ingredient i: mealPlan.getIngredients()) {
            // Gets ingredient from db.
            ingredientDB.getIngredient(i, (foundIngredient, success1) -> {
                // Initialize a mapping for the ingredient.
                HashMap<String, Object> ingredientMap = new HashMap<>();

                // If the ingredient already exists in our Ingredient collection.
                if (foundIngredient != null) {
                    DocumentReference ingredientRef = ingredientDB.getDocumentReference(foundIngredient);
                    i.setId(foundIngredient.getId());

                    ingredientMap.put("ingredientRef", ingredientRef);
                    ingredientMap.put("amount", i.getAmount());
                    ingredientMap.put("unit", i.getUnit());
                } else {
                    // If the ingredient does not exist in our Ingredient collection,
                    // we will create a new ingredient for it.
                    ingredientDB.addIngredient(i, (addedIngredient, success2) -> {
//                        if (addedIngredient == null) {
//                            listener.onAddMealPlanResult(null, false);
//                            return;
//                        }
                        i.setId(addedIngredient.getId());

                        ingredientMap.put("ingredientRef", ingredientDB.getDocumentReference(addedIngredient));
                        ingredientMap.put("amount", i.getAmount());
                        ingredientMap.put("unit", i.getUnit());
                    });
                }
            });
        }

        // Stores references to recipes in the MealPlan object.
        ArrayList<DocumentReference> mealPlanRecipes = new ArrayList<>();
        for (Recipe r: mealPlan.getRecipes()) {
            if (r.getId() != null) {
                // If the recipe has an id, gets the recipe's reference from db.
                DocumentReference recipeRef = recipeDB.getDocumentReference(r.getId());
                mealPlanRecipes.add(recipeRef);
            } else {
                throw new Exception("Recipe object with a null id detected");
            }
        }

        // Initialize MealPlan document mapping.
        HashMap<String, Object> mealPlanMap = new HashMap<>();
        // Fill the map.
        mealPlanMap.put("title", mealPlan.getTitle());
        mealPlanMap.put("category", mealPlan.getCategory());
        mealPlanMap.put("eat date", mealPlan.getEatDate());
        mealPlanMap.put("servings", mealPlan.getServings());
        mealPlanMap.put("ingredients", mealPlanIngredients);
        mealPlanMap.put("recipes", mealPlanRecipes);

        // Adds the MealPlan mapping to the db.
        this.mealPlanCollection
                .add(mealPlanMap)
                .addOnSuccessListener(addedMealPlanDoc -> {
                    // Sets the document id for the MealPlan object.
                    mealPlan.setId(addedMealPlanDoc.getId());
                    listener.onAddMealPlanResult(mealPlan, true);
                })
                .addOnFailureListener(failure -> {
                    listener.onAddMealPlanResult(null, false);
                });
    }

    /**
     * Gets the MealPlan object with the given id from the db.
     * @param id the id of the MealPlan object to be retrieved.
     * @param listener the OnGetMealPlanListener object to handle the result.
     */
    public void getMealPlan(String id, OnGetMealPlanListener listener) {
        if (id == null) {
            listener.onGetMealPlanResult(null, false);
            return;
        }
        // Get reference to the MealPlan document with the given id.
        DocumentReference mealPlanRef = this.mealPlanCollection.document(id);
        mealPlanRef.get()
                .addOnSuccessListener(mealPlanDoc -> {
                    // Initialize a new MealPlan object and set its fields.
                    MealPlan mealPlan = new MealPlan(mealPlanDoc.getString("title"));
                    mealPlan.setId(mealPlanDoc.getId());
                    mealPlan.setCategory(mealPlanDoc.getString("category"));
                    mealPlan.setEatDate(mealPlanDoc.getDate("eat date"));
                    mealPlan.setServings(mealPlanDoc.getLong("servings").intValue());

                    // Convert the list of ingredient references to a list of Ingredient objects.
                    ArrayList<Ingredient> ingredients = new ArrayList<>();
                    for (DocumentReference ingredientRef: (ArrayList<DocumentReference>) mealPlanDoc.get("ingredients")) {
                        ingredientRef.get()
                                .addOnSuccessListener(ingredientDoc -> {
                                    // Create a new Ingredient object and set its fields.
                                    Ingredient ingredient = new Ingredient(ingredientDoc.getString("description"));
                                    ingredient.setId(ingredientDoc.getId());
                                    ingredient.setCategory(ingredientDoc.getString("category"));
                                    ingredients.add(ingredient);
                                })
                                .addOnFailureListener(failure -> {
                                    listener.onGetMealPlanResult(null, false);
                                });
                    }

                    // Convert the list of recipe references to a list of Recipe objects.
                    ArrayList<Recipe> recipes = new ArrayList<>();
                    for (DocumentReference recipeRef: (ArrayList<DocumentReference>) mealPlanDoc.get("recipes")) {
                        recipeRef.get()
                                .addOnSuccessListener(recipeDoc -> {
                                    // Create a new Recipe object and set its fields.
                                    Recipe recipe = new Recipe(recipeDoc.getString("title"));
                                    recipe.setId(recipeDoc.getId());
                                    recipe.setCategory(recipeDoc.getString("category"));
                                    recipe.setServings(recipeDoc.getLong("servings").intValue());
                                    recipe.setComments(recipeDoc.getString("comments"));
                                    recipe.setPhotograph(recipeDoc.getString("photograph"));
                                    recipe.setPrepTimeMinutes(recipeDoc.getLong("preparation-time").intValue());

                                    // Add the recipe to the list of recipes.
                                    recipes.add(recipe);
                                })
                                .addOnFailureListener(failure -> {
                                    listener.onGetMealPlanResult(null, false);
                                });
                    }

                    // Adds ingredients and recipes to the MealPlan object.
                    for (Ingredient i: ingredients) {
                        mealPlan.addIngredient(i);
                    }

                    for (Recipe r: recipes) {
                        mealPlan.addRecipe(r);
                    }

                    // Return the MealPlan object.
                    listener.onGetMealPlanResult(mealPlan, true);
                })
                // If the MealPlan document is not found, return null.
                .addOnFailureListener(failure -> {
                    listener.onGetMealPlanResult(null, false);
                });
    }

    /**
     * Deletes the MealPlan object with the given id from the db.
     *
     * @param id the id of the MealPlan object to be deleted.
     * @param listener the OnDeleteMealPlanListener object to handle the result.
     */
    public void deleteMealPlan(String id, OnDeleteMealPlanListener listener) {
        if (id == null) {
            listener.onDeleteMealPlanResult(null, false);
        }

        // Get reference to the MealPlan document with the given id.
        DocumentReference mealPlanRef = this.mealPlanCollection.document(id);
        mealPlanRef.delete()
                .addOnSuccessListener(success -> {
                    listener.onDeleteMealPlanResult(new MealPlan(id), true);
                })
                .addOnFailureListener(failure -> {
                    listener.onDeleteMealPlanResult(null, false);
                });
    }

    /**
     * Updates
     *
     * @param mealPlan the MealPlan object to be updated.
     * @param listener the OnUpdateMealPlanListener object to handle the result.
     */
    public void updateMealPlan(MealPlan mealPlan, OnUpdateMealPlanListener listener) throws Exception{
        if (mealPlan == null) {
            listener.onUpdateMealPlanResult(null, false);
        }

        // Similar to that in addMealPlan(). We fetch references to ingredients
        // and recipes stored in the MealPlan object.
        ArrayList<DocumentReference> mealPlanIngredientDocuments = new ArrayList<>();
        ArrayList<DocumentReference> mealPlanRecipeDocuments = new ArrayList<>();

        for (Ingredient i: mealPlan.getIngredients()) {
            if (i.getId() != null) {
                // If the ingredient has an id, gets the ingredient's reference from db.
                DocumentReference ingredientRef = ingredientDB.getDocumentReference(i);
                mealPlanIngredientDocuments.add(ingredientRef);
            } else {
                throw new Exception("Ingredient object with a null id detected");
            }
        }

        for (Recipe r: mealPlan.getRecipes()) {
            if (r.getId() != null) {
                // If the recipe has an id, gets the recipe's reference from db.
                DocumentReference recipeRef = recipeDB.getDocumentReference(r.getId());
                mealPlanRecipeDocuments.add(recipeRef);
            } else {
                throw new Exception("Recipe object with a null id detected");
            }
        }

        // Get reference to the MealPlan document with the given id.
        DocumentReference mealPlanRef = this.mealPlanCollection.document(mealPlan.getId());

        // Update the MealPlan document based on the object.
        mealPlanRef.update("title", mealPlan.getTitle(),
                        "category", mealPlan.getCategory(),
                        "eat date", mealPlan.getEatDate(),
                        "servings", mealPlan.getServings(),
                        "ingredients", mealPlanIngredientDocuments,
                        "recipes", mealPlanRecipeDocuments)
                .addOnSuccessListener(success -> {
                    listener.onUpdateMealPlanResult(mealPlan, true);
                })
                .addOnFailureListener(failure -> {
                    listener.onUpdateMealPlanResult(null, false);
                });
    }


    /**
     * Fet the DocumentReference object for the MealPlan document with the given id.
     *
     * @param id the id of the MealPlan document.
     * @return the DocumentReference object for the MealPlan document.
     */
    public DocumentReference getMealPlanDocumentReference(String id) {
        return this.mealPlanCollection.document(id);
    }

    /** Gets a query for MealPlans in the db.
     * @return the query for MealPlans in the db.
     */
    public Query getQuery() {
        return this.mealPlanCollection;
    }
}