package com.nidasehba.llmeval.eval;

import java.util.List;

/**
 * One test case for the bot, loaded from JSON. Expectations are optional; only the ones present are checked.
 *
 * @param expect one of "answer", "unknown" (the KB has no answer) or "out_of_scope"
 */
public record EvalCase(
        String id,
        String category,
        String question,
        String expect,
        List<String> mustContain,
        List<String> mustNotContain,
        List<String> expectedDocs,
        Integer maxWords) {

    public List<String> mustContain() {
        return mustContain == null ? List.of() : mustContain;
    }

    public List<String> mustNotContain() {
        return mustNotContain == null ? List.of() : mustNotContain;
    }

    public List<String> expectedDocs() {
        return expectedDocs == null ? List.of() : expectedDocs;
    }

    public String expect() {
        return expect == null ? "answer" : expect;
    }
}
