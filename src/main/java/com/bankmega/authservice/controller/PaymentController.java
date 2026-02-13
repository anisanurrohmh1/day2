package com.bankmega.authservice.controller;

import com.bankmega.authservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {


   private final PaymentService paymentService;


   @GetMapping()
    public CompletableFuture<String> paymentAction (){
       return paymentService.pay();
   }
}
