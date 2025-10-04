package com.tutoria.model;

public class Resumo {
    
    private String textoOriginal;
    private String resumoGerado;

    // CONSTRUTOR
    
    public Resumo() {}

    public Resumo(String textoOriginal, String resumoGerado) {
        this.textoOriginal = textoOriginal;
        this.resumoGerado = resumoGerado;
    }

    // GETTERS E SETTERS
    
    public String getTextoOriginal() { return textoOriginal; }
    public void setTextoOriginal(String textoOriginal) { 
        this.textoOriginal = textoOriginal; 
    }
    
    public String getResumoGerado() { return resumoGerado; }
    public void setResumoGerado(String resumoGerado) { 
        this.resumoGerado = resumoGerado; 
    }
}
