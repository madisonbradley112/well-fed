package com.example.wellfed.common;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wellfed.R;
import com.example.wellfed.ingredient.Ingredient;
import com.example.wellfed.recipe.RecipeIngredientEditActivity;

import java.io.Serializable;
import java.util.ArrayList;

public abstract class EditRecyclerViewFragment<Item extends Serializable>
        extends Fragment implements EditItemAdapter.OnEditListener<Item>,
        EditItemAdapter.OnDeleteListener<Item> {
    private RecyclerView recyclerView;
    private EditItemAdapter<Item> adapter;
    private Item selectedItem;
    private String title;

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new EditItemContract<>(),
                    this::onEditActivityResult);

    private final ActivityResultLauncher<Intent> searchLauncher =
            registerForActivityResult(new SearchItemContract<>(),
                    this::onSearchActivityResult);

    public void setAdapter(EditItemAdapter<Item> adapter) {
        this.adapter = adapter;
        this.adapter.setEditListener(this);
        this.adapter.setDeleteListener(this);
    }

    public void setTitle(String title) {
        this.title = title;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_recycler_view,
                container, false);

        TextView titleTextView = view.findViewById(R.id.titleTextView);
        titleTextView.setText(title);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        ImageView addButton = view.findViewById(R.id.addButton);
        ImageView searchButton = view.findViewById(R.id.searchButton);

        addButton.setOnClickListener(v -> {
            onEdit(null);
        });

        searchButton.setOnClickListener(v -> {
            onSearch(null);
        });

        return view;
    }

    public abstract Intent createOnEditIntent(Item item);

    public abstract Intent createOnSearchIntent(Item item);

    @Override
    public void onEdit(Item item) {
        this.selectedItem = item;
        Intent intent = createOnEditIntent(item);
        editLauncher.launch(intent);
    }

    public void onSearch(Item item) {
        Intent intent = createOnSearchIntent(item);
        searchLauncher.launch(intent);
    }

    @Override
    public void onDelete(Item item) {
        int index = adapter.getItems().indexOf(item);
        adapter.getItems().remove(index);
        adapter.notifyItemRemoved(index);
    }

    abstract public void onSearchActivityResult(Pair<String, Item> result);

    private void onEditActivityResult(Pair<String, Item> result) {
        if (result == null) {
            return;
        }
        String type = result.first;
        Item ingredient = result.second;
        switch (type) {
            case "add":
                adapter.getItems().add(ingredient);
                adapter.notifyItemInserted(adapter.getItemCount());
                break;
            case "edit":
                int index = adapter.getItems().indexOf(selectedItem);
                adapter.getItems().set(index, ingredient);
                adapter.notifyItemChanged(index);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
}
