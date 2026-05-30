package com.auction.client.config;

public final class ApiConfig {
    private ApiConfig() {}

    public static final String BASE_URL =
            System.getProperty("auction.server.url", "http://localhost:8080");
}