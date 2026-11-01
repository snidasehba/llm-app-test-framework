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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs the full evaluation against the live bot and writes eval-report.md.
 * Usage: java -jar llm-app-test-framework.jar [--no-judge]
 * Exits with status 2 when the pass rate is below the threshold, so CI can block a bad prompt change.
 */
public final class App {

    static final double THRESHOLD = 0.9;

    private App() {
    }

    public static void main(String[] args) throws IOException {
        String model = System.getenv().getOrDefault("CLAUDE_MODEL", "claude-opus-5-5");
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        ClaudeFaqBot bot = new ClaudeFaqBot(client, model, new Retriever(KnowledgeBase.load()));
        boolean useJudge = !List.of(args).contains("--no-judge");

        List<CaseResult> results = new EvalRunner(bot, useJudge ? new ClaudeJudge(client, model) : null)
                .run(EvalRunner.loadCases("evals/payflow-evals.json"));
        String report = EvalReport.markdown(results, THRESHOLD);
        Files.writeString(Path.of("eval-report.md"), report);
        System.out.println(report);
        System.exit(EvalReport.passRate(results) >= THRESHOLD ? 0 : 2);
    }
}
