package com.nkeanyi.knowledge.generation;

public record OllamaChatRequest(
        String model,
        String prompt,
        boolean stream
) {}
