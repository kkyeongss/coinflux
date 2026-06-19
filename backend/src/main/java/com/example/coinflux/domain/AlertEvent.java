package com.example.coinflux.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent {
    private String ruleId;
    private String code;
    private String condition;
    private double targetPrice;
    private double actualPrice;
    private long triggeredAt;
}
