package pl.bmalinowski.iwedzakv2.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;

@Value
public class TemperatureRange {
    int minTemp;
    int maxTemp;

    @JsonCreator
    public TemperatureRange(@JsonProperty("minTemp") final int minTemp, @JsonProperty("maxTemp") final int maxTemp) {
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
    }

    public static TemperatureRange defaultRange() {
        return new TemperatureRange(45, 65);
    }

    public boolean inRange(final int temp) {
        return minTemp <= temp && temp <= maxTemp;
    }
}
