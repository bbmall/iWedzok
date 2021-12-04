package pl.bmalinowski.iwedzakv2.services.impl;

import android.content.Context;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.stream.IntStream;

class LanScannerWorkerTest {

    @Test
//    @DisplayName("1 + 1 = 2")
    void addsTwoNumbers() {
        final String currentIp = "192.168.55.121";
        IntStream.range(100, 131)
                .boxed()
                .map(idx -> currentIp.substring(0, currentIp.lastIndexOf('.')) + "." + idx)
                .forEach(System.out::println);
    }

    @Test
    void trueTest() {
        final Context mockContext = Mockito.mock(Context.class);
        final LanScannerWorker w = new LanScannerWorker(mockContext, port);
        final String currentIp = "192.168.55.121";
        IntStream.range(100, 131)
                .boxed()
                .map(idx -> currentIp.substring(0, currentIp.lastIndexOf('.')) + "." + idx)
                .forEach(System.out::println);
    }
}