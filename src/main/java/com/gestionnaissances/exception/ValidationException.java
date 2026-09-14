package com.gestionnaissances.exception;

/**
 * Exception levée lors d'une violation de contrainte métier ou de validation
 * des dossiers d'état civil (champs obligatoires manquants, doublon de registre, etc.).
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
