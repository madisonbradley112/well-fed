package com.example.wellfed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.wellfed.ingredient.Ingredient;
import com.example.wellfed.ingredient.IngredientDB;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

/**
 * The IngredientDBTest class is used to test the IngredientDB class.
 */
@RunWith(AndroidJUnit4.class)
public class IngredientDBTest {
    private static final String TAG = "IngredientDBTest";
    /**
     * Holds the timeout in seconds for the CountDownLatch.
     */
    private static final long TIMEOUT = 5;
    /**
     * Holds the instance of the IngredientDB class.
     */
    IngredientDB ingredientDB;
    /**
     * Holds the instance of the mockIngredient
     */
    Ingredient mockIngredient;
    /**
     * Holds the instance of the nonExistingIngredient
     */
    Ingredient nonExistingIngredient;

    /**
     * Creates a new IngredientDB object and mock ingredients.
     */
    @Before
    public void before() {
        ingredientDB = new IngredientDB();
        mockIngredient = new Ingredient("Broccoli");
        mockIngredient.setCategory("Vegetable");
        nonExistingIngredient = new Ingredient(null);
        nonExistingIngredient.setId("-1");
    }

    /**
     * Tests addIngredient and getIngredient on DB with a complete
     * ingredient, checks if the ingredient is added to DB and retrieved from
     * DB.
     *
     * @throws InterruptedException if the test times out
     */
    @Test
    public void testAddFull() throws InterruptedException {
        Log.d(TAG, "testAddFull");
        CountDownLatch latch = new CountDownLatch(1);
        ingredientDB.addIngredient(mockIngredient, (addIngredient, addSuccess) -> {
            Log.d(TAG, ":onAddIngredient");
            String id = addIngredient.getId();
            assertNotNull(addIngredient);
            assertTrue(addSuccess);

            ingredientDB.getIngredient(id, (getIngredient, getSuccess) -> {
                Log.d(TAG, ":onGetIngredient");
                assertNotNull(getIngredient);
                assertTrue(getSuccess);
                assertEquals(mockIngredient.getCategory(),
                        getIngredient.getCategory());
                assertEquals(mockIngredient.getDescription(),
                        getIngredient.getDescription());

                // remove the ingredient
                ingredientDB.deleteIngredient(getIngredient,
                        (deleteIngredient, deleteSuccess) -> {
                            Log.d(TAG, ":onDeleteIngredient");
                            assertNotNull(deleteIngredient);
                            assertTrue(deleteSuccess);
                            latch.countDown();
                        });
            });
        });
        if (!latch.await(TIMEOUT, SECONDS)) {
            throw new InterruptedException();
        }
    }

    /**
     * Tests the add and get functionality, when fields are blank.
     *
     * @throws InterruptedException if the test times out
     */
    @Test
    public void testAddMissingFields() throws InterruptedException {
        Log.d(TAG, "testAddMissingFields");
        CountDownLatch latch = new CountDownLatch(1);

        mockIngredient.setCategory(null);

        // testing whether it was what was inserted into db
        ingredientDB.addIngredient(mockIngredient, (addIngredient, addSuccess) -> {
            Log.d(TAG, ":onAddIngredient");
            String id = addIngredient.getId();
            assertNotNull(addIngredient);
            assertTrue(addSuccess);

            ingredientDB.getIngredient(id, (getIngredient, getSuccess) -> {
                Log.d(TAG, ":onGetIngredient");
                assertNotNull(getIngredient);
                assertTrue(getSuccess);
                assertEquals("Broccoli", getIngredient.getDescription());
                assertNull(getIngredient.getCategory());

                // remove the ingredient
                ingredientDB.deleteIngredient(getIngredient,
                        (deleteIngredient, deleteSuccess) -> {
                            Log.d(TAG, ":onDeleteIngredient");
                            assertNotNull(deleteIngredient);
                            assertTrue(deleteSuccess);
                            latch.countDown();
                        });
            });
        });

        if (!latch.await(TIMEOUT, SECONDS)) {
            throw new InterruptedException();
        }
    }

    /**
     * Tests deleteIngredient on DB, checks if the ingredient is deleted.
     *
     * @throws InterruptedException if the test times out
     */
    @Test
    public void testDeleteIngredient() throws InterruptedException {
        Log.d(TAG, "testDeleteIngredient");
        CountDownLatch latch = new CountDownLatch(1);

        ingredientDB.addIngredient(mockIngredient,
                (addIngredient, addSuccess) -> {
            Log.d(TAG, ":onAddIngredient");
            String id = addIngredient.getId();
            assertNotNull(addIngredient);
            assertTrue(addSuccess);
            ingredientDB.deleteIngredient(addIngredient,
                    (deleteIngredient, deleteSuccess) -> {
                Log.d(TAG, ":onDeleteIngredient");
                assertNotNull(deleteIngredient);
                assertTrue(deleteSuccess);
                ingredientDB.getIngredient(id, (getIngredient, getSuccess) -> {
                    Log.d(TAG, ":onGetIngredient");
                    assertNull(getIngredient);
                    assertFalse(getSuccess);
                    latch.countDown();
                });
            });
        });

        if (!latch.await(TIMEOUT, SECONDS)) {
            throw new InterruptedException();
        }
    }

