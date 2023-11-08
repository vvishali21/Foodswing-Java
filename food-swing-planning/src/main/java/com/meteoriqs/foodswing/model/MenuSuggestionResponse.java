package com.meteoriqs.foodswing.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuSuggestionResponse {
   private int mealSuggestionId;
   private BigDecimal totalMakingCost;
   private BigDecimal costPerMeal;
   private String percentOfSaleCost;
   private List<SuggestedItem> mealSuggestionDetails;

   public MenuSuggestionResponse(List<SuggestedItem> mealSuggestionDetails) {
      this.mealSuggestionDetails = mealSuggestionDetails;
   }

   @Data
   @AllArgsConstructor
   @NoArgsConstructor
   public static class SuggestedItem {
      private int itemId;
      private String itemName;
      private int categoryId;
      private String categoryName;
      private BigDecimal quantity;
   }
}

