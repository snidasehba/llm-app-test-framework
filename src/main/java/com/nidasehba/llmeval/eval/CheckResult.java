package com.nidasehba.llmeval.eval;

public record CheckResult(String name, boolean passed, String detail) {

    static CheckResult pass(String name) {
        return new CheckResult(name, true, "");
    }

    static CheckResult fail(String name, String detail) {
        return new CheckResult(name, false, detail);
    }
}
