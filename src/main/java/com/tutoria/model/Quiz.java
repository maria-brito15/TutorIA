package com.tutoria.model;

import java.util.ArrayList;
import java.util.List;

public class Quiz {

    private String titulo;
    private List<QuestaoQuiz> questoes;

    // CONSTRUTORES

    public Quiz() {
        this.questoes = new ArrayList<>();
    }
    
    public Quiz(String titulo) {
        this.titulo = titulo;
        this.questoes = new ArrayList<>();
    }

    // MÉTODOS

    public void adicionarQuestao(QuestaoQuiz questao) {
        this.questoes.add(questao);
    }
    
    public int getTotalQuestoes() {
        return this.questoes.size();
    }

    // GETTERS E SETTERS

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public List<QuestaoQuiz> getQuestoes() {
        return questoes;
    }
    
    public void setQuestoes(List<QuestaoQuiz> questoes) {
        this.questoes = questoes;
    }
}