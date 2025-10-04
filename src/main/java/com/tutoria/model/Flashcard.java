package com.tutoria.model;

public class Flashcard {
    
    private String frentePergunta;
    private String versoResposta;

    // CONSTRUTOR

    public Flashcard() {}

    public Flashcard(String frentePergunta, String versoResposta) {
        this.frentePergunta = frentePergunta;
        this.versoResposta = versoResposta;
    }

    // GETTERS E SETTERS 

    public String getFrentePergunta() { return frentePergunta; }
    public void setFrentePergunta(String frentePergunta) {
        this.frentePergunta = frentePergunta;
    }

    public String getVersoResposta() { return versoResposta; }
    public void setVersoResposta(String versoResposta) {
        this.versoResposta = versoResposta;
    }

}
