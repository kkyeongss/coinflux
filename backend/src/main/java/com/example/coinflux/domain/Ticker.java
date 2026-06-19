package com.example.coinflux.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ticker {

    private String code;

    @JsonAlias("trade_price")
    private double tradePrice;

    private String change;

    @JsonAlias("change_rate")
    private double changeRate;

    @JsonAlias("acc_trade_volume_24h")
    private double accTradeVolume24h;

    private long timestamp;
}
