package com.meteoriqs.foodswing.data.model;

import com.meteoriqs.foodswing.data.entity.RecipeMaster;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class RecipeGetResponseModel extends BaseEntity {

    private RecipeGetResponseModel() {

    }

    @Data
    public static class RecipeGetDetailsResponse {
        private RecipeMaster master;
        private List<Stage> stages = new ArrayList<>();
    }

    @Data
    public static class Stage {
        private List<StageDetails> ingredientSteps = new ArrayList<>();
        private VesselConfiguration vesselData;
    }

    @Data
    public static class StageDetails {
        private String itemName;
        private String itemUomName;
        private int mediumId;
        private String mediumName;
        private int ingredientId;
        private String ingredientName;
        private String ingredientUomName;
        private BigDecimal quantity;
        private BigDecimal cost;
    }

    @Data
    public static class VesselConfiguration {
        private int stage;
        private int processId;
        private String processName;
        private String stageEndAlertDuration;
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
        private String stageDuration;

    }
}
