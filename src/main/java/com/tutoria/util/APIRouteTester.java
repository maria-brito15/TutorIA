package com.tutoria.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.tutoria.util.PDFReader;
import okhttp3.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class APIRouteTester {

    private static final String BASE_URL = "http://localhost:4567/api/ai";
    private final OkHttpClient client;
    private final Gson gson;
    private final PDFReader pdfReader;
    private final StringBuilder relatorio;
    private int totalTestes = 0;
    private int testesPassados = 0;
    private int testesFalhados = 0;

    public APIRouteTester() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.pdfReader = new PDFReader();
        this.relatorio = new StringBuilder();
    }

    public static void main(String[] args) {
        APIRouteTester tester = new APIRouteTester();
        
        System.out.println("=================================================");
        System.out.println("    TESTADOR DE ROTAS - API DE IA");
        System.out.println("=================================================\n");
        
        tester.iniciarRelatorio();
        tester.executarTodosTestes();
        tester.finalizarRelatorio();
        
        String nomeArquivo = tester.salvarRelatorio();
        
        System.out.println("\n=================================================");
        System.out.println("    TESTES CONCLUÍDOS!");
        System.out.println("=================================================");
        System.out.println("Relatório salvo em: " + nomeArquivo);
        System.out.println("\nResumo:");
        System.out.println("  Total de testes: " + tester.totalTestes);
        System.out.println("  ✓ Passaram: " + tester.testesPassados);
        System.out.println("  ✗ Falharam: " + tester.testesFalhados);
        System.out.println("=================================================\n");
    }

    private void iniciarRelatorio() {
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        
        relatorio.append("=".repeat(80)).append("\n");
        relatorio.append("              RELATÓRIO DE TESTES - API DE IA\n");
        relatorio.append("=".repeat(80)).append("\n");
        relatorio.append("Data/Hora: ").append(dataHora).append("\n");
        relatorio.append("Base URL: ").append(BASE_URL).append("\n");
        relatorio.append("=".repeat(80)).append("\n\n");
    }

    private void executarTodosTestes() {
        // 1. Health Check
        testarHealthCheck();
        
        // 2. Resumo de Texto
        testarResumoTexto();
        
        // 3. Resumo de PDF
        testarResumoPDF();
        
        // 4. Quiz de Texto
        testarQuizTexto();
        
        // 5. Quiz de PDF
        testarQuizPDF();
        
        // 6. Flashcards de Texto
        testarFlashcardsTexto();
        
        // 7. Flashcards de PDF
        testarFlashcardsPDF();
        
        // 8. Pergunta Simples
        testarPerguntaSimples();
        
        // 9. Pergunta com Contexto
        testarPerguntaComContexto();
        
        // 10. Pergunta com PDF
        testarPerguntaComPDF();
        
        // 11. Testes de Validação (erros esperados)
        testarValidacoes();
    }

    // ========================================
    // TESTE 1: HEALTH CHECK
    // ========================================
    private void testarHealthCheck() {
        iniciarTeste("Health Check", "GET /api/ai/health");
        
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/health")
                    .get()
                    .build();
            
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            
            if (response.isSuccessful()) {
                registrarSucesso("Health check respondeu corretamente", body);
            } else {
                registrarFalha("Health check falhou", response.code(), body);
            }
            
        } catch (Exception e) {
            registrarErro("Health check", e);
        }
    }

    // ========================================
    // TESTE 2: RESUMO DE TEXTO
    // ========================================
    private void testarResumoTexto() {
        iniciarTeste("Resumo de Texto", "POST /api/ai/resumir/texto");
        
        try {
            JsonObject body = new JsonObject();
            body.addProperty("texto", "A fotossíntese é o processo pelo qual as plantas convertem luz solar em energia química. Este processo ocorre nos cloroplastos e é essencial para a vida na Terra.");
            
            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/resumir/texto")
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            if (response.isSuccessful()) {
                registrarSucesso("Resumo gerado com sucesso", responseBody);
            } else {
                registrarFalha("Falha ao gerar resumo", response.code(), responseBody);
            }
            
        } catch (Exception e) {
            registrarErro("Resumo de texto", e);
        }
    }

    // ========================================
    // TESTE 3: RESUMO DE PDF
    // ========================================
    private void testarResumoPDF() {
        iniciarTeste("Resumo de PDF", "POST /api/ai/resumir/pdf");
        
        // Criar PDF de teste temporário
        File pdfTeste = criarPDFTeste();
        
        if (pdfTeste == null) {
            registrarFalha("Não foi possível criar PDF de teste", 0, "Arquivo não criado");
            return;
        }
        
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("pdf", "teste.pdf",
                            RequestBody.create(pdfTeste, MediaType.parse("application/pdf")))
                    .build();
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/resumir/pdf")
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            if (response.isSuccessful()) {
                registrarSucesso("Resumo de PDF gerado com sucesso", responseBody);
            } else {
                registrarFalha("Falha ao gerar resumo de PDF", response.code(), responseBody);
            }
            
        } catch (Exception e) {
            registrarErro("Resumo de PDF", e);
        }
    }

    // ========================================
    // TESTE 4: QUIZ DE TEXTO
    // ========================================
    private void testarQuizTexto() {
        iniciarTeste("Quiz de Texto", "POST /api/ai/quiz/texto");
        
        try {
            JsonObject body = new JsonObject();
            body.addProperty("conteudo", "A água é composta por dois átomos de hidrogênio e um átomo de oxigênio (H2O). É essencial para a vida e cobre cerca de 71% da superfície da Terra.");
            body.addProperty("titulo", "Quiz sobre Água");
            body.addProperty("numeroQuestoes", 3);
            
            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/quiz/texto")
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            if (response.isSuccessful()) {
                registrarSucesso("Quiz criado com sucesso", responseBody);
            } else {
                registrarFalha("Falha ao criar quiz", response.code(), responseBody);
            }
            
        } catch (Exception e) {
            registrarErro("Quiz de texto", e);
        }
    }

    // ========================================
    // TESTE 5: QUIZ DE PDF
    // ========================================
    private void testarQuizPDF() {
        iniciarTeste("Quiz de PDF", "POST /api/ai/quiz/pdf");
        
        File pdfTeste = criarPDFTeste();
        
        if (pdfTeste == null) {
            registrarFalha("Não foi possível criar PDF de teste", 0, "Arquivo não criado");
            return;
        }
        
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("pdf", "teste.pdf",
                            RequestBody.create(pdfTeste, MediaType.parse("application/pdf")))
                    .addFormDataPart("titulo", "Quiz de Teste")
                    .addFormDataPart("numeroQuestoes", "3")
                    .build();
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/quiz/pdf")
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            if (response.isSuccessful()) {
                registrarSucesso("Quiz de PDF criado com sucesso", responseBody);
            } else {
                registrarFalha("Falha ao criar quiz de PDF", response.code(), responseBody);
            }
            
        } catch (Exception e) {
            registrarErro("Quiz de PDF", e);
        }
    }

    // ========================================
    // TESTE 6: FLASHCARDS DE TEXTO
    // ========================================
    private void testarFlashcardsTexto() {
        iniciarTeste("Flashcards de Texto", "POST /api/ai/flashcards/texto");
        
        try {
            JsonObject body = new JsonObject();
            body.addProperty("conteudo", "JavaScript é uma linguagem de programação interpretada. Foi criada por Brendan Eich em 1995. É muito usada para desenvolvimento web.");
            body.addProperty("numeroCards", 3);
            
            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/flashcards/texto")
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            if (response.isSuccessful()) {
                registrarSucesso("Flashcards criados com sucesso", responseBody);
            } else {
                registrarFalha("Falha ao criar flashcards", response.code(), responseBody);
            }
            
        } catch (Exception e) {
            registrarErro("Flashcards de texto", e);
        }
    }

    // ========================================
    // TESTE 7: FLASHCARDS DE PDF
    // ========================================
    private void testarFlashcardsPDF() {
        iniciarTeste("Flashcards de PDF", "POST /api/ai/flashcards/pdf");
        
        File pdfTeste = criarPDFTeste();
        
        if (pdfTeste == null) {
            registrarFalha("Não foi possível criar PDF de teste", 0, "Arquivo não criado");
            return;
        }
        
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("pdf", "teste.pdf",
                            RequestBody.create(pdfTeste, MediaType.parse("application/pdf")))
                    .addFormDataPart("numeroCards", "3")
                    .build();
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/flashcards/pdf")
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            if (response.isSuccessful()) {
                registrarSucesso("Flashcards de PDF criados com sucesso", responseBody);
            } else {
                registrarFalha("Falha ao criar flashcards de PDF", response.code(), responseBody);
            }
            
        } catch (Exception e) {
            registrarErro("Flashcards de PDF", e);
        }
    }

    // ========================================
    // TESTE 8: PERGUNTA SIMPLES
    // ========================================
    private void testarPerguntaSimples() {
        iniciarTeste("Pergunta Simples", "POST /api/ai/perguntar");
        
        try {
            JsonObject body = new JsonObject();
            body.addProperty("pergunta", "O que é inteligência artificial?");
            
            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/perguntar")
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            if (response.isSuccessful()) {
                registrarSucesso("Pergunta respondida com sucesso", responseBody);
            } else {
                registrarFalha("Falha ao responder pergunta", response.code(), responseBody);
            }
            
        } catch (Exception e) {
            registrarErro("Pergunta simples", e);
        }
    }

    // ========================================
    // TESTE 9: PERGUNTA COM CONTEXTO
    // ========================================
    private void testarPerguntaComContexto() {
        iniciarTeste("Pergunta com Contexto", "POST /api/ai/perguntar/contexto");
        
        try {
            JsonObject body = new JsonObject();
            body.addProperty("pergunta", "Quais são os principais componentes?");
            body.addProperty("contexto", "Um computador é composto por CPU, memória RAM, disco rígido, placa-mãe e fonte de alimentação. A CPU é o cérebro do computador.");
            
            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/perguntar/contexto")
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            if (response.isSuccessful()) {
                registrarSucesso("Pergunta com contexto respondida com sucesso", responseBody);
            } else {
                registrarFalha("Falha ao responder pergunta com contexto", response.code(), responseBody);
            }
            
        } catch (Exception e) {
            registrarErro("Pergunta com contexto", e);
        }
    }

    // ========================================
    // TESTE 10: PERGUNTA COM PDF
    // ========================================
    private void testarPerguntaComPDF() {
        iniciarTeste("Pergunta com PDF", "POST /api/ai/perguntar/pdf");
        
        File pdfTeste = criarPDFTeste();
        
        if (pdfTeste == null) {
            registrarFalha("Não foi possível criar PDF de teste", 0, "Arquivo não criado");
            return;
        }
        
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("pergunta", "Do que trata o documento?")
                    .addFormDataPart("pdf", "teste.pdf",
                            RequestBody.create(pdfTeste, MediaType.parse("application/pdf")))
                    .build();
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/perguntar/pdf")
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            if (response.isSuccessful()) {
                registrarSucesso("Pergunta com PDF respondida com sucesso", responseBody);
            } else {
                registrarFalha("Falha ao responder pergunta com PDF", response.code(), responseBody);
            }
            
        } catch (Exception e) {
            registrarErro("Pergunta com PDF", e);
        }
    }

    // ========================================
    // TESTE 11: VALIDAÇÕES (ERROS ESPERADOS)
    // ========================================
    private void testarValidacoes() {
        relatorio.append("\n").append("-".repeat(80)).append("\n");
        relatorio.append("TESTES DE VALIDAÇÃO (Erros Esperados)\n");
        relatorio.append("-".repeat(80)).append("\n\n");
        
        testarTextoVazio();
        
        testarPerguntaSemCampo();
        
        testarNumeroQuestoesInvalido();
    }

    private void testarTextoVazio() {
        iniciarTeste("Validação: Texto Vazio", "POST /api/ai/resumir/texto");
        
        try {
            JsonObject body = new JsonObject();
            body.addProperty("texto", "");
            
            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/resumir/texto")
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            if (response.code() == 400) {
                registrarSucesso("Validação funcionou corretamente (erro 400 esperado)", responseBody);
            } else {
                registrarFalha("Validação não funcionou", response.code(), responseBody);
            }
            
        } catch (Exception e) {
            registrarErro("Validação de texto vazio", e);
        }
    }

    private void testarPerguntaSemCampo() {
        iniciarTeste("Validação: Pergunta sem Campo", "POST /api/ai/perguntar");
        
        try {
            JsonObject body = new JsonObject();
            // Não adiciona o campo "pergunta"
            
            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/perguntar")
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            if (response.code() == 400) {
                registrarSucesso("Validação funcionou corretamente (erro 400 esperado)", responseBody);
            } else {
                registrarFalha("Validação não funcionou", response.code(), responseBody);
            }
            
        } catch (Exception e) {
            registrarErro("Validação de campo obrigatório", e);
        }
    }

    private void testarNumeroQuestoesInvalido() {
        iniciarTeste("Validação: Número de Questões Inválido", "POST /api/ai/quiz/texto");
        
        try {
            JsonObject body = new JsonObject();
            body.addProperty("conteudo", "Conteúdo de teste");
            body.addProperty("numeroQuestoes", 100);
            
            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "/quiz/texto")
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            if (response.code() == 400) {
                registrarSucesso("Validação funcionou corretamente (erro 400 esperado)", responseBody);
            } else {
                registrarFalha("Validação não funcionou", response.code(), responseBody);
            }
            
        } catch (Exception e) {
            registrarErro("Validação de limite de questões", e);
        }
    }

    // ========================================
    // MÉTODOS AUXILIARES
    // ========================================

    private void iniciarTeste(String nomeTeste, String endpoint) {
        totalTestes++;
        relatorio.append("\n").append("-".repeat(80)).append("\n");
        relatorio.append("TESTE #").append(totalTestes).append(": ").append(nomeTeste).append("\n");
        relatorio.append("Endpoint: ").append(endpoint).append("\n");
        relatorio.append("-".repeat(80)).append("\n");
        
        System.out.println("Executando: " + nomeTeste + "...");
    }

    private void registrarSucesso(String mensagem, String resposta) {
        testesPassados++;
        relatorio.append("✓ SUCESSO: ").append(mensagem).append("\n\n");
        relatorio.append("Resposta (primeiros 500 caracteres):\n");
        relatorio.append(truncar(resposta, 500)).append("\n");
    }

    private void registrarFalha(String mensagem, int codigo, String resposta) {
        testesFalhados++;
        relatorio.append("✗ FALHA: ").append(mensagem).append("\n");
        relatorio.append("Código HTTP: ").append(codigo).append("\n\n");
        relatorio.append("Resposta:\n");
        relatorio.append(truncar(resposta, 500)).append("\n");
    }

    private void registrarErro(String teste, Exception e) {
        testesFalhados++;
        relatorio.append("✗ ERRO DE EXECUÇÃO: ").append(teste).append("\n");
        relatorio.append("Exceção: ").append(e.getClass().getSimpleName()).append("\n");
        relatorio.append("Mensagem: ").append(e.getMessage()).append("\n");
    }

    private void finalizarRelatorio() {
        relatorio.append("\n").append("=".repeat(80)).append("\n");
        relatorio.append("              RESUMO DOS TESTES\n");
        relatorio.append("=".repeat(80)).append("\n");
        relatorio.append("Total de testes executados: ").append(totalTestes).append("\n");
        relatorio.append("✓ Testes bem-sucedidos: ").append(testesPassados).append("\n");
        relatorio.append("✗ Testes falhados: ").append(testesFalhados).append("\n");
        
        double taxaSucesso = (double) testesPassados / totalTestes * 100;
        relatorio.append("Taxa de sucesso: ").append(String.format("%.1f%%", taxaSucesso)).append("\n");
        relatorio.append("=".repeat(80)).append("\n");
    }

    private String salvarRelatorio() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nomeArquivo = "relatorio_testes_" + timestamp + ".txt";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(nomeArquivo))) {
            writer.print(relatorio.toString());
            return nomeArquivo;
        } catch (IOException e) {
            System.err.println("Erro ao salvar relatório: " + e.getMessage());
            return "ERRO: Não foi possível salvar o relatório";
        }
    }

    private String truncar(String texto, int maxLength) {
        if (texto == null) return "null";
        if (texto.length() <= maxLength) return texto;
        return texto.substring(0, maxLength) + "\n... (truncado)";
    }

    private File criarPDFTeste() {
        try {
            File pdfFile = new File("arquivo_test.pdf");
            
            if (!pdfFile.exists()) {
                String[] possiveisCaminhos = {
                    "./arquivo_test.pdf",
                    "../arquivo_test.pdf",
                    "arquivo_test.pdf"
                };
                
                for (String caminho : possiveisCaminhos) {
                    pdfFile = new File(caminho);
                    if (pdfFile.exists()) {
                        break;
                    }
                }
                
                if (!pdfFile.exists()) {
                    System.err.println("ERRO: arquivo_test.pdf não encontrado na raiz do projeto!");
                    System.err.println("Caminho esperado: " + new File("arquivo_test.pdf").getAbsolutePath());
                    relatorio.append("\n⚠ AVISO: PDF de teste não encontrado. Testes com PDF serão pulados.\n");
                    return null;
                }
            }
            
            try {
                String conteudo = pdfReader.extrairTextoDePDF(pdfFile);
                if (conteudo == null || conteudo.trim().isEmpty()) {
                    System.err.println("AVISO: PDF de teste está vazio ou sem texto extraível!");
                    relatorio.append("\n⚠ AVISO: PDF de teste vazio. Alguns testes podem falhar.\n");
                }
                System.out.println("✓ PDF de teste validado: " + pdfFile.getAbsolutePath());
                System.out.println("  Conteúdo extraído: " + conteudo.substring(0, Math.min(100, conteudo.length())) + "...");
            } catch (IOException e) {
                System.err.println("ERRO ao ler PDF de teste: " + e.getMessage());
                relatorio.append("\n⚠ ERRO: Não foi possível ler o PDF de teste.\n");
                return null;
            }
            
            return pdfFile;
            
        } catch (Exception e) {
            System.err.println("Erro ao localizar PDF de teste: " + e.getMessage());
            return null;
        }
    }
}