package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VesselIndentIngredientsResponse extends BaseEntity{
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
}
