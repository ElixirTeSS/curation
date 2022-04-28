package yanan.zhang;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Yanan Zhang
 **/
public class ScheduleTask implements Runnable {

    private final MainService mainService = new MainService();

    @Override
    public void run() {
        System.out.println(new Date() + " : Schedule task is started.");
        mainService.execute();
        System.out.println(new Date() + " : Schedule task is finished.");
    }

    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(
                new ScheduleTask(),
                0,
                24,
                TimeUnit.HOURS);
    }
}
