package br.edu.fatec.rp.powerplus.domain;

import org.springframework.http.HttpStatus;

/**
 * Exceção única para representar erros de regra de negócio, conflitos,
 * itens não encontrados ou falta de login na API.
 */
public class RegraNegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public RegraNegocioException(String mensagem) {
        this(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_NEGOCIO", mensagem);
    }

    public RegraNegocioException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public static RegraNegocioException conflito(String mensagem) {
        return new RegraNegocioException(HttpStatus.CONFLICT, "CONFLITO_NEGOCIO", mensagem);
    }

    public static RegraNegocioException naoEncontrado(String mensagem) {
        return new RegraNegocioException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", mensagem);
    }

    public static RegraNegocioException naoAutorizado(String mensagem) {
        return new RegraNegocioException(HttpStatus.UNAUTHORIZED, "NAO_AUTORIZADO", mensagem);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
