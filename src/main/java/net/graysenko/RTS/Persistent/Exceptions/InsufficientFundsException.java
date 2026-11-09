package net.graysenko.RTS.Persistent.Exceptions;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() {}
    public InsufficientFundsException(String message) {
        super(message);
    }
    public InsufficientFundsException(String message, Throwable cause) {super(message, cause);}
}
