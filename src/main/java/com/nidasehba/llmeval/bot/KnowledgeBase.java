package com.nidasehba.llmeval.bot;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** The help-centre articles the bot answers from, loaded from src/main/resources/kb. */
public final class KnowledgeBase {

    public record Doc(String id, String text) {
    }

    private KnowledgeBase() {
    }

    /** Loads every article listed in kb/index.txt; the id is the file name without ".md". */
    public static List<Doc> load() {
        List<Doc> docs = new ArrayList<>();
        for (String file : read("kb/index.txt").split("\\R")) {
            if (!file.isBlank()) {
                docs.add(new Doc(file.strip().replace(".md", ""), read("kb/" + file.strip())));
            }
        }
        return docs;
    }

    static String read(String resource) {
        try (InputStream in = KnowledgeBase.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
