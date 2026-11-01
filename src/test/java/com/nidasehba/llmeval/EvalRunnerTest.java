package com.nidasehba.llmeval;

import com.nidasehba.llmeval.bot.Answerer;
import com.nidasehba.llmeval.bot.Answerer.BotAnswer;
import com.nidasehba.llmeval.bot.KnowledgeBase.Doc;
import com.nidasehba.llmeval.eval.EvalCase;
import com.nidasehba.llmeval.eval.EvalReport;
import com.nidasehba.llmeval.eval.EvalRunner;
import com.nidasehba.llmeval.eval.EvalRunner.CaseResult;
import com.nidasehba.llmeval.eval.Verdict;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** Exercises the runner, judge wiring and report with a scripted bot, so no API calls are made. */
public class EvalRunnerTest {

    private static final List<EvalCase> CASES = List.of(
            new EvalCase("A", "factual", "refund window?", "answer", List.of("180 days"), null, List.of("refunds"), null),
            new EvalCase("B", "factual", "card fee?", "answer", List.of("1.5%"), null, List.of("fees"), null));

    private static final Answerer SCRIPTED_BOT = question -> question.startsWith("refund")
            ? new BotAnswer("Within 180 days [refunds].", List.of(new Doc("refunds", "...")))
            : new BotAnswer("It costs 2% [fees].", List.of(new Doc("fees", "...")));

    @Test
    public void reportsPassRateAndFailureEvidence() {
        List<CaseResult> results = new EvalRunner(SCRIPTED_BOT, null).run(CASES);

        assertTrue(results.get(0).passed());
        assertFalse(results.get(1).passed());
        assertEquals(EvalReport.passRate(results), 0.5);

        String report = EvalReport.markdown(results, 0.9);
        assertTrue(report.contains("**Pass rate: 50%**"));
        assertTrue(report.contains("- FAIL"));
        assertTrue(report.contains("### B (factual)"));
        assertTrue(report.contains("FAILED contains '1.5%': missing from answer"));
    }

    @Test
    public void judgeVerdictCanFailAnAnswerThatPassesStringChecks() {
        List<CaseResult> results = new EvalRunner(SCRIPTED_BOT,
                (q, a) -> new Verdict(2, 5, List.of("invented condition"), "Adds a condition not in the article."))
                .run(CASES.subList(0, 1));

        assertTrue(results.get(0).checks().stream().allMatch(c -> c.passed()));
        assertFalse(results.get(0).passed(), "a low faithfulness score fails the case");
        assertTrue(EvalReport.markdown(results, 0.9).contains("faithfulness 2"));
    }
}
