package com.tutoria.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.tutoria.model.Flashcard;
import com.tutoria.model.Quiz;
import com.tutoria.service.AIService;
import com.tutoria.util.PDFReader;

import spark.Request;
import spark.Response;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.notFound;
import static spark.Spark.post;

public class AIController {

    private final AIService aiService;
    private final PDFReader pdfReader;
    private final Gson gson;
    
    private static final int MAX_FILE_SIZE = 25 * 1024 * 1024;
    private static final int MIN_QUESTOES = 1;
    private static final int MAX_QUESTOES = 20;
    private static final int MIN_FLASHCARDS = 1;
    private static final int MAX_FLASHCARDS = 50;
    private static final int MAX_PERGUNTA_LENGTH = 1000;
    private static final int MAX_CONTEXTO_LENGTH = 50000;
    private static final int MAX_TEXTO_LENGTH = 100000;

    public AIController(AIService aiService) {
        this.aiService = aiService;
        this.pdfReader = new PDFReader();
        this.gson = new Gson();
    }

    public void configurarRotas() {
                
        // ========== ROTAS DE RESUMO ==========

        // 1. POST /api/ai/resumir/pdf - Resumir PDF
        post("/api/ai/resumir/pdf", (req, res) -> resumirPDF(req, res));
        
        // 2. POST /api/ai/resumir/texto - Resumir texto direto
        post("/api/ai/resumir/texto", (req, res) -> resumirTexto(req, res));
        
        // ========== ROTAS DE QUIZ ==========

        // 3. POST /api/ai/quiz/pdf - Criar Quiz de PDF
        post("/api/ai/quiz/pdf", (req, res) -> criarQuizPDF(req, res));
        
        // 4. POST /api/ai/quiz/texto - Criar Quiz de texto
        post("/api/ai/quiz/texto", (req, res) -> criarQuizTexto(req, res));
        
        // ========== ROTAS DE FLASHCARDS ==========

        // 5. POST /api/ai/flashcards/pdf - Criar Flashcards de PDF
        post("/api/ai/flashcards/pdf", (req, res) -> criarFlashcardsPDF(req, res));
        
        // 6. POST /api/ai/flashcards/texto - Criar Flashcards de texto
        post("/api/ai/flashcards/texto", (req, res) -> criarFlashcardsTexto(req, res));
        
        // ========== ROTAS DE PERGUNTAS ==========

        // 7. POST /api/ai/perguntar - Responder dúvida simples (sem contexto)
        post("/api/ai/perguntar", (req, res) -> responderDuvidaSimples(req, res));
        
        // 8. POST /api/ai/perguntar/contexto - Responder dúvida com contexto de texto
        post("/api/ai/perguntar/contexto", (req, res) -> responderDuvidaComContexto(req, res));
        
        // 9. POST /api/ai/perguntar/pdf - Responder dúvida com contexto de PDF
        post("/api/ai/perguntar/pdf", (req, res) -> responderDuvidaComPDF(req, res));
        
        // ========== ROTAS AUXILIARES ==========

        // 10. GET /api/ai/health - Health check
        get("/api/ai/health", (req, res) -> healthCheck(req, res));
        
        exception(IllegalArgumentException.class, (e, req, res) -> {
            res.status(400);
            res.type("application/json");
            res.body(gson.toJson(criarErro(e.getMessage())));
        });
        
        exception(JsonSyntaxException.class, (e, req, res) -> {
            res.status(400);
            res.type("application/json");
            res.body(gson.toJson(criarErro("JSON inválido: " + e.getMessage())));
        });
        
        exception(IOException.class, (e, req, res) -> {
            res.status(503);
            res.type("application/json");
            res.body(gson.toJson(criarErro("Erro ao comunicar com serviço de IA: " + e.getMessage())));
        });
        
        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.type("application/json");
            
            System.err.println("Erro interno: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject erro = new JsonObject();
            erro.addProperty("erro", "Erro interno do servidor");
            erro.addProperty("mensagem", e.getMessage());
            res.body(gson.toJson(erro));
        });
        
