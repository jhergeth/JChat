package com.example.ai;

import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class NoopStatementExtractor implements StatementExtractor {

    @Override
    public List<String> extract(String text) {
        return List.of();
    }
}
