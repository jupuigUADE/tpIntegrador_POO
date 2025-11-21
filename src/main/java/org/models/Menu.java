package org.models;

import java.util.HashMap;
import java.util.Map;

public class Menu {
    private final Map<Integer, Recipe> recipes = new HashMap<>();

    public void addRecipe(Recipe r) {
        recipes.put(r.getId(), r);
    }

    public Recipe getRecipeById(int id) {
        return recipes.get(id);
    }

    public Map<Integer, Recipe> getAll() { return recipes; }
}

