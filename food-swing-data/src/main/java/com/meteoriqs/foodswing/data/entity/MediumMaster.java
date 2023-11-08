package com.meteoriqs.foodswing.data.entity;

import com.meteoriqs.foodswing.data.model.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MediumMaster extends BaseEntity {
    @Id
    private int mediumId;
    private String mediumName;
}
