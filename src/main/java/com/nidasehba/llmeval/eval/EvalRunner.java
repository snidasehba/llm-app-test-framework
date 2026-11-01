package com.nidasehba.llmeval.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nidasehba.llmeval.bot.Answerer;
import com.nidasehba.llmeval.bot.Answerer.BotAnswer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Runs every case through the bot, applies the checks and (for answerable questions) the judge. */
public class EvalRunner {

    public record CaseResult(EvalCase evalCase, BotAnswer answer, List<CheckResult> checks, Optional<Verdict> verdict) {
        public boolean passed() {
            return checks.stream().allMatch(CheckResult::passed) && verdict.map(Verdict::passed).orElse(true);
        }
    }

    private final Answerer bot;
    private final Judge judge;

    /** @param judge may be null to run deterministic checks only */
    public EvalRunner(Answerer bot, Judge judge) {
        this.bot = bot;
        this.judge = judge;
    }

    public List<CaseResult> run(List<EvalCase> cases) {
        List<CaseResult> results = new ArrayList<>();
        for (EvalCase c : cases) {
            BotAnswer answer = bot.answer(c.question());
            List<CheckResult> checks = Checks.run(c, answer);
            Optional<Verdict> verdict = judge != null && c.expect().equals("answer")
                    ? Optional.of(judge.grade(c.question(), answer))
                    : Optional.empty();
            results.add(new CaseResult(c, answer, checks, verdict));
        }
        return results;
    }

    public static List<EvalCase> loadCases(String resource) throws IOException {
        try (InputStream in = EvalRunner.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Missing eval file " + resource);
            }
            return new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                    .readValue(in, new TypeReference<List<EvalCase>>() { });
        }
    }
}
