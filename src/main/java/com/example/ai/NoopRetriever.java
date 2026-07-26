package com.example.ai;

import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class NoopRetriever implements Retriever {

    @Override
    public List<String> retrieve(String query) {
        return List.of();
    }
}
