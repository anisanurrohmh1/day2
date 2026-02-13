package com.bankmega.authservice.service;

import java.util.concurrent.CompletableFuture;

public interface PaymentService {


    CompletableFuture<String> pay();
}
