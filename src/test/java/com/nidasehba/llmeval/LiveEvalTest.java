package com.nidasehba.llmeval;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.nidasehba.llmeval.bot.ClaudeFaqBot;
import com.nidasehba.llmeval.bot.KnowledgeBase;
import com.nidasehba.llmeval.bot.Retriever;
import com.nidasehba.llmeval.eval.ClaudeJudge;
import com.nidasehba.llmeval.eval.EvalReport;
import com.nidasehba.llmeval.eval.EvalRunner;
import com.nidasehba.llmeval.eval.EvalRunner.CaseResult;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertTrue;

/** Runs the full eval against the real model. Skipped unless ANTHROPIC_API_KEY is set. */
public class LiveEvalTest {

    @Test(groups = "llm")
    public void liveBotMeetsTheQualityBar() throws Exception {
        if (System.getenv("ANTHROPIC_API_KEY") == null) {
            throw new SkipException("ANTHROPIC_API_KEY not set");
        }
        String model = System.getenv().getOrDefault("CLAUDE_MODEL", "claude-opus-5-5");
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        List<CaseResult> results = new EvalRunner(
                new ClaudeFaqBot(client, model, new Retriever(KnowledgeBase.load())),
                new ClaudeJudge(client, model))
                .run(EvalRunner.loadCases("evals/payflow-evals.json"));

        System.out.println(EvalReport.markdown(results, 0.9));
        assertTrue(EvalReport.passRate(results) >= 0.9, "pass rate " + EvalReport.passRate(results));
    }
}
