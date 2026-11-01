package com.nidasehba.llmeval.bot;

import com.nidasehba.llmeval.bot.KnowledgeBase.Doc;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TF-IDF keyword retrieval over the knowledge base. Deliberately simple and deterministic, so the
 * retrieval step can be tested on its own with precise metrics before any model is involved.
 */
public class Retriever {

    private static final Set<String> STOPWORDS = Set.of("a", "an", "the", "is", "are", "to", "of", "in", "on",
            "for", "and", "or", "my", "i", "can", "do", "does", "how", "what", "when", "it", "be", "with",
            "you", "your", "from", "if", "will", "much", "many", "long", "there", "any", "me", "at", "by");

    private final List<Doc> docs;
    private final Map<String, Map<String, Integer>> termCounts = new HashMap<>();
    private final Map<String, Double> idf = new HashMap<>();

    public Retriever(List<Doc> docs) {
        this.docs = docs;
        Map<String, Integer> docFreq = new HashMap<>();
        for (Doc d : docs) {
            Map<String, Integer> counts = new HashMap<>();
            for (String t : tokens(d.text())) {
                counts.merge(t, 1, Integer::sum);
            }
            termCounts.put(d.id(), counts);
            counts.keySet().forEach(t -> docFreq.merge(t, 1, Integer::sum));
        }
        docFreq.forEach((t, df) -> idf.put(t, Math.log(1.0 + (double) docs.size() / df)));
    }

    /** The k best-matching articles, best first. Articles with no shared terms are never returned. */
    public List<Doc> search(String query, int k) {
        List<String> q = tokens(query);
        Map<Doc, Double> scores = new HashMap<>();
        for (Doc d : docs) {
            Map<String, Integer> counts = termCounts.get(d.id());
            double score = 0;
            for (String t : q) {
                score += counts.getOrDefault(t, 0) * idf.getOrDefault(t, 0.0);
            }
            if (score > 0) {
                scores.put(d, score);
            }
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Doc, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(e -> e.getKey().id()))
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    static List<String> tokens(String text) {
        return java.util.Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(t -> t.length() > 1 && !STOPWORDS.contains(t))
                .map(Retriever::stem)
                .collect(Collectors.toList());
    }

    /** Crude plural stemming so "refunds" matches "refund". */
    private static String stem(String t) {
        if (t.length() > 4 && t.endsWith("ies")) {
            return t.substring(0, t.length() - 3) + "y";
        }
        if (t.length() > 3 && t.endsWith("s") && !t.endsWith("ss")) {
            return t.substring(0, t.length() - 1);
        }
        return t;
    }
}