    /**
     * Tests deleteIngredient with a NonExistingIngredient, checks if the
     * listener is called with not null.
     *
     * @throws InterruptedException if the test times out
     */
    @Test
    public void deleteNonExistingIngredient()
            throws InterruptedException {
        Log.d(TAG, "deleteNonExistingIngredient");
        CountDownLatch latch = new CountDownLatch(1);

        ingredientDB.deleteIngredient(nonExistingIngredient,
                (deleteIngredient, deleteSuccess) -> {
                    Log.d(TAG, ":onDeleteIngredient");
                    assertNotNull(deleteIngredient);
                    assertTrue(deleteSuccess);
                    latch.countDown();
                });

        if (!latch.await(TIMEOUT, SECONDS)) {
            throw new InterruptedException();
        }
    }

    /**
     * Tests whether null is returned upon getting an invalid
     * ingredient.
     *
     * @throws InterruptedException if the test times out
     */
    @Test
    public void getNonExistingIngredient() throws InterruptedException {
        Log.d(TAG, "getNonExistingIngredient");
        CountDownLatch latch = new CountDownLatch(1);

        ingredientDB.getIngredient("-1", (getIngredient, getSuccess) -> {
            Log.d(TAG, ":onGetIngredient");
            assertNull(getIngredient);
            assertFalse(getSuccess);
            latch.countDown();
        });

        if (!latch.await(TIMEOUT, SECONDS)) {
            throw new InterruptedException();
        }
    }

    @Test
    public void getIngredientByCategory() throws InterruptedException {
        Log.d(TAG, "get Ingredient based on category and description");
        CountDownLatch latch = new CountDownLatch(1);

        ingredientDB.addIngredient(mockIngredient,
                (addedIngredient, addSuccess) -> {
            assertNotNull(addedIngredient);
            assertTrue(addSuccess);
            ingredientDB.getIngredient(mockIngredient,
                    (searchedIngredient, searchSuccess) -> {
                Log.d(TAG, ":onGetIngredient by category");
                assertNotNull(searchedIngredient);
                assertTrue(searchSuccess);
                ingredientDB.deleteIngredient(searchedIngredient,
                        (deletedIngredient, deleteSuccess) -> {
                    Log.d(TAG, ":onDeleteIngredient");
                    assertNotNull(deletedIngredient);
                    assertTrue(deleteSuccess);
                    latch.countDown();
                });
            });
        });

        if (!latch.await(TIMEOUT, SECONDS)) {
            throw new InterruptedException();
        }
    }


    /**
     * Tests updateIngredient on DB. Checks if the ingredient was updated.
     *
     * @throws InterruptedException if the test times out
     */
    @Test
    public void testUpdateIngredient() throws InterruptedException {
        Log.d(TAG, "testUpdateIngredient");

        String updatedCategory = "Fruit";
        String updatedDescription = "Apple";

        CountDownLatch latch = new CountDownLatch(1);

        ingredientDB.addIngredient(mockIngredient,
                (addIngredient, addSuccess) -> {
            Log.d(TAG, ":onAddIngredient");
            String id = addIngredient.getId();
            assertNotNull(addIngredient);
            assertTrue(addSuccess);

            addIngredient.setCategory(updatedCategory);
            addIngredient.setDescription(updatedDescription);

            ingredientDB.updateIngredient(addIngredient,
                    (updateIngredient, updateSuccess) -> {
                Log.d(TAG, ":onUpdateIngredient");
                assertNotNull(updateIngredient);
                assertTrue(updateSuccess);
                ingredientDB.getIngredient(id, (getIngredient, getSuccess) -> {
                    Log.d(TAG, ":onGetIngredient");
                    assertNotNull(getIngredient);
                    assertTrue(getSuccess);
                    assertEquals(updatedCategory, getIngredient.getCategory());
                    assertEquals(updatedDescription,
                            getIngredient.getDescription());
                    //  remove the ingredient
                    ingredientDB.deleteIngredient(getIngredient,
                            (deleteIngredient, deleteSuccess) -> {
                                assertNotNull(deleteIngredient);
                                assertTrue(deleteSuccess);
                                Log.d(TAG, ":onDeleteIngredient");
                                latch.countDown();
                            });
                });
            });
        });
        if (!latch.await(TIMEOUT, SECONDS)) {
            throw new InterruptedException();
        }
    }


    @Test
    public void getIngredientByCategoryNotExists() throws InterruptedException {
        Log.d(TAG, "get Ingredient based on category and description");
        CountDownLatch latch = new CountDownLatch(1);
        Ingredient testIngredient = new Ingredient();
        testIngredient.setCategory("fffffffffffff");
        testIngredient.setDescription("affffffffffff");

        ingredientDB.getIngredient(testIngredient,
                (searchIngredient, getSuccess) -> {
            assertNull(searchIngredient);
            assertFalse(getSuccess);
            latch.countDown();
        });

        if (!latch.await(TIMEOUT, SECONDS)) {
            throw new InterruptedException("Timed out");
        }
    }
}



