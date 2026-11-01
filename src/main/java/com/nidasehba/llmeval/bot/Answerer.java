package com.nidasehba.llmeval.bot;

import java.util.List;

/** Anything that answers a customer question: the real bot, or a fake in unit tests. */
public interface Answerer {

    /** The answer text plus the ids and text of the articles that were given to the model. */
    record BotAnswer(String text, List<KnowledgeBase.Doc> context) {
        public List<String> contextIds() {
            return context.stream().map(KnowledgeBase.Doc::id).toList();
        }
    }

    BotAnswer answer(String question);
}
