package com.MM_CONNECT.api.admin.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Login failures commit account attempt counters, never credential material. */
public class StaffAuthFailure extends ResponseStatusException {
    public StaffAuthFailure() { super(HttpStatus.UNAUTHORIZED, "Invalid staff username or password."); }
}
