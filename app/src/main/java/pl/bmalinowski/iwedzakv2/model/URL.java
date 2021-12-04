package pl.bmalinowski.iwedzakv2.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Value;

@Value
public class URL {
    String value;

    public static URL parseOrNull(final String value) {

        final String regex = "http://[1-9][0-9]+\\.[1-9][0-9]+\\.[1-9][0-9]+\\.[1-9][0-9]+[:0-9]*";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(value);

        if (matcher.find()) {
            return new URL(value);
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }

    public URL replaceSubnetWith(final Integer idx, final int port) {
        return new URL(value.substring(0, value.lastIndexOf('.')) + "." + idx + ":" + port);
    }
}
