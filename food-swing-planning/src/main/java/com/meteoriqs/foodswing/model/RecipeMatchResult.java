package com.meteoriqs.foodswing.model;

import java.util.ArrayList;
import java.util.List;

public class RecipeMatchResult {
    private List<Integer> matchedIds = new ArrayList<>();
    private List<Integer> closestAboveIds = new ArrayList<>();
    private List<Integer> closestBelowIds = new ArrayList<>();

    public void addMatchedId(int recipeId) {
        matchedIds.add(recipeId);
    }

    public void addClosestAboveId(int recipeId) {
        closestAboveIds.add(recipeId);
    }

    public void addClosestBelowId(int recipeId) {
        closestBelowIds.add(recipeId);
    }

    public List<Integer> getMatchedIds() {
        return matchedIds;
    }

    public List<Integer> getClosestAboveIds() {
        return closestAboveIds;
    }

    public List<Integer> getClosestBelowIds() {
        return closestBelowIds;
    }

    public boolean isPerfectMatch() {
        return !matchedIds.isEmpty();
    }
}