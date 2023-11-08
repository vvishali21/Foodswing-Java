package com.meteoriqs.foodswing.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaveRecipePlanningResponse {
    private Status status;
    private Data data;

    @lombok.Data
    public static class Data {
        private int createdBy;
        private Instant createdTime;
        private int updatedBy;
        private Instant updatedTime;
        private Boolean isDraft;
        private RecipeMasterRequest recipeMasterRequest;
        private List<RecipeDetailsEntityRequest> recipeDetailsEntityRequest;
        private Boolean active;
    }

    @lombok.Data
    public static class RecipeMasterRequest {
        private int createdBy;
        private Instant createdTime;
        private int updatedBy;
        private Instant updatedTime;
        private int recipeId;
        private int itemId;
        private int uomId;
        private BigDecimal preparationQuantity;
        private String recipeDescription;
        private String totalTimeTaken;
        private String preparationType;
        private Boolean active;
    }

    @lombok.Data
    public static class RecipeDetailsEntityRequest {
        private VesselData vesselData;
        private List<IngredientStep> ingredientSteps;
    }

    @lombok.Data
    public static class VesselData {
        private int createdBy;
        private Instant createdTime;
        private int updatedBy;
        private Instant updatedTime;
        private int baseSv;
        private Float basePv;
        private int productionSv;
        private Float productPv;
        private String duration;
        private String durationUnit;
        private int power;
        private int fq;
        private String fwdTime;
        private String revTime;
        private LocalTime startTime;
        private LocalTime endTime;
        private String timeTaken;
        private String stageEndAlertDuration;
        private String stageDuration;
        private int processId;
        private int stage;
        private Boolean active;
    }

    @lombok.Data
    public static class IngredientStep {
        private int createdBy;
        private Instant createdTime;
        private int updatedBy;
        private Instant updatedTime;
        private int ingredientId;
        private BigDecimal quantity;
        private int mediumId;
        private Boolean active;
    }
}
