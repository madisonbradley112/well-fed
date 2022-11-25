package com.xffffff.wellfed.mealplan;

import android.app.Activity;
import android.util.Pair;

import com.google.android.material.snackbar.Snackbar;
import com.xffffff.wellfed.ActivityBase;
import com.xffffff.wellfed.common.DBConnection;
import com.xffffff.wellfed.common.Launcher;
import com.xffffff.wellfed.ingredient.Ingredient;
import com.xffffff.wellfed.recipe.Recipe;
import com.xffffff.wellfed.unit.Unit;
import com.xffffff.wellfed.unit.UnitConverter;

import java.util.Date;

public class MealPlanController {
    private final ActivityBase activity;
    /**
     * The DB of stored ingredients
     */
    private final MealPlanDB db;
    private MealPlanAdapter adapter;

    public void setOnDataChanged(OnDataChanged onDataChanged) {
        this.onDataChanged = onDataChanged;
    }

    private OnDataChanged onDataChanged;

    public interface OnDataChanged {
        public void onDataChanged(MealPlan mealPlan);
    }

    /**
     * The MealPlanController constructor. Creates a new MealPlanController
     * object.
     *
     * @param activity The activity that the controller is being created in.
     */
    public MealPlanController(Activity activity) {
        this.activity = (ActivityBase) activity;
        DBConnection connection =
                new DBConnection(activity.getApplicationContext());
        db = new MealPlanDB(connection);
        adapter = new MealPlanAdapter(db);
    }

    public void startListening(String id) {
        db.getMealPlanDocumentReference(id)
                .addSnapshotListener((doc, err) -> {
                    db.getMealPlan(doc, (foundMealPlan, success) -> {
                        onDataChanged.onDataChanged(foundMealPlan);
                    });
                });
    }

    /**
     * GetAdapter method for the MealPlanController. Returns the adapter
     * for the RecyclerView of meal plans.
     *
     * @return The adapter for the RecyclerView of meal plans.
     */
    public MealPlanAdapter getAdapter() {
        return adapter;
    }

    /**
     * The setAdapter method for the ingredient search screen. Sets the
     * adapter for the RecyclerView of meal plans.
     *
     * @param adapter The adapter for the RecyclerView of meal plans.
     */
    public void setAdapter(MealPlanAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * The addMealPlan method for the MealPlanController. Adds a new meal plan
     * to the database.
     *
     * @param mealPlan The meal plan to be added to the database.
     */
    public void addMealPlan(MealPlan mealPlan) {
        db.addMealPlan(mealPlan, (addMealPlan, addSuccess) -> {
            if (!addSuccess) {
                this.activity.makeSnackbar(
                        "Failed to add " + addMealPlan.getTitle());
            } else {
                this.activity.makeSnackbar("Added " + addMealPlan.getTitle());
            }
        });
    }

    /**
     * The updateMealPlan method for the MealPlanController. Updates a meal
     * plan in the database.
     *
     * @param mealPlan The meal plan to be updated in the database.
     */
    public void updateMealPlan(MealPlan mealPlan) {
        db.updateMealPlan(mealPlan, (updateMealPlan, updateSuccess) -> {
            if (!updateSuccess) {
                this.activity.makeSnackbar(
                        "Failed to update " + updateMealPlan.getTitle());
            } else {
                this.activity.makeSnackbar(
                        "Updated " + updateMealPlan.getTitle());
            }
        });
    }

    /**
     * The deleteMealPlan method for the MealPlanController. Deletes a meal
     * plan from the database.
     * @param mealPlan The meal plan to be deleted from the database.
     * @param surpressSnackbar A boolean indicating whether to surpress the snackbar or not.
     */
    public void deleteMealPlan(MealPlan mealPlan, boolean surpressSnackbar) {
        this.db.delMealPlan(mealPlan, ((deleteMealPlan, deleteSuccess) -> {
            if (!deleteSuccess) {
                if (!surpressSnackbar) {
                    this.activity.makeSnackbar(
                            "Failed to delete" + deleteMealPlan.getTitle());
                }
            } else {
                if (!surpressSnackbar) {
                    this.activity.makeSnackbar(
                            "Deleted " + deleteMealPlan.getTitle());
                }
            }
        }));
    }

    /**
     * Checks if an item is in the past - if it is, delete it and display snackbar.
     * @param mealPlan the mealplan to check
     */
    public void checkMealInPast(MealPlan mealPlan) {
        Date today = new Date(System.currentTimeMillis()-24*60*60*1000);
        Date eatDate = mealPlan.getEatDate();

        assert eatDate != null;
        if (eatDate.getTime() < today.getTime()) {
            deleteMealPlan(mealPlan, true);
            this.activity.makeSnackbar("Cannot make a MealPlan in the past.");
        }
    }

    /**
     * The scaleRecipe method for the MealPlanController. Scales a recipe
     * to the desired number of servings.
     *
     * @param recipe           The recipe to be scaled.
     * @param mealPlanServings The number of servings to scale the recipe to.
     * @return The scaled recipe.
     */
    public Recipe scaleRecipe(Recipe recipe, int mealPlanServings) {
        Recipe scaledRecipe = new Recipe(recipe);
        UnitConverter unitConverter = new UnitConverter(this.activity);
        int recipeServings = scaledRecipe.getServings();
        double scalingFactor = (double) mealPlanServings / recipeServings;
        for (Ingredient ingredient : scaledRecipe.getIngredients()) {
            ingredient.setAmount(ingredient.getAmount() * scalingFactor);
            Pair<Double, Unit> pair =
                    unitConverter.scaleUnit(ingredient.getAmount(),
                            unitConverter.build(ingredient.getUnit()));
            ingredient.setAmount(pair.first);
            ingredient.setUnit(pair.second.getUnit());
        }
        scaledRecipe.setServings(mealPlanServings);
        return scaledRecipe;
    }
}
