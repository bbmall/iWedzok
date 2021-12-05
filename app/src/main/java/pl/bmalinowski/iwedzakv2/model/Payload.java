package pl.bmalinowski.iwedzakv2.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

import lombok.Value;

@Value
public class Payload {
    Instant currentTime;
    Integer temp1;
    Integer temp2;
    Duration duration;

    @JsonCreator
    public Payload(@JsonProperty("temp1") final Integer temp1, @JsonProperty("temp2") final Integer temp2,
                   @JsonProperty("duration") final Duration duration) {
        this.temp1 = temp1;
        this.temp2 = temp2;
        this.duration = duration;
        this.currentTime = Instant.now();
    }

    public LocalTime durationAsLocalTime() {
        return LocalTime.of((int) duration.toHours(),
                (int) duration.minusHours(duration.toHours()).toMinutes(),
                (int) duration.minusHours(duration.toHours()).minusMinutes(duration.minusHours(duration.toHours()).toMinutes()).getSeconds());
    }
}
