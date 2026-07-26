package com.example.ai;

import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class InMemoryKnowledgeStore implements KnowledgeStore {

    private final List<String> statements = new CopyOnWriteArrayList<>();

    @Override
    public void add(String statement) {
        statements.add(statement);
    }

    @Override
    public List<String> all() {
        return List.copyOf(statements);
    }
}
