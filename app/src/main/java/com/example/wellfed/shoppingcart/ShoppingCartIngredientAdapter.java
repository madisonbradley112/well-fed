package com.example.wellfed.shoppingcart;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;


import com.example.wellfed.R;

import java.util.ArrayList;

/**
 * The ShoppingCartIngredientAdapter class binds ArrayList<ShoppingCartIngredient> to RecyclerView.
 * <p>
 * @author Weiguo Jiang
 * @version v1.0.0 2022-10-28
 **/
public class ShoppingCartIngredientAdapter extends RecyclerView.Adapter<ShoppingCartIngredientAdapter.ViewHolder> {
    ArrayList<ShoppingCartIngredient> ingredients;

    public ShoppingCartIngredientAdapter(ArrayList<ShoppingCartIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView ingredientNameTextView;
        public TextView ingredientAttributeTextView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ingredientNameTextView = itemView.findViewById(R.id.shopping_cart_ingredient_name);
            ingredientAttributeTextView = itemView.findViewById(R.id.shopping_cart_ingredient_subtext);
        }
    }

    @NonNull
    @Override
    public ShoppingCartIngredientAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        View ingredientView = layoutInflater.inflate(R.layout.shopping_cart_ingredient, parent,
                false);

        return new ViewHolder(ingredientView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingCartIngredient ingredient = ingredients.get(position);

        TextView ingredientNameTextView = holder.ingredientNameTextView;
        ingredientNameTextView.setText(ingredient.getDescription());

        TextView ingredientAttributeTextView = holder.ingredientAttributeTextView;
        String unit = ingredient.getUnit();
        ingredientAttributeTextView.setText(unit);
    }

    @Override
    public int getItemCount() {return ingredients.size();}
}