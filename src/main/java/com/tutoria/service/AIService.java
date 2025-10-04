package com.tutoria.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tutoria.model.Flashcard;
import com.tutoria.model.Quiz;
import com.tutoria.model.QuestaoQuiz;
import com.tutoria.util.PDFReader;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AIService {

    private final String apiKey;
    private final String apiUrl;
    private final OkHttpClient client;
    private final Gson gson;
    private final PDFReader pdfReader;

    // ========== CONSTRUTOR ==========

    public AIService(String apiKey) {
        this.apiKey = apiKey;
        this.apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        this.gson = new Gson();
        this.pdfReader = new PDFReader();
    }

    // ========== CHAMADA CENTRAL À IA ==========

    private String chamarIA(String prompt, String systemPrompt) throws IOException {
        JsonObject requestBody = new JsonObject();

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("maxOutputTokens", 4096);
        requestBody.add("generationConfig", generationConfig);

        JsonArray safetySettings = new JsonArray();
        String[] categories = {
                "HARM_CATEGORY_HARASSMENT",
                "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                "HARM_CATEGORY_DANGEROUS_CONTENT"
        };

        for (String category : categories) {
            JsonObject setting = new JsonObject();
            setting.addProperty("category", category);
            setting.addProperty("threshold", "BLOCK_ONLY_HIGH");
            safetySettings.add(setting);
        }

        requestBody.add("safetySettings", safetySettings);

        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");

        String fullPrompt = (systemPrompt != null && !systemPrompt.isEmpty()) 
                ? systemPrompt + "\n\n" + prompt 
                : prompt;

        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", fullPrompt);
        parts.add(textPart);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("content-type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Sem detalhes";
                throw new IOException("Erro na API Gemini: " + response.code() + " - " + response.message() + "\nDetalhes: " + errorBody);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                JsonObject contentObj = candidate.getAsJsonObject("content");
                JsonArray partsArray = contentObj.getAsJsonArray("parts");
                if (partsArray != null && partsArray.size() > 0) {
                    return partsArray.get(0).getAsJsonObject().get("text").getAsString();
                }
            }

            throw new IOException("Resposta inválida da API Gemini");
        }
    }

    // =======================
    // MÉTODOS DE RESUMO
    // =======================

    public String resumirTexto(String textoCompleto) throws IOException {
        String systemPrompt = "Você é um assistente educacional especializado em criar resumos claros e organizados em formato Markdown. Sempre responda em português do Brasil.";

        String prompt = String.format(
                "Crie um resumo estruturado e didático do seguinte texto em formato Markdown. " +
                        "Use títulos (##), subtítulos (###), listas e destaques em negrito quando apropriado. " +
                        "Organize o conteúdo de forma lógica e fácil de estudar.\n\n" +
                        "TEXTO:\n%s\n\n" +
                        "RESUMO EM MARKDOWN:",
                textoCompleto
        );

        return chamarIA(prompt, systemPrompt);
    }

    public String resumirPDF(InputStream pdfStream) throws IOException {
        String texto = pdfReader.extrairTextoDePDF(pdfStream);
        return resumirTexto(texto);
    }

    public String resumirPDF(File pdfFile) throws IOException {
        String texto = pdfReader.extrairTextoDePDF(pdfFile);
        return resumirTexto(texto);
    }

    // =======================
    // MÉTODOS DE QUIZ
    // =======================

    public Quiz criarQuiz(String conteudo, String titulo, int numeroQuestoes) throws IOException {
        String systemPrompt = "Você é um assistente educacional que cria questões de múltipla escolha em português do Brasil. " +
                "Sempre responda APENAS com um JSON válido, sem texto adicional antes ou depois.";

        String prompt = String.format(
                "Crie %d questões de múltipla escolha sobre o seguinte conteúdo. " +
                        "IMPORTANTE: Retorne APENAS o JSON puro, sem markdown, sem ```json, sem explicações.\n\n" +
                        "Formato obrigatório:\n" +
                        "{\n" +
                        "  \"questoes\": [\n" +
                        "    {\n" +
                        "      \"pergunta\": \"texto da pergunta\",\n" +
                        "      \"opcoes\": [\"A) opção 1\", \"B) opção 2\", \"C) opção 3\", \"D) opção 4\"],\n" +
                        "      \"resposta_correta\": \"A) opção 1\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n" +
                        "CONTEÚDO:\n%s\n\n" +
                        "JSON:",
                numeroQuestoes,
                conteudo
        );

        String resposta = chamarIA(prompt, systemPrompt);

        resposta = resposta.replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        return parseQuizJSON(resposta, titulo);
    }

    public Quiz criarQuizPDF(InputStream pdfStream, String titulo, int numeroQuestoes) throws IOException {
        String texto = pdfReader.extrairTextoDePDF(pdfStream);
        return criarQuiz(texto, titulo, numeroQuestoes);
    }

    public Quiz criarQuizPDF(File pdfFile, String titulo, int numeroQuestoes) throws IOException {
        String texto = pdfReader.extrairTextoDePDF(pdfFile);
        return criarQuiz(texto, titulo, numeroQuestoes);
    }

    // =======================
    // MÉTODOS DE FLASHCARDS
    // =======================

    public List<Flashcard> criarFlashcards(String conteudo, int numeroCards) throws IOException {
        String systemPrompt = "Você é um assistente educacional que cria flashcards para estudo em português do Brasil. " +
                "Sempre responda APENAS com um JSON válido, sem texto adicional antes ou depois.";

        String prompt = String.format(
                "Crie %d flashcards sobre o seguinte conteúdo. " +
                        "IMPORTANTE: Retorne APENAS o JSON puro, sem markdown, sem ```json, sem explicações.\n\n" +
                        "Formato obrigatório:\n" +
                        "{\n" +
                        "  \"flashcards\": [\n" +
                        "    {\n" +
                        "      \"frente\": \"pergunta ou conceito\",\n" +
                        "      \"verso\": \"resposta ou explicação detalhada\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n" +
                        "CONTEÚDO:\n%s\n\n" +
                        "JSON:",
                numeroCards,
                conteudo
        );

        String resposta = chamarIA(prompt, systemPrompt);

        resposta = resposta.replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        return parseFlashcardsJSON(resposta);
    }

    public List<Flashcard> criarFlashcardsPDF(InputStream pdfStream, int numeroCards) throws IOException {
        String texto = pdfReader.extrairTextoDePDF(pdfStream);
        return criarFlashcards(texto, numeroCards);
    }

    public List<Flashcard> criarFlashcardsPDF(File pdfFile, int numeroCards) throws IOException {
        String texto = pdfReader.extrairTextoDePDF(pdfFile);
        return criarFlashcards(texto, numeroCards);
    }

    // =======================
    // MÉTODOS DE DÚVIDAS
    // =======================

    public String responderDuvida(String pergunta, String contexto) throws IOException {
        String systemPrompt = "Você é um tutor educacional paciente e didático que responde em português do Brasil. " +
                "Explique conceitos de forma clara, use exemplos quando apropriado, " +
                "e formate suas respostas em Markdown para melhor legibilidade.";

        String prompt;
        if (contexto != null && !contexto.isEmpty()) {
            prompt = String.format(
                    "Baseado no seguinte contexto, responda a pergunta do estudante de forma didática:\n\n" +
                            "CONTEXTO:\n%s\n\n" +
                            "PERGUNTA: %s\n\n" +
                            "RESPOSTA EM MARKDOWN:",
                    contexto,
                    pergunta
            );
        } else {
            prompt = String.format(
                    "Responda a seguinte pergunta de forma clara, didática e bem explicada:\n\n" +
                            "PERGUNTA: %s\n\n" +
                            "RESPOSTA EM MARKDOWN:",
                    pergunta
            );
        }

        return chamarIA(prompt, systemPrompt);
    }

    // =======================
    // MÉTODOS AUXILIARES PARA PARSE
    // =======================

    private Quiz parseQuizJSON(String jsonString, String titulo) {
        Quiz quiz = new Quiz(titulo);

        try {
            JsonObject jsonResponse = gson.fromJson(jsonString, JsonObject.class);
            JsonArray questoesArray = jsonResponse.getAsJsonArray("questoes");

            if (questoesArray == null) {
                throw new RuntimeException("JSON não contém array 'questoes'");
            }

            for (int i = 0; i < questoesArray.size(); i++) {
                JsonObject questaoObj = questoesArray.get(i).getAsJsonObject();

                String pergunta = questaoObj.get("pergunta").getAsString();

                JsonArray opcoesArray = questaoObj.getAsJsonArray("opcoes");
                List<String> opcoes = new ArrayList<>();
                for (int j = 0; j < opcoesArray.size(); j++) {
                    opcoes.add(opcoesArray.get(j).getAsString());
                }

                String respostaCorreta = questaoObj.get("resposta_correta").getAsString();

                QuestaoQuiz questao = new QuestaoQuiz(pergunta, opcoes, respostaCorreta);
                quiz.adicionarQuestao(questao);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar JSON do quiz: " + e.getMessage() + "\nJSON recebido: " + jsonString, e);
        }

        return quiz;
    }

    private List<Flashcard> parseFlashcardsJSON(String jsonString) {
        List<Flashcard> flashcards = new ArrayList<>();

        try {
            JsonObject jsonResponse = gson.fromJson(jsonString, JsonObject.class);
            JsonArray flashcardsArray = jsonResponse.getAsJsonArray("flashcards");

            if (flashcardsArray == null) {
                throw new RuntimeException("JSON não contém array 'flashcards'");
            }

            for (int i = 0; i < flashcardsArray.size(); i++) {
                JsonObject cardObj = flashcardsArray.get(i).getAsJsonObject();

                String frente = cardObj.get("frente").getAsString();
                String verso = cardObj.get("verso").getAsString();

                Flashcard card = new Flashcard(frente, verso);
                flashcards.add(card);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar JSON dos flashcards: " + e.getMessage() + "\nJSON recebido: " + jsonString, e);
        }

        return flashcards;
    }
}