package com.obar.bll.auth;

/**
 * Exception used to signal authentication and credential-management failures
 *
 */
public class AuthenticationException extends RuntimeException {

    /**
     * Creates a new authentication exception
     *
     * @param message error message
     */
    public AuthenticationException(String message) {
        super(message);
    }
}