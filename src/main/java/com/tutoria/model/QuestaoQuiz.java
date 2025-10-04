package com.tutoria.model;

import java.util.List;

public class QuestaoQuiz  {

    private String textoPergunta;
    private List<String> opcoesResposta;
    private String respostaCorreta;

    // CONSTRUTORES

    public QuestaoQuiz () {}

    public QuestaoQuiz (String textoPergunta, List<String> opcoesResposta, String respostaCorreta) {
        this.textoPergunta = textoPergunta;
        this.opcoesResposta = opcoesResposta;
        this.respostaCorreta = respostaCorreta;
    }

    // VALIDAÇÃO

    public boolean verificarResposta(String respostaUsuario) {
        return this.respostaCorreta.trim().equalsIgnoreCase(respostaUsuario.trim());
    }

    // GETTERS E SETTERS

    public String getTextoPergunta() { return textoPergunta; }
    public void setTextoPergunta(String textoPergunta) {
        this.textoPergunta = textoPergunta;
    }

    public List<String> getOpcoesResposta() { return opcoesResposta; }
    public void setOpcoesResposta(List<String> opcoesResposta) {
        this.opcoesResposta = opcoesResposta;
    }

    public String getRespostaCorreta() { return respostaCorreta; }
    public void setRespostaCorreta(String respostaCorreta) {
        this.respostaCorreta = respostaCorreta;
    }
}
