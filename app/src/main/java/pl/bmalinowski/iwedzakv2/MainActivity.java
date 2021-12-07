package pl.bmalinowski.iwedzakv2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pl.bmalinowski.iwedzakv2.command.Command;
import pl.bmalinowski.iwedzakv2.command.DebugChangedCommand;
import pl.bmalinowski.iwedzakv2.command.ReceivedPayloadCommand;
import pl.bmalinowski.iwedzakv2.command.StopForegroundServiceCommand;
import pl.bmalinowski.iwedzakv2.command.TempRangeChangedCommand;
import pl.bmalinowski.iwedzakv2.command.UrlChangedCommand;
import pl.bmalinowski.iwedzakv2.model.Payload;
import pl.bmalinowski.iwedzakv2.model.TemperatureRange;
import pl.bmalinowski.iwedzakv2.model.URL;

public class MainActivity extends AppCompatActivity {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final List<BroadcastReceiver> receivers = new ArrayList<>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initClass();
    }

    private void initClass() {
        //register handlers
        findViewById(R.id.nbAlarmFrom)
                .setOnKeyListener(getOnKeyListener());
        findViewById(R.id.nbAlarmTo)
                .setOnKeyListener(getOnKeyListener());
        ((SwitchCompat) findViewById(R.id.debugSwitch))
                .setOnCheckedChangeListener((t, checked) -> publish(new DebugChangedCommand(checked)));
        registerReceiver();

        final Intent foregroundIntent = new Intent(this, ForegroundActivity.class);

        ((SwitchCompat) findViewById(R.id.foregroundServiceSwitch))
                .setOnCheckedChangeListener((t, checked) -> {
                    runOnUiThread(() -> {
                                if (checked) {
                                    startForegroundService(ForegroundActivity.class);
                                } else {
                                    stopForegroundService(ForegroundActivity.class);
                                }
                            }
                    );
                });
    }

    private boolean isDebug() {
        return ((SwitchCompat) findViewById(R.id.debugSwitch)).isChecked();
    }

    private boolean isForegroundServiceActive() {
        return ((SwitchCompat) findViewById(R.id.foregroundServiceSwitch)).isChecked();
    }

    private void startForegroundService(final Class<?> activityClass) {
        final Intent startIntent = new Intent(MainActivity.this, activityClass);
        startIntent.setAction("STARTFOREGROUND_ACTION");
        startIntent.putExtra("debug", isDebug());
        startForegroundService(startIntent);
    }

    private void stopForegroundService(final Class<?> activityClass) {
        final Intent stopIntent = new Intent(MainActivity.this, activityClass);
        stopIntent.setAction(StopForegroundServiceCommand.class.getName());
        startService(stopIntent);
        runOnUiThread(() -> {
            handleSmokingHouseUrlChanged(null);
            handlePayloadReceived(null);
        });
    }

    private void registerReceiver() {
        final BroadcastReceiver urlBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final String json = intent.getStringExtra("json");
                try {
                    final UrlChangedCommand command = OBJECT_MAPPER.readValue(json, UrlChangedCommand.class);
                    runOnUiThread(() -> handleSmokingHouseUrlChanged(command.getUrl()));
                } catch (final IOException e) {
                    runOnUiThread(() -> handleSmokingHouseUrlChanged(null));
                }
            }
        };
        final BroadcastReceiver sensorsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final String json = intent.getStringExtra("json");
                try {
                    final ReceivedPayloadCommand command = OBJECT_MAPPER.readValue(json, ReceivedPayloadCommand.class);
                    runOnUiThread(() -> handlePayloadReceived(command.getPayload()));
                } catch (final IOException e) {
                    runOnUiThread(() -> handlePayloadReceived(null));
                }
            }
        };
        registerReceiver(urlBroadcastReceiver, new IntentFilter(UrlChangedCommand.class.getName()));
        registerReceiver(sensorsBroadcastReceiver, new IntentFilter(ReceivedPayloadCommand.class.getName()));
    }

    @Override
    public Intent registerReceiver(final BroadcastReceiver receiver, final IntentFilter filter) {
        receivers.add(receiver);
        return super.registerReceiver(receiver, filter);
    }

    private void publish(final Command command) {
        final Intent in = new Intent(command.getClass().getName());
        final Bundle extras = new Bundle();
        extras.putString("json", writeValueAsString(command));
        in.putExtras(extras);
        getBaseContext().sendBroadcast(in);
    }

    private String writeValueAsString(final Object o) {
        try {
            return OBJECT_MAPPER.writeValueAsString(o);
        } catch (final JsonProcessingException e) {
            return null;
        }
    }

    @NonNull
    private View.OnKeyListener getOnKeyListener() {
        return (view, keyCode, event) -> {
            try {
                final EditText nbAlarmFrom = findViewById(R.id.nbAlarmTo);
                final EditText nbAlarmTo = findViewById(R.id.nbAlarmFrom);
                publish(new TempRangeChangedCommand(
                                new TemperatureRange(
                                        Integer.parseInt(nbAlarmFrom.getText().toString()),
                                        Integer.parseInt(nbAlarmTo.getText().toString()))
                        )
                );
            } catch (final NumberFormatException exc) {
                Log.e("System.err", "Cannot publish temp range changed", exc);
                return false;
            }
            return true;
        };
    }

    private void handlePayloadReceived(final Payload payload) {
        final TextView txtTemp1 = findViewById(R.id.txtTemp1);
        final TextView txtTemp2 = findViewById(R.id.txtTemp2);
        final TextView txtDuration = findViewById(R.id.txtDuration);
        if (Objects.nonNull(payload)) {
            txtTemp1.setText(payload.getTemp1().toString());
            txtTemp2.setText(payload.getTemp2().toString());
            txtDuration.setText(payload.durationAsLocalTime().toString());
        } else {
            txtTemp1.setText("");
            txtTemp2.setText("");
            txtDuration.setText("");
        }
    }

    private URL handleSmokingHouseUrlChanged(final URL url) {
        final TextView txtIp = findViewById(R.id.txtIp);
        if (Objects.nonNull(url)) {
            txtIp.setClickable(true);
            txtIp.setMovementMethod(LinkMovementMethod.getInstance());
            final String link = String.format("<a href='%s'> %s </a>", url, url);
            txtIp.setText(Html.fromHtml(link, Html.FROM_HTML_MODE_COMPACT));
        } else {
            txtIp.setClickable(false);
            txtIp.setText(isForegroundServiceActive() ? "Nieaktywny" : "Brak połączenia");
        }
        return url;
    }

    @Override
    protected void onStop() {
        super.onStop();
        receivers.stream()
                .filter(Objects::nonNull)
                .forEach(this::unregisterReceiver);
    }

}