package com.example.coinflux.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ticker {

    @JsonProperty("code")
    private String code;

    @JsonProperty("trade_price")
    private double tradePrice;

    @JsonProperty("change")
    private String change;           // RISE / EVEN / FALL

    @JsonProperty("change_rate")
    private double changeRate;

    @JsonProperty("acc_trade_volume_24h")
    private double accTradeVolume24h;

    @JsonProperty("timestamp")
    private long timestamp;
}
