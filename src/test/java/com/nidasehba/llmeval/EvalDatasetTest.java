package com.nidasehba.llmeval;

import com.nidasehba.llmeval.bot.KnowledgeBase;
import com.nidasehba.llmeval.eval.EvalCase;
import com.nidasehba.llmeval.eval.EvalRunner;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** A broken eval set silently measures the wrong thing, so the dataset is validated like code. */
public class EvalDatasetTest {

    @Test
    public void everyCaseIsWellFormed() throws Exception {
        List<EvalCase> cases = EvalRunner.loadCases("evals/payflow-evals.json");
        Set<String> docIds = new HashSet<>(KnowledgeBase.load().stream().map(KnowledgeBase.Doc::id).toList());
        Set<String> ids = new HashSet<>();

        for (EvalCase c : cases) {
            assertTrue(ids.add(c.id()), "duplicate id " + c.id());
            assertTrue(Set.of("answer", "unknown", "out_of_scope").contains(c.expect()), c.id() + " expect");
            assertTrue(docIds.containsAll(c.expectedDocs()), c.id() + " refers to a missing article");
            if (c.expect().equals("answer")) {
                assertTrue(!c.expectedDocs().isEmpty(), c.id() + ": answerable cases need expectedDocs");
            }
        }
    }

    @Test
    public void coversTheRiskCategories() throws Exception {
        Set<String> categories = new HashSet<>();
        EvalRunner.loadCases("evals/payflow-evals.json").forEach(c -> categories.add(c.category()));
        assertTrue(categories.containsAll(List.of("factual", "multi-article", "boundary", "unknown",
                "out-of-scope", "prompt-injection", "sensitive-data")), categories.toString());
    }

    @Test
    public void knowledgeBaseLoads() {
        assertEquals(KnowledgeBase.load().size(), 8);
    }
}
