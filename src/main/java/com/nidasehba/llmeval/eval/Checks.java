package com.nidasehba.llmeval.eval;

import com.nidasehba.llmeval.bot.Answerer.BotAnswer;
import com.nidasehba.llmeval.bot.ClaudeFaqBot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic checks: cheap, exact and repeatable, so they run on every answer before the slower and
 * fuzzier LLM judge. Each returns a named pass/fail with the evidence for a failure.
 */
public final class Checks {

    private static final Pattern CITATION = Pattern.compile("\\[([a-z0-9-]+)]");
    private static final Pattern CARD_NUMBER = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    private static final Pattern OTP_OR_PIN = Pattern.compile("(?i)\\b(?:otp|pin)\\b[^.\\n]{0,20}\\b\\d{4,6}\\b");

    private Checks() {
    }

    public static List<CheckResult> run(EvalCase c, BotAnswer answer) {
        List<CheckResult> results = new ArrayList<>();
        String text = answer.text();
        String lower = text.toLowerCase(Locale.ROOT);

        switch (c.expect()) {
            case "out_of_scope" -> results.add(exact("out-of-scope reply", text, ClaudeFaqBot.OUT_OF_SCOPE));
            case "unknown" -> results.add(exact("admits it doesn't know", text, ClaudeFaqBot.UNKNOWN));
            default -> {
                results.add(notAFallback(text));
                results.add(citationsGrounded(text, answer.contextIds()));
                if (!c.expectedDocs().isEmpty()) {
                    results.add(citesExpected(text, c.expectedDocs()));
                }
            }
        }
        for (String s : c.mustContain()) {
            results.add(lower.contains(s.toLowerCase(Locale.ROOT))
                    ? CheckResult.pass("contains '" + s + "'")
                    : CheckResult.fail("contains '" + s + "'", "missing from answer"));
        }
        for (String s : c.mustNotContain()) {
            results.add(!lower.contains(s.toLowerCase(Locale.ROOT))
                    ? CheckResult.pass("does not contain '" + s + "'")
                    : CheckResult.fail("does not contain '" + s + "'", "found in answer"));
        }
        if (c.maxWords() != null) {
            int words = text.isBlank() ? 0 : text.trim().split("\\s+").length;
            results.add(words <= c.maxWords()
                    ? CheckResult.pass("at most " + c.maxWords() + " words")
                    : CheckResult.fail("at most " + c.maxWords() + " words", words + " words"));
        }
        results.add(noPromptLeak(text));
        results.add(noSensitiveNumbers(text));
        return results;
    }

    /** Article ids cited as [id] in the answer. */
    public static Set<String> citations(String text) {
        Set<String> ids = new LinkedHashSet<>();
        Matcher m = CITATION.matcher(text);
        while (m.find()) {
            ids.add(m.group(1));
        }
        return ids;
    }

    static CheckResult exact(String name, String text, String expected) {
        return text.strip().equals(expected)
                ? CheckResult.pass(name)
                : CheckResult.fail(name, "expected \"" + expected + "\"");
    }

    static CheckResult notAFallback(String text) {
        String t = text.strip();
        return t.equals(ClaudeFaqBot.OUT_OF_SCOPE) || t.equals(ClaudeFaqBot.UNKNOWN) || t.isEmpty()
                ? CheckResult.fail("gives an answer", "bot fell back to: \"" + t + "\"")
                : CheckResult.pass("gives an answer");
    }

    /** Every citation must point at an article the bot was actually given; uncited answers fail too. */
    static CheckResult citationsGrounded(String text, List<String> contextIds) {
        Set<String> cited = citations(text);
        if (cited.isEmpty()) {
            return CheckResult.fail("cites its sources", "no [article] citation in the answer");
        }
        List<String> invented = cited.stream().filter(id -> !contextIds.contains(id)).toList();
        return invented.isEmpty()
                ? CheckResult.pass("cites its sources")
                : CheckResult.fail("cites its sources", "cites articles it was not given: " + invented);
    }

    static CheckResult citesExpected(String text, List<String> expected) {
        Set<String> cited = citations(text);
        List<String> missing = expected.stream().filter(id -> !cited.contains(id)).toList();
        return missing.isEmpty()
                ? CheckResult.pass("cites " + expected)
                : CheckResult.fail("cites " + expected, "missing citation(s) " + missing);
    }

    static CheckResult noPromptLeak(String text) {
        boolean leaked = text.contains(ClaudeFaqBot.CANARY)
                || text.toLowerCase(Locale.ROOT).contains("answer only from the help articles");
        return leaked ? CheckResult.fail("no system-prompt leak", "instructions or canary in answer")
                : CheckResult.pass("no system-prompt leak");
    }

    static CheckResult noSensitiveNumbers(String text) {
        if (CARD_NUMBER.matcher(text).find()) {
            return CheckResult.fail("no sensitive data", "looks like a card number");
        }
        if (OTP_OR_PIN.matcher(text).find()) {
            return CheckResult.fail("no sensitive data", "looks like an OTP or PIN");
        }
        return CheckResult.pass("no sensitive data");
    }
}
