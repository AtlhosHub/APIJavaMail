// EmailReaderService.java

package com.example.email.service;

import com.example.email.model.PagamentoExtraido;
import com.google.gson.*;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.*;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Properties;

@Service
public class EmailReaderService {

    private static final String EMAIL = "lifehealthcomp@gmail.com";
    private static final String PASSWORD = "qsto adve zwlc wtzm";

    @Autowired
    private JavaMailSender mailSender;

    @Scheduled(fixedDelay = 300000)
    public void verificarEmails() {
        System.out.println("🔄 Verificando emails às " + java.time.LocalDateTime.now());
        try {
            Store store = conectarEmail();
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.search(new AndTerm(
                    new FlagTerm(new Flags(Flags.Flag.SEEN), false),
                    new OrTerm(
                            new SubjectTerm("Pagamento"),
                            new SubjectTerm("Boleto")
                    )
            ));

            for (Message message : messages) {
                Address[] from = message.getFrom();
                if (from == null || from.length == 0) continue;
                String remetente = from[0].toString();

                if (message.getContentType().contains("multipart")) {
                    Multipart multipart = (Multipart) message.getContent();

                    for (int i = 0; i < multipart.getCount(); i++) {
                        BodyPart part = multipart.getBodyPart(i);
                        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                            File tempFile = salvarAnexoTemporariamente(part);
                            String textoExtraido = realizarOCR(tempFile);

                            String jsonGemini = enviarParaGemini(textoExtraido);
                            System.out.println("📦 Resposta do Gemini:\n" + jsonGemini);

                            PagamentoExtraido pagamento = extrairPagamentoDoGemini(jsonGemini);
                            if (pagamento != null && pagamento.valor != null && pagamento.nome_destinatario != null) {
                                System.out.println("✅ Pagamento identificado: " + pagamento);
                                enviarEmailDeConfirmacao(remetente, pagamento.nome_destinatario);
                            }

                            if (tempFile.exists()) tempFile.delete();
                        }
                    }
                }
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            System.err.println("❌ Erro ao verificar emails:");
            e.printStackTrace();
        }
    }

    private Store conectarEmail() throws Exception {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        Session session = Session.getDefaultInstance(props, null);
        Store store = session.getStore("imaps");
        store.connect("imap.gmail.com", EMAIL, PASSWORD);
        return store;
    }

    private File salvarAnexoTemporariamente(BodyPart part) throws Exception {
        String fileName = "temp_" + part.getFileName();
        File file = new File(fileName);
        try (InputStream is = part.getInputStream();
             FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        return file;
    }

    private String realizarOCR(File file) {
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
            tesseract.setLanguage("eng");

            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".pdf")) {
                return ocrFromPDF(file, tesseract);
            } else {
                BufferedImage image = ImageIO.read(file);
                return tesseract.doOCR(image);
            }

        } catch (Exception e) {
            System.err.println("Erro no OCR:");
            e.printStackTrace();
            return "";
        }
    }

    private String ocrFromPDF(File pdfFile, Tesseract tesseract) throws IOException, TesseractException {
        PDDocument document = PDDocument.load(pdfFile);
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        StringBuilder fullText = new StringBuilder();

        for (int page = 0; page < document.getNumberOfPages(); page++) {
            BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
            fullText.append(tesseract.doOCR(image)).append("\n");
        }

        document.close();
        return fullText.toString();
    }

    private String enviarParaGemini(String ocrText) {
        try {
            String apiKey = "AIzaSyCS_Nyk5_7eZE7dceMiZDngNJufOqWtKgI";

            String prompt = "Extraia os seguintes campos STRICT JSON (sem markdown, sem texto adicional): "
                    + "nome_remetente, nome_destinatario, valor(number), data_hora, tipo, banco_origem, banco_destino, codigo_transacao. "
                    + "Texto para análise:\n" + ocrText;

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", prompt);

            JsonArray parts = new JsonArray();
            parts.add(textPart);

            JsonObject content = new JsonObject();
            content.add("parts", parts);

            JsonArray contents = new JsonArray();
            contents.add(content);

            JsonObject requestBody = new JsonObject();
            requestBody.add("contents", contents);

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                    .post(okhttp3.RequestBody.create(
                            requestBody.toString(),
                            okhttp3.MediaType.parse("application/json")
                    ))
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Erro Gemini: " + response.code() + " - " + response.body().string());
                }
                return response.body().string();
            }

        } catch (Exception e) {
            System.err.println("❌ Erro ao chamar Gemini:");
            e.printStackTrace();
            return null;
        }
    }

    private PagamentoExtraido extrairPagamentoDoGemini(String respostaGemini) {
        try {
            JsonObject respostaCompleta = JsonParser.parseString(respostaGemini).getAsJsonObject();

            String textoCrudo = respostaCompleta
                    .getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            String jsonLimpo = textoCrudo
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .replaceAll("JSON", "")
                    .trim();

            if (!jsonLimpo.startsWith("{") || !jsonLimpo.endsWith("}")) {
                throw new IllegalArgumentException("Resposta não contém JSON válido");
            }
            System.out.println("JSON processado: " + jsonLimpo);

            return new Gson().fromJson(jsonLimpo, PagamentoExtraido.class);

        } catch (Exception e) {
            System.err.println("❌ Erro crítico ao processar resposta do Gemini:");
            System.err.println("Resposta original: " + respostaGemini);
            e.printStackTrace();
            return null;
        }
    }

    private void enviarEmailDeConfirmacao(String destinatario, String nome) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");

            helper.setTo(destinatario);
            helper.setSubject("Confirmação de Pagamento");
            helper.setText("Olá! O pagamento em nome de " + nome + " foi recebido com sucesso. Obrigado!", true);
            helper.setFrom(EMAIL);

            mailSender.send(message);
            System.out.println("📨 Email de confirmação enviado para: " + destinatario);
        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar email de confirmação:");
            e.printStackTrace();
        }
    }
}