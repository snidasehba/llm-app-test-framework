package com.nidasehba.llmeval.eval;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Structured output from the LLM judge. */
public record Verdict(
        @JsonPropertyDescription("1-5: 5 means every claim is directly supported by the articles; 1 means mostly unsupported")
        int faithfulness,
        @JsonPropertyDescription("1-5: 5 means it fully and directly answers the question that was asked")
        int relevance,
        @JsonPropertyDescription("Claims in the answer that the articles do not support; empty if none")
        List<String> unsupportedClaims,
        @JsonPropertyDescription("One or two sentences explaining the scores")
        String reasoning) {

    public static final int PASS_MARK = 4;

    public boolean passed() {
        return faithfulness >= PASS_MARK && relevance >= PASS_MARK;
    }
}
