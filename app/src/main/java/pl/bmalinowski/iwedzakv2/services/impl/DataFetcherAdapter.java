package pl.bmalinowski.iwedzakv2.services.impl;

import android.content.Context;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.bmalinowski.iwedzakv2.model.Payload;
import pl.bmalinowski.iwedzakv2.model.URL;
import pl.bmalinowski.iwedzakv2.services.DataFetcherPort;

class DataFetcherAdapter implements DataFetcherPort {
    private static final String TEMP_1_PREFIX = "Temp 1";
    private static final String TEMP_2_PREFIX = "Temp 2";
    private static final String TIME_PREFIX = "Time";

    private final ApiClient apiClient;

    public DataFetcherAdapter(final Context context) {
        this.apiClient = new ApiClient(context);
    }

    @Override
    public Optional<Payload> collectData(final URL URL) {
        final Optional<String> responseOpt = apiClient.getCurrentSmokingHouseState(URL);
        if (responseOpt.isPresent()) {
            final String response = responseOpt.get();
            final int temp1 = extractNumberFor(TEMP_1_PREFIX, response);
            final int temp2 = extractNumberFor(TEMP_2_PREFIX, response);
            final Duration duration = extractDurationFor(TIME_PREFIX, response);

            return Optional.of(new Payload(temp1, temp2, duration));
        }
        return Optional.empty();
    }

    private int extractNumberFor(final String prefix, final String text) {
        return (int) Double.parseDouble(extractStringFor(prefix, text).orElse("-1"));
    }

    private Duration extractDurationFor(final String prefix, final String text) {
        try {
            final String[] duration = extractStringFor(prefix, text)
                    .orElseThrow(RuntimeException::new)
                    .split(":");

            return Duration.ofMinutes(Integer.parseInt(duration[0]))
                    .plus(Duration.ofSeconds(Integer.parseInt(duration[1])));
        } catch (final Exception e) {
            return Duration.of(-1, ChronoUnit.SECONDS);
        }
    }

    private Optional<String> extractStringFor(final String prefix, final String text) {

        final String regex = prefix + ": ([0-9\\.:]+)";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1));
        }
        return Optional.empty();
    }
}
