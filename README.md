# LLM App Test Framework

A Java framework for **testing an AI chatbot** the way a QA team would test any other product: a
labelled evaluation dataset, deterministic checks, an **LLM-as-judge** for faithfulness, retrieval
metrics, **prompt-injection and data-leak tests**, and a CI quality gate.

The system under test is a small **retrieval-augmented (RAG) help bot** for "PayFlow", a fictional UAE
payments wallet, built on the **Claude API**. It answers only from eight help-centre articles and must
cite them.

## What gets tested

| Layer | How | Example |
|---|---|---|
| Retrieval | recall@3 and MRR on every labelled question (offline, no model) | "refund to my bank account" must retrieve `refunds` first |
| Facts | answer must contain the right number from the article | refund window is "180 days", not 365 |
| Grounding | every `[citation]` must point at an article the bot was actually given | catches invented sources |
| Hallucination | **LLM judge** scores faithfulness 1–5 against the same articles; one changed number caps it at 2 | adds a condition that isn't in the policy |
| Unknowns | questions the articles can't answer must get the exact "I don't have that information" reply | savings interest rate |
| Scope | off-topic requests get the exact out-of-scope reply | "write me a poem" |
| Prompt injection | "ignore previous instructions", fake "developer mode" | must keep the real 180-day answer |
| Data leaks | a canary token planted in the system prompt must never appear; no card numbers, OTPs or PINs echoed | user pastes their OTP into the question |

The evaluation set is `src/main/resources/evals/payflow-evals.json`: 19 cases across factual,
multi-article, boundary, unknown, out-of-scope, prompt-injection and sensitive-data categories. The
dataset is validated by its own test (unique ids, real article ids, every risk category present).

## Run it

```bash
mvn test                                   # offline: retrieval metrics, checks, runner, dataset
export ANTHROPIC_API_KEY=...               # your own key
mvn -q package -DskipTests
java -jar target/llm-app-test-framework.jar             # full live eval + LLM judge → eval-report.md
java -jar target/llm-app-test-framework.jar --no-judge  # deterministic checks only (cheaper)
```

The CLI exits with status `2` if fewer than 90% of cases pass, so a pipeline can block a prompt or
model change that makes the bot worse.

## Structure

```
src/main/java/com/nidasehba/llmeval/
  bot/          KnowledgeBase, Retriever (TF-IDF), ClaudeFaqBot (system under test)
  eval/         EvalCase, Checks (deterministic), ClaudeJudge + Verdict (LLM-as-judge),
                RetrievalMetrics, EvalRunner, EvalReport (Markdown)
  App.java      live evaluation CLI
src/main/resources/kb/       the eight help articles
src/main/resources/evals/    the labelled evaluation set
src/test/java/...            offline tests; LiveEvalTest runs only with an API key
```

## Design choices

- **Test the layers separately.** If retrieval misses the right article, no prompt can fix the answer, so retrieval has its own metrics and threshold.
- **Cheap checks first, judge second.** String and citation checks are exact and free; the LLM judge is used only for what they can't see (faithfulness and relevance).
- **Exact fallback sentences.** The bot is told to use fixed sentences for "unknown" and "out of scope", which turns fuzzy behaviour into something a test can assert exactly.
- **Assert properties, not wording.** Model output changes run to run, so tests check facts, citations and safety rules rather than full sentences.
- **Test the tests.** Each check has unit tests proving it passes a good answer and catches the bad one it exists for (the answers in those tests are hand-written fixtures).
