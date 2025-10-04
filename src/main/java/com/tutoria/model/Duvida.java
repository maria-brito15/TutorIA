package com.tutoria.model;

public class Duvida {

    private String questao;
    private String resposta;

    // CONSTRUTOR

    public Duvida() {}

    public Duvida(String questao, String resposta) {
        this.questao = questao;
        this.resposta = resposta;
    }

    // GETTERS E SETTERS

    public String getQuestao() { return questao; }
    public void setQuestao(String questao) {
        this.questao = questao;
    }

    public String getResposta() { return resposta; }
    public void setResposta(String resposta) {
        this.resposta = resposta;
    }
    
}