        notFound((req, res) -> {
            res.type("application/json");
            return gson.toJson(criarErro("Rota não encontrada: " + req.pathInfo()));
        });
    }
    
    private String healthCheck(Request req, Response res) {
        res.type("application/json");
        JsonObject response = new JsonObject();
        response.addProperty("status", "ok");
        response.addProperty("service", "AI Service");
        response.addProperty("timestamp", System.currentTimeMillis());
        return gson.toJson(response);
    }

    // ========================================
    // MÉTODOS DE RESUMO
    // ========================================

    // 1. RESUMIR PDF
    private String resumirPDF(Request req, Response res) throws IOException, ServletException {
        res.type("application/json");
        
        req.attribute("org.eclipse.jetty.multipartConfig", 
            new MultipartConfigElement(
                System.getProperty("java.io.tmpdir"),
                MAX_FILE_SIZE,
                MAX_FILE_SIZE,
                1024 * 1024
            ));
        
        Part filePart = req.raw().getPart("pdf");
        if (filePart == null) {
            res.status(400);
            return gson.toJson(criarErro("PDF não enviado"));
        }
        
        String contentType = filePart.getContentType();
        if (!contentType.equals("application/pdf")) {
            res.status(400);
            return gson.toJson(criarErro("Arquivo deve ser um PDF"));
        }
        
        if (filePart.getSize() > MAX_FILE_SIZE) {
            res.status(400);
            return gson.toJson(criarErro("Arquivo muito grande. Máximo: 15MB"));
        }
        
        InputStream inputStream = filePart.getInputStream();
        String textoPDF = pdfReader.extrairTextoDePDF(inputStream);
        
        if (textoPDF.trim().isEmpty()) {
            res.status(400);
            return gson.toJson(criarErro("PDF vazio ou sem texto extraível"));
        }
        
        String resumo = aiService.resumirTexto(textoPDF);
        
        JsonObject resposta = new JsonObject();
        resposta.addProperty("resumo", resumo);
        resposta.addProperty("tamanhoOriginal", textoPDF.length());
        resposta.addProperty("tamanhoResumo", resumo.length());
        resposta.addProperty("fonte", "pdf");
        
        res.status(200);
        return gson.toJson(resposta);
    }

    // 2. RESUMIR TEXTO
    private String resumirTexto(Request req, Response res) throws IOException {
        res.type("application/json");
        
        JsonObject body = gson.fromJson(req.body(), JsonObject.class);
        
        if (!body.has("texto")) {
            res.status(400);
            return gson.toJson(criarErro("Campo 'texto' não fornecido"));
        }
        
        String texto = body.get("texto").getAsString().trim();
        
        if (texto.isEmpty()) {
            res.status(400);
            return gson.toJson(criarErro("Texto não pode estar vazio"));
        }
        
        if (texto.length() > MAX_TEXTO_LENGTH) {
            res.status(400);
            return gson.toJson(criarErro(
                String.format("Texto muito longo. Máximo: %d caracteres", MAX_TEXTO_LENGTH)));
        }
        
        String resumo = aiService.resumirTexto(texto);
        
        JsonObject resposta = new JsonObject();
        resposta.addProperty("resumo", resumo);
        resposta.addProperty("tamanhoOriginal", texto.length());
        resposta.addProperty("tamanhoResumo", resumo.length());
        resposta.addProperty("fonte", "texto");
        
        res.status(200);
        return gson.toJson(resposta);
    }

    // ========================================
    // MÉTODOS DE QUIZ
    // ========================================

    // 3. CRIAR QUIZ DE PDF
    private String criarQuizPDF(Request req, Response res) throws IOException, ServletException {
        res.type("application/json");
        
        req.attribute("org.eclipse.jetty.multipartConfig", 
            new MultipartConfigElement(
                System.getProperty("java.io.tmpdir"),
                MAX_FILE_SIZE,
                MAX_FILE_SIZE,
                1024 * 1024
            ));
        
        Part filePart = req.raw().getPart("pdf");
        if (filePart == null) {
            res.status(400);
            return gson.toJson(criarErro("PDF não enviado"));
        }
        
        if (!filePart.getContentType().equals("application/pdf")) {
            res.status(400);
            return gson.toJson(criarErro("Arquivo deve ser um PDF"));
        }
        
        String titulo = "Quiz";
        int numeroQuestoes = 5;
        
        Part tituloPart = req.raw().getPart("titulo");
        if (tituloPart != null) {
            titulo = new String(tituloPart.getInputStream().readAllBytes()).trim();
        }
        
        Part numQuestoesPart = req.raw().getPart("numeroQuestoes");
        if (numQuestoesPart != null) {
            String numStr = new String(numQuestoesPart.getInputStream().readAllBytes()).trim();
            try {
                numeroQuestoes = Integer.parseInt(numStr);
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(criarErro("Número de questões inválido"));
            }
        }
        
        if (numeroQuestoes < MIN_QUESTOES || numeroQuestoes > MAX_QUESTOES) {
            res.status(400);
            return gson.toJson(criarErro(
                String.format("Número de questões deve estar entre %d e %d", 
                    MIN_QUESTOES, MAX_QUESTOES)));
        }
        
        InputStream inputStream = filePart.getInputStream();
        String conteudo = pdfReader.extrairTextoDePDF(inputStream);
        
        if (conteudo.trim().isEmpty()) {
            res.status(400);
            return gson.toJson(criarErro("PDF vazio ou sem texto extraível"));
        }
        
        Quiz quiz = aiService.criarQuiz(conteudo, titulo, numeroQuestoes);
        
        res.status(200);
        return gson.toJson(quiz);
    }

    // 4. CRIAR QUIZ DE TEXTO
    private String criarQuizTexto(Request req, Response res) throws IOException {
        res.type("application/json");
        
        JsonObject body = gson.fromJson(req.body(), JsonObject.class);
        
        if (!body.has("conteudo")) {
            res.status(400);
            return gson.toJson(criarErro("Campo 'conteudo' não fornecido"));
        }
        
        String conteudo = body.get("conteudo").getAsString().trim();
        String titulo = body.has("titulo") ? body.get("titulo").getAsString().trim() : "Quiz";
        int numeroQuestoes = body.has("numeroQuestoes") ? body.get("numeroQuestoes").getAsInt() : 5;
        
        if (conteudo.isEmpty()) {
            res.status(400);
            return gson.toJson(criarErro("Conteúdo não pode estar vazio"));
        }
        
        if (conteudo.length() > MAX_TEXTO_LENGTH) {
            res.status(400);
            return gson.toJson(criarErro(
                String.format("Conteúdo muito longo. Máximo: %d caracteres", MAX_TEXTO_LENGTH)));
        }
        
        if (titulo.isEmpty()) {
            titulo = "Quiz";
        }
        
        if (numeroQuestoes < MIN_QUESTOES || numeroQuestoes > MAX_QUESTOES) {
            res.status(400);
            return gson.toJson(criarErro(
                String.format("Número de questões deve estar entre %d e %d", 
                    MIN_QUESTOES, MAX_QUESTOES)));
        }
        
        Quiz quiz = aiService.criarQuiz(conteudo, titulo, numeroQuestoes);
        
        res.status(200);
        return gson.toJson(quiz);
    }

    // ========================================
    // MÉTODOS DE FLASHCARDS
    // ========================================

    // 5. CRIAR FLASHCARDS DE PDF
    private String criarFlashcardsPDF(Request req, Response res) throws IOException, ServletException {
        res.type("application/json");
        
        req.attribute("org.eclipse.jetty.multipartConfig", 
            new MultipartConfigElement(
                System.getProperty("java.io.tmpdir"),
                MAX_FILE_SIZE,
                MAX_FILE_SIZE,
                1024 * 1024
            ));
        
        Part filePart = req.raw().getPart("pdf");
        if (filePart == null) {
            res.status(400);
            return gson.toJson(criarErro("PDF não enviado"));
        }
        
        if (!filePart.getContentType().equals("application/pdf")) {
            res.status(400);
            return gson.toJson(criarErro("Arquivo deve ser um PDF"));
        }
        
        int numeroCards = 10;
        
        Part numCardsPart = req.raw().getPart("numeroCards");
        if (numCardsPart != null) {
            String numStr = new String(numCardsPart.getInputStream().readAllBytes()).trim();
            try {
                numeroCards = Integer.parseInt(numStr);
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(criarErro("Número de flashcards inválido"));
            }
        }
        
        if (numeroCards < MIN_FLASHCARDS || numeroCards > MAX_FLASHCARDS) {
            res.status(400);
            return gson.toJson(criarErro(
                String.format("Número de flashcards deve estar entre %d e %d", 
                    MIN_FLASHCARDS, MAX_FLASHCARDS)));
        }
        
        InputStream inputStream = filePart.getInputStream();
        String conteudo = pdfReader.extrairTextoDePDF(inputStream);
        
        if (conteudo.trim().isEmpty()) {
            res.status(400);
            return gson.toJson(criarErro("PDF vazio ou sem texto extraível"));
        }
        
        List<Flashcard> flashcards = aiService.criarFlashcards(conteudo, numeroCards);
        
        JsonObject resposta = new JsonObject();
        resposta.add("flashcards", gson.toJsonTree(flashcards));
        resposta.addProperty("total", flashcards.size());
        resposta.addProperty("fonte", "pdf");
        
        res.status(200);
        return gson.toJson(resposta);
    }

    // 6. CRIAR FLASHCARDS DE TEXTO
    private String criarFlashcardsTexto(Request req, Response res) throws IOException {
        res.type("application/json");
        
        JsonObject body = gson.fromJson(req.body(), JsonObject.class);
        
        if (!body.has("conteudo")) {
            res.status(400);
            return gson.toJson(criarErro("Campo 'conteudo' não fornecido"));
        }
        
        String conteudo = body.get("conteudo").getAsString().trim();
        int numeroCards = body.has("numeroCards") ? body.get("numeroCards").getAsInt() : 10;
        
        if (conteudo.isEmpty()) {
            res.status(400);
            return gson.toJson(criarErro("Conteúdo não pode estar vazio"));
        }
        
        if (conteudo.length() > MAX_TEXTO_LENGTH) {
            res.status(400);
            return gson.toJson(criarErro(
                String.format("Conteúdo muito longo. Máximo: %d caracteres", MAX_TEXTO_LENGTH)));
        }
        
        if (numeroCards < MIN_FLASHCARDS || numeroCards > MAX_FLASHCARDS) {
            res.status(400);
            return gson.toJson(criarErro(
                String.format("Número de flashcards deve estar entre %d e %d", 
                    MIN_FLASHCARDS, MAX_FLASHCARDS)));
        }
        
        List<Flashcard> flashcards = aiService.criarFlashcards(conteudo, numeroCards);
        
        JsonObject resposta = new JsonObject();
        resposta.add("flashcards", gson.toJsonTree(flashcards));
        resposta.addProperty("total", flashcards.size());
        resposta.addProperty("fonte", "texto");
        
        res.status(200);
        return gson.toJson(resposta);
    }

    // ========================================
    // MÉTODOS DE PERGUNTAS
    // ========================================

    // 7. RESPONDER DÚVIDA SIMPLES (sem contexto)
    private String responderDuvidaSimples(Request req, Response res) throws IOException {
        res.type("application/json");
        
        JsonObject body = gson.fromJson(req.body(), JsonObject.class);
        
        if (!body.has("pergunta")) {
            res.status(400);
            return gson.toJson(criarErro("Campo 'pergunta' não fornecido"));
        }
        
        String pergunta = body.get("pergunta").getAsString().trim();
        
        if (pergunta.isEmpty()) {
            res.status(400);
            return gson.toJson(criarErro("Pergunta não pode estar vazia"));
        }
        
        if (pergunta.length() > MAX_PERGUNTA_LENGTH) {
            res.status(400);
            return gson.toJson(criarErro(
                String.format("Pergunta muito longa. Máximo: %d caracteres", 
                    MAX_PERGUNTA_LENGTH)));
        }
        
        String resposta = aiService.responderDuvida(pergunta, null);
        
        JsonObject respostaJson = new JsonObject();
        respostaJson.addProperty("resposta", resposta);
        respostaJson.addProperty("temContexto", false);
        respostaJson.addProperty("tipoContexto", "nenhum");
        
        res.status(200);
        return gson.toJson(respostaJson);
    }

    // 8. RESPONDER DÚVIDA COM CONTEXTO DE TEXTO
    private String responderDuvidaComContexto(Request req, Response res) throws IOException {
        res.type("application/json");
        
        JsonObject body = gson.fromJson(req.body(), JsonObject.class);
        
        if (!body.has("pergunta")) {
            res.status(400);
            return gson.toJson(criarErro("Campo 'pergunta' não fornecido"));
        }
        
        if (!body.has("contexto")) {
            res.status(400);
            return gson.toJson(criarErro("Campo 'contexto' não fornecido"));
        }
        
        String pergunta = body.get("pergunta").getAsString().trim();
        String contexto = body.get("contexto").getAsString().trim();
        
        if (pergunta.isEmpty()) {
            res.status(400);
            return gson.toJson(criarErro("Pergunta não pode estar vazia"));
        }
        
        if (contexto.isEmpty()) {
            res.status(400);
            return gson.toJson(criarErro("Contexto não pode estar vazio"));
        }
        
        if (pergunta.length() > MAX_PERGUNTA_LENGTH) {
            res.status(400);
            return gson.toJson(criarErro(
                String.format("Pergunta muito longa. Máximo: %d caracteres", 
                    MAX_PERGUNTA_LENGTH)));
        }
        
        if (contexto.length() > MAX_CONTEXTO_LENGTH) {
            res.status(400);
            return gson.toJson(criarErro(
                String.format("Contexto muito longo. Máximo: %d caracteres", 
                    MAX_CONTEXTO_LENGTH)));
        }
        
        String resposta = aiService.responderDuvida(pergunta, contexto);
        
        JsonObject respostaJson = new JsonObject();
        respostaJson.addProperty("resposta", resposta);
        respostaJson.addProperty("temContexto", true);
        respostaJson.addProperty("tipoContexto", "texto");
        respostaJson.addProperty("tamanhoContexto", contexto.length());
        
        res.status(200);
        return gson.toJson(respostaJson);
    }

    // 9. RESPONDER DÚVIDA COM CONTEXTO DE PDF
    private String responderDuvidaComPDF(Request req, Response res) throws IOException, ServletException {
        res.type("application/json");
        
        req.attribute("org.eclipse.jetty.multipartConfig", 
            new MultipartConfigElement(
                System.getProperty("java.io.tmpdir"),
                MAX_FILE_SIZE,
                MAX_FILE_SIZE,
                1024 * 1024
            ));
        
        Part perguntaPart = req.raw().getPart("pergunta");
        if (perguntaPart == null) {
            res.status(400);
            return gson.toJson(criarErro("Campo 'pergunta' não fornecido"));
        }
        
        Part pdfPart = req.raw().getPart("pdf");
        if (pdfPart == null) {
            res.status(400);
            return gson.toJson(criarErro("PDF não enviado"));
        }
        
        if (!pdfPart.getContentType().equals("application/pdf")) {
            res.status(400);
            return gson.toJson(criarErro("Arquivo deve ser um PDF"));
        }
        
        String pergunta = new String(perguntaPart.getInputStream().readAllBytes()).trim();
        
        if (pergunta.isEmpty()) {
            res.status(400);
            return gson.toJson(criarErro("Pergunta não pode estar vazia"));
        }
        
        if (pergunta.length() > MAX_PERGUNTA_LENGTH) {
            res.status(400);
            return gson.toJson(criarErro(
                String.format("Pergunta muito longa. Máximo: %d caracteres", 
                    MAX_PERGUNTA_LENGTH)));
        }
        
        InputStream inputStream = pdfPart.getInputStream();
        String contexto = pdfReader.extrairTextoDePDF(inputStream);
        
        if (contexto.trim().isEmpty()) {
            res.status(400);
            return gson.toJson(criarErro("PDF vazio ou sem texto extraível"));
        }
        
        if (contexto.length() > MAX_CONTEXTO_LENGTH) {
            res.status(400);
            return gson.toJson(criarErro(
                String.format("Contexto do PDF muito longo. Máximo: %d caracteres", 
                    MAX_CONTEXTO_LENGTH)));
        }
        
        String resposta = aiService.responderDuvida(pergunta, contexto);
        
        JsonObject respostaJson = new JsonObject();
        respostaJson.addProperty("resposta", resposta);
        respostaJson.addProperty("temContexto", true);
        respostaJson.addProperty("tipoContexto", "pdf");
        respostaJson.addProperty("tamanhoContexto", contexto.length());
        
        res.status(200);
        return gson.toJson(respostaJson);
    }

    private JsonObject criarErro(String mensagem) {
        JsonObject erro = new JsonObject();
        erro.addProperty("erro", mensagem);
        erro.addProperty("timestamp", System.currentTimeMillis());
        return erro;
    }
}