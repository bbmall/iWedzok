package pl.bmalinowski.iwedzakv2.model;

import lombok.Value;

@Value
public class TemperatureRange {
    int minTemp;
    int maxTemp;

    public boolean inRange(final int temp) {
        return temp < minTemp || temp > maxTemp;
    }
}
