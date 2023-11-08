package com.meteoriqs.foodswing.data.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "uom_master")
public class UomMasterEntity extends BaseEntity{

    @Id
    private int uomId;
    private String name;
}
