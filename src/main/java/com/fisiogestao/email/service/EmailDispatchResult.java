package com.fisiogestao.email.service;

public record EmailDispatchResult(
        boolean sent,
        String message
) {

    public static EmailDispatchResult success(String message) {
        return new EmailDispatchResult(true, message);
    }

    public static EmailDispatchResult failure(String message) {
        return new EmailDispatchResult(false, message);
    }
}
