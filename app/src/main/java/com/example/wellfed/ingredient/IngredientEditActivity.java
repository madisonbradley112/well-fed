package com.example.wellfed.ingredient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wellfed.R;
import com.example.wellfed.common.OnQuitListener;
import com.example.wellfed.common.RequiredDateTextInputLayout;
import com.example.wellfed.common.RequiredTextInputLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Date;
import java.util.Objects;

public class IngredientEditActivity extends AppCompatActivity implements OnQuitListener {
    private EditText name;
    private EditText amount;
    private EditText unit;
    private EditText location;
    private EditText bestBefore;
    private StorageIngredient ingredient;
    private RequiredTextInputLayout nameLayout;
    private RequiredTextInputLayout amountLayout;
    private RequiredTextInputLayout unitLayout;
    private RequiredTextInputLayout locationLayout;
    private RequiredDateTextInputLayout bestBeforeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_ingredient);
        name = findViewById(R.id.ingredient_name);
        amount = findViewById(R.id.ingredient_quantity);
        unit = findViewById(R.id.ingredient_unit_value);
        location = findViewById(R.id.ingredient_location);
        bestBefore = findViewById(R.id.ingredient_expiration);

        nameLayout = findViewById(R.id.textInputLayout);
        amountLayout = findViewById(R.id.textInputLayout4);
        unitLayout = findViewById(R.id.textInputLayout5);
        locationLayout = findViewById(R.id.textInputLayout6);
        bestBeforeLayout = findViewById(R.id.textInputLayout2);

        // Get ingredient from intent
        ingredient = (StorageIngredient) getIntent().getSerializableExtra("ingredient");

        if (ingredient != null) {
            name.setText(ingredient.getDescription());
            amount.setText(String.valueOf(ingredient.getAmount()));
            unit.setText(ingredient.getUnit());
            location.setText(ingredient.getLocation());
            // Set date in yyyy-MM-dd format
            bestBeforeLayout.setPlaceholderDate(ingredient.getBestBeforeDate());
        }

        // Enable back button in action bar to go back to previous activity
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // Set up save button
        FloatingActionButton saveButton = findViewById(R.id.ingredient_save_button);
        saveButton.setOnClickListener(view -> {
            onSave();
        });
    }

    private Boolean hasUnsavedChanges() {
        if (ingredient == null) {
            return !name.getText().toString().isEmpty() ||
                    !amount.getText().toString().isEmpty() ||
                    !unit.getText().toString().isEmpty() ||
                    !location.getText().toString().isEmpty() ||
                    !bestBefore.getText().toString().isEmpty();
        } else {
            return !name.getText().toString().equals(ingredient.getDescription()) ||
                    !amount.getText().toString().equals(String.valueOf(ingredient.getAmount())) ||
                    !unit.getText().toString().equals(ingredient.getUnit()) ||
                    !location.getText().toString().equals(ingredient.getLocation()) ||
                    !bestBefore.getText().toString().equals(ingredient.getBestBefore());
        }
    }

    private void onSave() {
        // Verify that all fields are filled
        if (name.getText().toString().isEmpty()) {
            name.setError("Description is required");
            return;
        }

        if (amount.getText().toString().isEmpty()) {
            amount.setError("Amount is required");
            return;
        }

        if (unit.getText().toString().isEmpty()) {
            unit.setError("Unit is required");
            return;
        }

        if (location.getText().toString().isEmpty()) {
            location.setError("Location is required");
            return;
        }

        if (bestBefore.getText().toString().isEmpty()) {
            bestBefore.setError("Best before is required");
            return;
        }


        if (ingredient == null) {
            String[] date = bestBefore.getText().toString().split("-");
            ingredient = new StorageIngredient(name.getText().toString(),
                    Float.parseFloat(amount.getText().toString()),
                    unit.getText().toString(),
                    location.getText().toString(),
                    new Date(Integer.parseInt(date[0]) - 1900,
                            Integer.parseInt(date[1]) - 1,
                            Integer.parseInt(date[2])));
            Intent intent = new Intent();
            intent.putExtra("type", "add");
            intent.putExtra("ingredient", ingredient);
            setResult(Activity.RESULT_OK, intent);
            finish();
        } else {
            ingredient.setDescription(name.getText().toString());
            ingredient.setAmount(Float.parseFloat(amount.getText().toString()));
            ingredient.setUnit(unit.getText().toString());
            ingredient.setLocation(location.getText().toString());
            // Get date in yyyy-MM-dd format
            String[] date = bestBefore.getText().toString().split("-");
            ingredient.setBestBefore(new Date(Integer.parseInt(date[0]) - 1900,
                    Integer.parseInt(date[1]) - 1,
                    Integer.parseInt(date[2])));

            Intent intent = new Intent();
            intent.putExtra("type", "edit");
            intent.putExtra("ingredient", ingredient);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("type", "back");
        intent.putExtra("ingredient", ingredient);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void onQuit() {
        Intent intent = new Intent();
        intent.putExtra("type", "quit");
        intent.putExtra("ingredient", ingredient);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
