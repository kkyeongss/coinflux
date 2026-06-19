package com.example.coinflux.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {
    private String id;
    private String code;
    private String condition;   // ABOVE | BELOW
    private double targetPrice;
    private long createdAt;
}
