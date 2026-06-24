package com.itas.taxfiling.application.scheduling;

import com.itas.taxfiling.application.usecase.obligation.RefreshCalendarProjectionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Refreshes the local calendar_period projection from tax-type-engine.
 *
 * <p>Runs (a) once at startup so a freshly-deployed service is usable
 * immediately, and (b) every Sunday at 02:00 as a safety net. The Ministry's
 * own publish events would trigger ad-hoc refreshes too once that channel
 * is wired up.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CalendarRefreshJob {

    private final RefreshCalendarProjectionUseCase refresh;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Calendar refresh — startup tick");
        refresh.execute();
    }

    @Scheduled(cron = "${itas.scheduler.calendar-refresh-cron:0 0 2 * * SUN}")
    public void run() {
        log.info("Calendar refresh — weekly tick");
        refresh.execute();
    }
}
