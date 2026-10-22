package com.utplay.exception;

public class ExcepcionIntentos extends RuntimeException {
    private final long segundosParaReintentar;
    public ExcepcionIntentos(String mensaje, long segundosParaReintentar) {
        super(mensaje);
        this.segundosParaReintentar = segundosParaReintentar;;
    }
    public long getSegundosParaReintentar() {
        return segundosParaReintentar;
    }
    
}
