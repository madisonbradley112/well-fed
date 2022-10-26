package com.example.wellfed;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.view.Menu;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.color.DynamicColors;

public class MainActivity extends FragmentActivity {
    final String TAG = "Sample";
    NavigationCollectionAdapter navigationCollectionAdapter;
    ViewPager2 viewPager;
    BottomAppBar bottomAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navigationCollectionAdapter = new NavigationCollectionAdapter(this);
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(navigationCollectionAdapter);
        bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setOnMenuItemClickListener(menuItem -> {
            int j;
            switch (menuItem.getItemId()) {
                case R.id.ingredient_storage:
                    j = 0;
                    break;
                case R.id.recipe_book:
                    j = 1;
                    break;
                case R.id.meal_book:
                    j = 2;
                    break;
                case R.id.shopping_cart:
                    j = 3;
                    break;
                default:
                    return false;
            }
            viewPager.setCurrentItem(j);
            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                bottomAppBar = findViewById(R.id.bottomAppBar);
                Menu menu = bottomAppBar.getMenu();

                for (int i = 0; i < menu.size(); ++i) {
                    menu.getItem(i).getIcon().setTint(
                            getResources().getColor(R.color.black)
                    );
                }

                menu.getItem(position).getIcon().setTint(
                        getResources().getColor(R.color.purple_200)
                );
            }
        });

        viewPager.setCurrentItem(2);
    }
}
