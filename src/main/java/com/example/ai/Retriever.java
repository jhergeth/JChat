package com.example.ai;

import java.util.List;

public interface Retriever {
    List<String> retrieve(String query);
}
