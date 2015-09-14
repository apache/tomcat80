package org.apache.tomcat.util.http;

import java.util.Locale;

import org.junit.Test;

public class TesterHttpMessagesPerformance {

    @Test
    public void testGetMessage() {
        int iterations = 10000000;
        int status = 200;

        HttpMessages msgs = HttpMessages.getInstance(Locale.ENGLISH);

        for (int i = 0; i < iterations; i++) {
            msgs.getMessage(status);
        }

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            msgs.getMessage(status);
        }
        long end = System.nanoTime();

        System.out.println((end -start) + "ns");
    }
}
