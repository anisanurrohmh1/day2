package com.bankmega.authservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class PaymentServiceImpl implements PaymentService{

    @RateLimiter(name = "paymentService",  fallbackMethod = "rateLimitPaymentService" )
    @TimeLimiter(name = "paymentService")
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackPaymentService")
    public CompletableFuture<String> pay() {
        return CompletableFuture.supplyAsync(()-> {
            if(4>5){
                throw  new RuntimeException();
            }
            return "PAYMENT SUCCESS";
        });
    }

    public CompletableFuture<String> fallbackPaymentService(Throwable tr){
        return CompletableFuture.completedFuture("PAYMENT SERVICE CAN NOT ACCESS");
    }

    public CompletableFuture<String> rateLimitPaymentService(RequestNotPermitted rp){
        return CompletableFuture.completedFuture("RATE LIMIT IS OVER");
    }
}
