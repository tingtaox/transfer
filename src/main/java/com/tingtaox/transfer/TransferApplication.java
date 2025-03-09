package com.tingtaox.transfer;

import com.tingtaox.transfer.services.impls.ReconciliationProcess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class TransferApplication {

    static Logger logger = LogManager.getLogger(TransferApplication.class);

    private static final long PERIOD = 43200000L;   // 12 hours

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(TransferApplication.class, args);
        ReconciliationProcess reconciliationProcess = context.getBean(ReconciliationProcess.class);
        // have a thread to run the recon process
        new Thread(() -> {
            while (true) {
                reconciliationProcess.reconciliation();
                // run the recon process every 12 hours
                try {
                    Thread.sleep(PERIOD);
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }
        }).start();
    }
}
