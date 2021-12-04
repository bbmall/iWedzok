package pl.bmalinowski.iwedzakv2;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pl.bmalinowski.iwedzakv2.model.NotificationDTO;
import pl.bmalinowski.iwedzakv2.model.SensorsDTO;
import pl.bmalinowski.iwedzakv2.model.TemperatureRange;
import pl.bmalinowski.iwedzakv2.model.URL;
import pl.bmalinowski.iwedzakv2.services.DataFetcherPort;
import pl.bmalinowski.iwedzakv2.services.LanScannerPort;
import pl.bmalinowski.iwedzakv2.services.NotificationPort;
import pl.bmalinowski.iwedzakv2.services.impl.ServiceFactory;

public class MainActivity extends AppCompatActivity {

    private final double MAX_DURATION_MIN = 4.5 * 60;
    private ServiceFactory serviceFactory;
    private Optional<URL> smokingHouseIp = Optional.empty();
    private TextView txtTemp1;
    private TextView txtTemp2;
    private TextView txtDuration;
    private TextView txtIp;
    private EditText nbAlarmFrom;
    private EditText nbAlarmTo;
    private Instant tempNotificationSendTime;
    private Instant timeNotificationSendTime;
    //service: https://stackoverflow.com/a/39675175/6760468

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initClass();
        final SwitchCompat switchCompat = findViewById(R.id.debugSwitch);
        switchCompat.setOnCheckedChangeListener((t, checked) -> findAndSetSmokingHouseIp());
        supplyData();
    }

    private void initClass() {
        serviceFactory = new ServiceFactory(this);
        txtTemp1 = findViewById(R.id.txtTemp1);
        txtTemp2 = findViewById(R.id.txtTemp2);
        txtDuration = findViewById(R.id.txtDuration);
        txtIp = findViewById(R.id.txtIp);
        nbAlarmFrom = findViewById(R.id.nbAlarmFrom);
        nbAlarmTo = findViewById(R.id.nbAlarmTo);
    }

    private void supplyData() {
        final DataFetcherPort dataFetcherPort = serviceFactory.getDataFetcherPort();
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        final Runnable job = () -> {
            if (smokingHouseIp.isPresent()) {
                final Optional<SensorsDTO> sensorsDTO = dataFetcherPort.collectData(smokingHouseIp.get());
                runOnUiThread(() -> setUIData(sensorsDTO));
            } else {
                runOnUiThread(() -> setUIData(Optional.empty()));
                findAndSetSmokingHouseIp();
            }
        };
        executorService.scheduleWithFixedDelay(job, 0, 2, TimeUnit.SECONDS);

    }

    private void setUIData(final Optional<SensorsDTO> sensorsDTO) {
        if (sensorsDTO.isPresent()) {
            final SensorsDTO dto = sensorsDTO.get();
            txtTemp1.setText(dto.getTemp1().toString());
            txtTemp2.setText(dto.getTemp2().toString());
            txtDuration.setText(dto.durationAsLocalTime().toString());
            try {
                final TemperatureRange tempRange = new TemperatureRange(
                        Integer.parseInt(nbAlarmFrom.getText().toString()),
                        Integer.parseInt(nbAlarmTo.getText().toString())
                );

                if (!tempRange.inRange(dto.getTemp1()) || !tempRange.inRange(dto.getTemp2())) {
                    showTempOutOfRangeNotification(dto, !tempRange.inRange(dto.getTemp1()) ? dto.getTemp1() : dto.getTemp2());
                }
                if (dto.getDuration().toMinutes() > MAX_DURATION_MIN) {
                    showDurationNotification(dto);
                }
            } catch (final Exception e) {
                Log.e("System.err", "Cannot verify temps", e);
            }
        } else {
            txtTemp1.setText("");
            txtTemp2.setText("");
            txtDuration.setText("");
        }
    }

    private void showTempOutOfRangeNotification(final SensorsDTO sensorsDTO, final Integer temp) {
        if (tempNotificationSendTime == null ||
                Instant.now().toEpochMilli() - tempNotificationSendTime.toEpochMilli() > Duration.ofMinutes(1).toMillis()) {
            final NotificationPort notificationPort = serviceFactory.getNotificationPort();
            notificationPort.showNotification(new NotificationDTO("iWędzok", "Temperatura wędzoka " + temp + " °C!"));
            tempNotificationSendTime = Instant.now();
        }
    }

    private void showDurationNotification(final SensorsDTO sensorsDTO) {
        if (timeNotificationSendTime == null ||
                Instant.now().toEpochMilli() - timeNotificationSendTime.toEpochMilli() > Duration.ofMinutes(20).toMillis()) {
            final NotificationPort notificationPort = serviceFactory.getNotificationPort();
            notificationPort.showNotification(new NotificationDTO("iWędzok", "Czas wędzenia " + sensorsDTO.durationAsLocalTime()));
            timeNotificationSendTime = Instant.now();
        }
    }

    private void findAndSetSmokingHouseIp() {
        final SwitchCompat switchCompat = findViewById(R.id.debugSwitch);
        final int port = switchCompat.isChecked() ? 8000 : 80;
        final LanScannerPort lanScannerPort = serviceFactory.getScannerPort(port);
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Runnable job = () -> {
            final Optional<URL> ip = lanScannerPort.findExternalIP();
            runOnUiThread(() -> {
                if (ip.isPresent()) {
                    txtIp.setClickable(true);
                    txtIp.setMovementMethod(LinkMovementMethod.getInstance());
                    final String link = String.format("<a href='%s'> %s </a>", ip.get(), ip.get());
                    txtIp.setText(Html.fromHtml(link, Html.FROM_HTML_MODE_COMPACT));

                } else {
                    //Tu chyba nie powinno  to tyle razy wchodzić. jeno raz
                    txtIp.setClickable(false);
                    txtIp.setText("No connection");
                }
                this.setSmokingHouseIp(ip.orElse(null));
            });
        };
        executorService.execute(job);
    }

    private URL setSmokingHouseIp(final URL smokingHouseURL) {
        this.smokingHouseIp = Optional.ofNullable(smokingHouseURL);
        return smokingHouseURL;
    }
}