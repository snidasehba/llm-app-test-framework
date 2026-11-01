package com.nidasehba.llmeval.eval;

import com.nidasehba.llmeval.bot.Answerer.BotAnswer;

/** Grades qualities that string checks can't: whether the answer is faithful to the articles and on point. */
public interface Judge {

    Verdict grade(String question, BotAnswer answer);
}
