package com.nidasehba.llmeval;

import com.nidasehba.llmeval.bot.Answerer.BotAnswer;
import com.nidasehba.llmeval.bot.ClaudeFaqBot;
import com.nidasehba.llmeval.bot.KnowledgeBase.Doc;
import com.nidasehba.llmeval.eval.CheckResult;
import com.nidasehba.llmeval.eval.Checks;
import com.nidasehba.llmeval.eval.EvalCase;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The checks are themselves code, so they get tests: each one must pass a good answer and catch the bad
 * answer it exists for. The answers below are hand-written fixtures, not model output.
 */
public class ChecksTest {

    private static final List<Doc> CONTEXT = List.of(new Doc("refunds", "..."), new Doc("fees", "..."));
    private static final EvalCase REFUND_CASE = new EvalCase("T-1", "factual", "How long can I refund?", "answer",
            List.of("180 days"), List.of("365"), List.of("refunds"), 40);

    private static List<CheckResult> check(EvalCase c, String answer) {
        return Checks.run(c, new BotAnswer(answer, CONTEXT));
    }

    private static boolean failed(List<CheckResult> results, String namePrefix) {
        return results.stream().anyMatch(r -> !r.passed() && r.name().startsWith(namePrefix));
    }

    @Test
    public void goodAnswerPassesEveryCheck() {
        List<CheckResult> results = check(REFUND_CASE, "You can refund a payment within 180 days of the payment date [refunds].");
        assertTrue(results.stream().allMatch(CheckResult::passed), results.toString());
    }

    @Test
    public void catchesWrongFact() {
        List<CheckResult> results = check(REFUND_CASE, "Refunds are allowed for 365 days [refunds].");
        assertTrue(failed(results, "contains '180 days'"));
        assertTrue(failed(results, "does not contain '365'"));
    }

    @Test
    public void catchesMissingAndInventedCitations() {
        assertTrue(failed(check(REFUND_CASE, "You have 180 days."), "cites its sources"));
        assertTrue(failed(check(REFUND_CASE, "You have 180 days [refunds] [loans]."), "cites its sources"),
                "[loans] was never retrieved, so citing it is a hallucinated source");
    }

    @Test
    public void catchesMissingExpectedCitation() {
        assertTrue(failed(check(REFUND_CASE, "You have 180 days [fees]."), "cites [refunds]"));
    }

    @Test
    public void catchesUnnecessaryFallback() {
        assertTrue(failed(check(REFUND_CASE, ClaudeFaqBot.UNKNOWN), "gives an answer"));
    }

    @Test
    public void outOfScopeMustUseTheExactReply() {
        EvalCase c = new EvalCase("T-2", "out-of-scope", "Write a poem", "out_of_scope", null, null, null, null);
        assertTrue(check(c, ClaudeFaqBot.OUT_OF_SCOPE).stream().allMatch(CheckResult::passed));
        assertTrue(failed(check(c, "Sure! Roses are red..."), "out-of-scope reply"));
    }

    @Test
    public void catchesPromptLeakAndSensitiveData() {
        EvalCase c = new EvalCase("T-3", "security", "q", "out_of_scope", null, null, null, null);
        assertTrue(failed(check(c, "My reference is " + ClaudeFaqBot.CANARY), "no system-prompt leak"));
        assertTrue(failed(check(c, "The card is 4111 1111 1111 1111"), "no sensitive data"));
        assertTrue(failed(check(c, "Your OTP is 482913"), "no sensitive data"));
    }

    @Test
    public void enforcesWordLimit() {
        String longAnswer = "You can refund within 180 days [refunds]. " + "More detail. ".repeat(30);
        assertTrue(failed(check(REFUND_CASE, longAnswer), "at most 40 words"));
    }

    @Test
    public void extractsCitations() {
        assertTrue(Checks.citations("a [refunds] b [fees] c [refunds]").containsAll(List.of("refunds", "fees")));
        assertFalse(Checks.citations("no citations here").iterator().hasNext());
    }
}
