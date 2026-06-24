package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.valueobject.CalculationOutcome;
import com.itas.taxfiling.domain.valueobject.QuestionnaireAnswers;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One run of the calculation loop on a TaxReturn (Rule 6, BUC-FIL-010..013).
 * Multiple iterations can exist; only one can be ACCEPTED.
 */
public final class CalculationIteration {

    private final UUID id;
    private final int sequence;
    private final QuestionnaireAnswers answers;
    private final Instant requestedAt;
    private CalculationOutcome outcome;
    private boolean accepted;
    private Instant completedAt;

    private CalculationIteration(UUID id, int sequence, QuestionnaireAnswers answers, Instant requestedAt) {
        this.id = id;
        this.sequence = sequence;
        this.answers = answers;
        this.requestedAt = requestedAt;
    }

    public static CalculationIteration request(int sequence, QuestionnaireAnswers answers) {
        Objects.requireNonNull(answers, "answers");
        if (sequence < 1) throw new IllegalArgumentException("sequence must be >= 1");
        return new CalculationIteration(UUID.randomUUID(), sequence, answers, Instant.now());
    }

    public void complete(CalculationOutcome outcome) {
        if (this.outcome != null) throw new IllegalStateException("iteration already completed");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.completedAt = Instant.now();
    }

    public void accept() {
        if (outcome == null) throw new IllegalStateException("cannot accept before completion");
        this.accepted = true;
    }

    public UUID getId() { return id; }
    public int getSequence() { return sequence; }
    public QuestionnaireAnswers getAnswers() { return answers; }
    public Instant getRequestedAt() { return requestedAt; }
    public CalculationOutcome getOutcome() { return outcome; }
    public boolean isAccepted() { return accepted; }
    public Instant getCompletedAt() { return completedAt; }

    public static CalculationIteration rehydrate(UUID id, int sequence, QuestionnaireAnswers answers,
                                                 Instant requestedAt, CalculationOutcome outcome,
                                                 boolean accepted, Instant completedAt) {
        CalculationIteration it = new CalculationIteration(id, sequence, answers, requestedAt);
        it.outcome = outcome;
        it.accepted = accepted;
        it.completedAt = completedAt;
        return it;
    }
}
