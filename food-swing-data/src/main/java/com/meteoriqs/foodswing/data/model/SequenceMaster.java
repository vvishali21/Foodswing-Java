package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table
public class SequenceMaster {
    @Id
    private int sequenceId;
    private int counter;
    private int month;
    private String prefix;
    private int year;

}
