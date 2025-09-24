package com.kaustubh.transactions.ledger.service;

import com.kaustubh.transactions.ledger.config.ReplayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplayStartupRunner implements ApplicationRunner {

    private final ReplayProperties replayProperties;
    private final TransactionLogReplayService replayService;

    @Override
    public void run(ApplicationArguments args) {
        if (!replayProperties.enabledOnStartup()) {
            log.info("Replay on startup disabled");
            return;
        }

        log.warn("Starting transaction_log replay from beginning");
        replayService.replayFromBeginning();
    }
}