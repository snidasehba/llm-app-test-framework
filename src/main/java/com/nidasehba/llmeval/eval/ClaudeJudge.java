package com.nidasehba.llmeval.eval;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.nidasehba.llmeval.bot.Answerer.BotAnswer;
import com.nidasehba.llmeval.bot.KnowledgeBase.Doc;

import java.util.stream.Collectors;

/**
 * LLM-as-judge with a fixed rubric and structured output. The judge sees the same articles the bot saw, so
 * it can spot claims that were not in the source material (hallucinations).
 */
public class ClaudeJudge implements Judge {

    static final String RUBRIC = String.join("\n",
            "You are grading a customer-support answer for a fintech app. Score strictly.",
            "Faithfulness: is every factual claim (numbers, time limits, fees, steps) supported by the articles?",
            "A single invented or changed number caps faithfulness at 2.",
            "Relevance: does the answer address the actual question without padding or unrelated advice?",
            "Citations in [brackets] are expected and are not claims.");

    private final AnthropicClient client;
    private final String model;

    public ClaudeJudge(AnthropicClient client, String model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public Verdict grade(String question, BotAnswer answer) {
        String articles = answer.context().stream()
                .map(Doc::text)
                .collect(Collectors.joining("\n---\n"));
        StructuredMessageCreateParams<Verdict> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(4000L)
                .system(RUBRIC)
                .outputConfig(Verdict.class)
                .addUserMessage("<articles>\n" + articles + "\n</articles>\n<question>" + question
                        + "</question>\n<answer>" + answer.text() + "</answer>")
                .build();
        return client.messages().create(params).content().stream()
                .flatMap(block -> block.text().stream())
                .map(typed -> typed.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Judge returned no verdict"));
    }
}
