package com.example.wellfed.recipe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.example.wellfed.R;
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
public class RecipeEditActivity extends ActivityBase implements RecipeIngredientAdapter.OnIngredientClick {
    private RecyclerView ingredientRV;
    private List<Ingredient> recipeIngredients;
    private RecipeIngredientAdapter recipeIngredientAdapter;
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


    // take picture
    ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            result -> {
                if (result) {
                    uploadImg();
                }
            });

    // add ingredient
    ActivityResultLauncher<Ingredient> ingredientLauncher = registerForActivityResult(
            new RecipeIngredientEditContract(), result -> {
                if (result == null) {
                    return;
                }
                String type = result.first;
                Ingredient ingredient = result.second;
                switch (type) {
                    case "add":
                        recipeIngredients.add(ingredient);
                        recipeIngredientAdapter.notifyItemInserted(recipeIngredients.size());
                    case "quit":
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
    );

    ActivityResultLauncher<Ingredient> ingredientSearchLauncher = registerForActivityResult(
            new RecipeIngredientSearchContract(), result -> {
                if (result == null) {
                    return;
                }
                String type = result.first;
                StorageIngredient ingredient = result.second;
                switch (type) {
                    case "edit":
                        recipeIngredients.add(ingredient);
                        recipeIngredientAdapter.notifyItemInserted(recipeIngredients.size());
                    case "quit":
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
    );

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
        ingredientRV = findViewById(R.id.recipe_ingredient_recycleViewer);
        recipeImg = findViewById(R.id.recipe_img);
        title = findViewById(R.id.recipe_title);
        prepTime = findViewById(R.id.recipe_prep_time_textView);
        servings = findViewById(R.id.recipe_no_of_servings_textView);
        commentsTextInput = findViewById(R.id.commentsTextInput);
        recipeCategory = findViewById(R.id.recipe_category);
        ImageView addIngredient = findViewById(R.id.ingredient_add_btn);
        ImageView searchIngredient = findViewById(R.id.ingredient_search_btn);

        recipeCategory.setSimpleItems(new String[]{"Breakfast", "Lunch", "Dinner", "Appetizer", "Dessert"});


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
                    onSave();
                }
            });
        } else {

        }

        // ingredient recycle viewer and it's adapter
        recipeIngredientAdapter = new RecipeIngredientAdapter(recipeIngredients, R.layout.recipe_ingredient_edit, this);
        ingredientRV.setAdapter(recipeIngredientAdapter);
        ingredientRV.setLayoutManager(new LinearLayoutManager(RecipeEditActivity.this));

        uri = initTempUri();
        recipeImg.setOnClickListener(view -> {
            cameraLauncher.launch(uri);
        });

        addIngredient.setOnClickListener(view -> {
            ingredientLauncher.launch(null);
        });

        searchIngredient.setOnClickListener(view -> {
            ingredientSearchLauncher.launch(null);
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
                    Picasso.get()
                            .load(downloadUrl)
                            .rotate(90)
                            .into(recipeImg);
                });
            }

        });
    }

    @Override
    public void onEditClick(String reason, int pos) {
        switch (reason) {
            case "edit":
                ingredientLauncher.launch(recipeIngredients.get(pos));
            case "delete":
                recipeIngredients.remove(pos);
                recipeIngredientAdapter.notifyItemRemoved(pos);
                break;
            default:
                break;
        }
    }
}