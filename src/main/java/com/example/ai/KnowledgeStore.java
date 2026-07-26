package com.example.ai;

import java.util.List;

public interface KnowledgeStore {
    void add(String statement);
    List<String> all();
}
