package pl.bmalinowski.iwedzakv2.services.impl;

import android.content.Context;

import pl.bmalinowski.iwedzakv2.MainActivity;
import pl.bmalinowski.iwedzakv2.services.DataFetcherPort;
import pl.bmalinowski.iwedzakv2.services.LanScannerPort;
import pl.bmalinowski.iwedzakv2.services.NotificationPort;

//TODO Refactor!
public final class ServiceFactory {

    private final Context context;
    private LanScannerWorker lanScannerWorker;
    private DataFetcherAdapter dataFetcherAdapter;
    private NotificationAdapter notificationAdapter;

    public ServiceFactory(final Context context) {
        this.context = context;
    }

    public LanScannerPort getScannerPort(final int port) {
//        synchronized (this) {
//            if (lanScannerWorker == null) {
//                lanScannerWorker = new LanScannerWorker(context, port);
//            }
//        }
        return new LanScannerWorker(context, port);
    }

    public DataFetcherPort getDataFetcherPort() {
        synchronized (this) {
            if (dataFetcherAdapter == null) {
                dataFetcherAdapter = new DataFetcherAdapter(context);
            }
        }
        return dataFetcherAdapter;
    }

    public NotificationPort getNotificationPort() {
        synchronized (this) {
            if (notificationAdapter == null) {
                notificationAdapter = new NotificationAdapter(context, MainActivity.class);
            }
        }
        return notificationAdapter;
    }

}
