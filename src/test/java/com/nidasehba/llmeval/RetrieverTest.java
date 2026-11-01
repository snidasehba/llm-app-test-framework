package com.nidasehba.llmeval;

import com.nidasehba.llmeval.bot.KnowledgeBase;
import com.nidasehba.llmeval.bot.Retriever;
import com.nidasehba.llmeval.eval.EvalCase;
import com.nidasehba.llmeval.eval.EvalRunner;
import com.nidasehba.llmeval.eval.RetrievalMetrics;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests retrieval on its own. If the right article never reaches the model, no prompt can fix the answer,
 * so this layer gets its own quality bar.
 */
public class RetrieverTest {

    private final Retriever retriever = new Retriever(KnowledgeBase.load());

    private List<String> top3(String question) {
        return retriever.search(question, 3).stream().map(KnowledgeBase.Doc::id).toList();
    }

    @Test
    public void meetsRecallAndMrrTargetsOnTheLabelledQuestions() throws Exception {
        List<EvalCase> labelled = EvalRunner.loadCases("evals/payflow-evals.json").stream()
                .filter(c -> !c.expectedDocs().isEmpty()).toList();
        double recall = 0;
        double mrr = 0;
        for (EvalCase c : labelled) {
            List<String> ranked = top3(c.question());
            recall += RetrievalMetrics.recallAtK(ranked, c.expectedDocs(), 3);
            mrr += RetrievalMetrics.reciprocalRank(ranked, c.expectedDocs());
        }
        recall /= labelled.size();
        mrr /= labelled.size();
        System.out.printf("Retrieval over %d labelled questions: recall@3 = %.2f, MRR = %.2f%n", labelled.size(), recall, mrr);
        assertTrue(recall >= 0.9, "recall@3 " + recall);
        assertTrue(mrr >= 0.8, "MRR " + mrr);
    }

    @Test
    public void bestMatchIsTheObviousArticle() {
        assertEquals(top3("refund to my bank account").get(0), "refunds");
        assertEquals(top3("international transfer fee").get(0), "fees");
        assertEquals(top3("lost phone freeze account").get(0), "account-security");
    }

    @Test
    public void unrelatedQuestionRetrievesNothing() {
        assertTrue(top3("poem about the skyline").isEmpty());
    }

    @Test
    public void metricsMatchHandCalculations() {
        assertEquals(RetrievalMetrics.recallAtK(List.of("a", "b", "c"), List.of("b", "d"), 3), 0.5);
        assertEquals(RetrievalMetrics.reciprocalRank(List.of("a", "b", "c"), List.of("c")), 1.0 / 3);
        assertEquals(RetrievalMetrics.reciprocalRank(List.of("a"), List.of("z")), 0.0);
    }
}
