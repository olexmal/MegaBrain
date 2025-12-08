/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

/**
 * Base exception class for MegaBrain application errors.
 * Provides consistent error handling across the application.
 */
public class MegaBrainException extends RuntimeException {

    public MegaBrainException(String message) {
        super(message);
    }

    public MegaBrainException(String message, Throwable cause) {
        super(message, cause);
    }
}
