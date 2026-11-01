package com.nidasehba.llmeval.eval;

import com.nidasehba.llmeval.eval.EvalRunner.CaseResult;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Turns results into a pass-rate summary per category and a Markdown report with the failure evidence. */
public final class EvalReport {

    private EvalReport() {
    }

    public static double passRate(List<CaseResult> results) {
        return results.isEmpty() ? 0 : (double) results.stream().filter(CaseResult::passed).count() / results.size();
    }

    public static Map<String, double[]> byCategory(List<CaseResult> results) {
        Map<String, double[]> out = new TreeMap<>();
        results.stream()
                .collect(Collectors.groupingBy(r -> r.evalCase().category(), TreeMap::new, Collectors.toList()))
                .forEach((cat, rs) -> out.put(cat, new double[]{rs.stream().filter(CaseResult::passed).count(), rs.size()}));
        return out;
    }

    public static String markdown(List<CaseResult> results, double threshold) {
        StringBuilder sb = new StringBuilder("# LLM evaluation report\n\n");
        double rate = passRate(results);
        sb.append(String.format("**Pass rate: %.0f%%** (%d of %d cases, threshold %.0f%%) - %s%n%n",
                rate * 100, results.stream().filter(CaseResult::passed).count(), results.size(), threshold * 100,
                rate >= threshold ? "PASS" : "FAIL"));
        sb.append("| Category | Passed |\n|---|---|\n");
        byCategory(results).forEach((cat, v) ->
                sb.append(String.format("| %s | %d / %d |%n", cat, (int) v[0], (int) v[1])));

        List<CaseResult> failures = results.stream().filter(r -> !r.passed()).toList();
        if (!failures.isEmpty()) {
            sb.append("\n## Failures\n");
            for (CaseResult r : failures) {
                sb.append("\n### ").append(r.evalCase().id()).append(" (").append(r.evalCase().category()).append(")\n");
                sb.append("**Question:** ").append(r.evalCase().question()).append("\n\n");
                sb.append("**Answer:** ").append(r.answer().text().replace("\n", " ")).append("\n\n");
                r.checks().stream().filter(c -> !c.passed()).forEach(c ->
                        sb.append("- FAILED ").append(c.name()).append(": ").append(c.detail()).append('\n'));
                r.verdict().filter(v -> !v.passed()).ifPresent(v -> sb.append(String.format(
                        "- FAILED judge: faithfulness %d, relevance %d. %s Unsupported: %s%n",
                        v.faithfulness(), v.relevance(), v.reasoning(), v.unsupportedClaims())));
            }
        }
        return sb.toString();
    }
}
