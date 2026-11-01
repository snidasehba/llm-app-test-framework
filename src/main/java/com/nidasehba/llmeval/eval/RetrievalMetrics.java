package com.nidasehba.llmeval.eval;

import java.util.List;

/** Standard ranking metrics for the retrieval step. */
public final class RetrievalMetrics {

    private RetrievalMetrics() {
    }

    /** Share of the relevant articles that appear in the top k results. */
    public static double recallAtK(List<String> ranked, List<String> relevant, int k) {
        if (relevant.isEmpty()) {
            return 1.0;
        }
        List<String> top = ranked.subList(0, Math.min(k, ranked.size()));
        return (double) relevant.stream().filter(top::contains).count() / relevant.size();
    }

    /** 1 / rank of the first relevant article, or 0 if none was retrieved. */
    public static double reciprocalRank(List<String> ranked, List<String> relevant) {
        for (int i = 0; i < ranked.size(); i++) {
            if (relevant.contains(ranked.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }
}
