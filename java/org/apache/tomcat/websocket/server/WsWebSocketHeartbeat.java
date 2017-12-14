package org.apache.tomcat.websocket.server;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tomcat.websocket.BackgroundProcess;
import org.apache.tomcat.websocket.BackgroundProcessManager;

public class WsWebSocketHeartbeat implements BackgroundProcess {

    private Timer pingTimer;
    private boolean stop = false;
    private volatile int processPeriod = 10;
    private volatile WsSession session;

    public WsWebSocketHeartbeat() {}

    public WsWebSocketHeartbeat(WsSession session) {
        this.session = session;
    }

    @Override
    public void backgroundProcess() {
        this.stop = false;
        this.setProcessPeriod(processPeriod);
    }

    @Override
    public void setProcessPeriod(int period) {
        this.processPeriod = period;

        if(this.pingTimer != null) {
            stopPingTimer();
        }

        startPingTimer(new TimerTask() {
            final int MAX_PONG_FAIL_COUNT = 3;
            int failCount = 0;

            @Override
            public void run() {
                if(!stop) {
                    if(failCount < MAX_PONG_FAIL_COUNT) {
                        long lastPongTime = session.getLastPong();
                        long currentTime = System.currentTimeMillis();

                        if(lastPongTime < currentTime - pingTimeout * 1000L) {
                            failCount++;
                        else
                            failCount = 0;
                    
                        try {
                            sendPingMessage(session);
                        } catch (IOException e) {
                            failCount++;
                        }
                    }
                    else {
                        stop = true;
                    }
                }
                else {
                    stopPingTimer();
                }
            }
        }, period * 1000L);

    }

    @Override
    public int getProcessPeriod() {
        return processPeriod;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public boolean isHeartbeatStop() {
        return stop;
    }

    public void startPingTimer(TimerTask timerTask, long period) {
        this.pingTimer = new Timer();
        this.pingTimer.scheduleAtFixedRate(timerTask, 0L, period);
    }

    public void stopPingTimer() {
        this.pingTimer.cancel();
        this.pingTimer = null;
    }
}
