package com.example.wellfed.recipe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wellfed.ActivityBase;
import com.example.wellfed.EditActivityBase;
import com.example.wellfed.R;
import com.example.wellfed.common.EditRecyclerViewFragment;
import com.example.wellfed.common.RequiredDropdownTextInputLayout;
import com.example.wellfed.common.RequiredNumberTextInputLayout;
import com.example.wellfed.common.RequiredTextInputLayout;
import com.example.wellfed.ingredient.Ingredient;
import com.example.wellfed.ingredient.StorageIngredient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
// todo create an xml file for this class


/**
 * Activity which allows user to edit an existing recipe
 *
 * @version 1.0.0
 */
public class RecipeEditActivity extends EditActivityBase {
    private List<Ingredient> recipeIngredients;
    private Recipe recipe;
    private FloatingActionButton fab;
    private Uri uri;
    private String downloadUrl;
    private ImageView recipeImg;
    private RequiredNumberTextInputLayout prepTime;
    private RequiredNumberTextInputLayout servings;
    private RequiredTextInputLayout title;
    private RequiredTextInputLayout commentsTextInput;
    private RequiredDropdownTextInputLayout recipeCategory;
    private EditRecipeIngredientsFragment ingredientEditFragment;

    // take picture
    ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            result -> {
                if (result) {
                    uploadImg();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_edit);

        // intialize the variables
        Intent intent = getIntent();
        recipeIngredients = new ArrayList<>();
        recipe = (Recipe) intent.getSerializableExtra("Recipe");
        fab = findViewById(R.id.save_fab);


        // views
        recipeImg = findViewById(R.id.recipe_img);
        title = findViewById(R.id.recipe_title);
        prepTime = findViewById(R.id.recipe_prep_time_textView);
        servings = findViewById(R.id.recipe_no_of_servings_textView);
        commentsTextInput = findViewById(R.id.commentsTextInput);
        recipeCategory = findViewById(R.id.recipe_category);
        recipeCategory.setSimpleItems(new String[]{"Breakfast", "Lunch", "Dinner", "Appetizer", "Dessert"});

        ingredientEditFragment = new EditRecipeIngredientsFragment();
        RecipeIngredientAdapter adapter = new RecipeIngredientAdapter();
        ingredientEditFragment.setAdapter(adapter);
        ingredientEditFragment.setTitle("Ingredients");
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainerView, ingredientEditFragment)
                .commit();

        // activity started to add data a recipe
        if (recipe == null) {
            fab.setImageDrawable(getDrawable(R.drawable.ic_baseline_save_24)); // fab is save button
            fab.setOnClickListener(view -> {
                if (areValidFields()) {
                    recipe = new Recipe(title.getText().toString());
                    recipe.setCategory(recipeCategory.getText());
                    recipe.setComments(commentsTextInput.getText());
                    recipe.setServings(Integer.parseInt(servings.getText().toString()));
                    recipe.setPrepTimeMinutes(Integer.parseInt(prepTime.getText().toString()));
                    for (Ingredient ingredient : recipeIngredients) {
                        recipe.addIngredient(ingredient);
                    }
                    recipe.setPhotograph(downloadUrl);
                    recipeIngredients = ingredientEditFragment.getItems();
                    recipe.addIngredients(recipeIngredients);
                    onSave();
                }
            });
        } else {
            recipeIngredients = recipe.getIngredients();
            title.setPlaceholderText(recipe.getTitle());
            prepTime.setPlaceholderText(recipe.getPrepTimeMinutes().toString());
            servings.setPlaceholderText(recipe.getServings().toString());
            commentsTextInput.setPlaceholderText(recipe.getComments());
            recipeCategory.setPlaceholderText(recipe.getCategory());
            Picasso.get()
                    .load(recipe.getPhotograph())
                    .rotate(90)
                    .into(recipeImg);
            fab.setOnClickListener(view -> {
                if (areValidFields()) {
                    Intent intent1 = new Intent();
                    String id = recipe.getId();
                    String photoUrl = recipe.getPhotograph();
                    recipe = new Recipe(title.getText());
                    recipe.setId(id);
                    recipe.setPhotograph(photoUrl);
                    recipe.setComments(commentsTextInput.getText());
                    recipe.setServings(servings.getInteger());
                    recipe.setCategory(recipeCategory.getText());
                    recipe.addIngredients(recipeIngredients);
                    recipe.setPrepTimeMinutes(prepTime.getInteger());
                    intent.putExtra("type", "edit");
                    intent.putExtra("Recipe", recipe);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }

        uri = initTempUri();
        recipeImg.setOnClickListener(view -> {
            cameraLauncher.launch(uri);
        });
    }

    public Boolean areValidFields() {
        if (!title.isValid()) return false;
        if (!prepTime.isValid()) return false;
        if (!servings.isValid()) return false;
        if (!commentsTextInput.isValid()) return false;
        if (!recipeCategory.isValid()) return false;
        return true;
    }

    @Override public Boolean hasUnsavedChanges() {
        if (title.hasChanges()) return true;
        if (prepTime.hasChanges()) return true;
        if (servings.hasChanges()) return true;
        if (commentsTextInput.hasChanges()) return true;
        if (recipeCategory.hasChanges()) return true;
//      if (editRecyclerViewAdapter.hasChanges()) return true;
        return false;
    }

    public void onSave() {
        // return the new recipe via intent
        Intent intent = new Intent();
        intent.putExtra("type", "save");
        intent.putExtra("Recipe", recipe);
        setResult(RESULT_OK, intent);
        finish();
    }

    public Uri initTempUri() {
        File tempImgDir = new File(RecipeEditActivity.this.getFilesDir(),
                getString(R.string.temp_images_dir));
        tempImgDir.mkdir();

        File tempImg = new File(tempImgDir, getString(R.string.temp_image));
        Uri imageUri = FileProvider.getUriForFile(
                this,
                getString(R.string.authorities),
                tempImg);
        return imageUri;
    }

    public void uploadImg() {
        Uri file = uri;
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference recipesRef = storage.getReference("recipe_imgs" + (new Date()).toString());

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bitmap == null) {

            return;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes);
        String path = MediaStore.Images.Media.insertImage(RecipeEditActivity.this.getContentResolver(), bitmap, "temp", null);
        Uri uri = Uri.parse(path);
        UploadTask uploadTask = recipesRef.putFile(uri);
// Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
                int temp = 0;
                recipesRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    downloadUrl = uri.toString();
                    if (recipe != null){
                        recipe.setPhotograph(downloadUrl);
                    }
                    Picasso.get()
                            .load(downloadUrl)
                            .rotate(90)
                            .into(recipeImg);
                });
            }

        });
    }
}
