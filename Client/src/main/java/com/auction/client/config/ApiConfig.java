package com.auction.client.config;

public final class ApiConfig {
    private ApiConfig() {}

    public static final String BASE_URL =
            System.getProperty(
                    "auction.server.url",
                    System.getenv().getOrDefault("AUCTION_SERVER_URL", "http://192.168.42.107:8080")
            );
}
