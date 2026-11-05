package br.edu.fatec.rp.powerplus.domain.exception;

public class NaoAutorizadoException extends RuntimeException {
    public NaoAutorizadoException(String message) {
        super(message);
    }
}
