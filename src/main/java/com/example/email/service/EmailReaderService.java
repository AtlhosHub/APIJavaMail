package com.example.email.service;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.OrTerm;
import jakarta.mail.search.SubjectTerm;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

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

            Flags seen = new Flags(Flags.Flag.SEEN);
            FlagTerm unseenFlagTerm = new FlagTerm(seen, false);

            Message[] messages = inbox.search(new AndTerm(
                    unseenFlagTerm,
                    new OrTerm(
                            new SubjectTerm("Pagamento"),
                            new SubjectTerm("Boleto")
                            //CONSULTAR EMAILS CADASTRADOS, FAZER UMA QUERY QUE BUSQUE
                            // O EMAIL DO REMETENTE, E A QUE ALAUNO ELE É ASSOCIADO
                    )
            ));

            for (Message message : messages) {
                Address[] from = message.getFrom();
                if (from == null || from.length == 0) continue;
                String remetente = from[0].toString();

                boolean pagamentoIdentificado = false;

                if (message.getContentType().contains("multipart")) {
                    Multipart multipart = (Multipart) message.getContent();

                    for (int i = 0; i < multipart.getCount(); i++) {
                        BodyPart part = multipart.getBodyPart(i);
                        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                            File tempFile = salvarAnexoTemporariamente(part);
                            String textoExtraido = realizarOCR(tempFile);

                            if (verificaPagamento(textoExtraido)) {
                                pagamentoIdentificado = true;
                            }

                            if (tempFile.exists()) tempFile.delete();
                        }
                    }

                    if (pagamentoIdentificado) {
                        System.out.println("✅ Pagamento identificado no anexo. Enviando confirmação...");
                        enviarEmailDeConfirmacao(remetente);
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
    //estabeler um periodo onde a caixa de entrada vai ser verificada
    private String ocrFromPDF(File pdfFile, Tesseract tesseract) throws IOException, TesseractException {
        PDDocument document = PDDocument.load(pdfFile);
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        StringBuilder fullText = new StringBuilder();

        for (int page = 0; page < document.getNumberOfPages(); page++) {
            BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);

            String text = tesseract.doOCR(image);
            fullText.append(text).append("\n");
        }

        document.close();
        return fullText.toString();
    }


    private boolean verificaPagamento(String texto) {
        texto = texto.replaceAll("\\s+", " ").toUpperCase();

        boolean contemValor = Pattern.compile("R?\\$?\\s?652[.,\\s]?00").matcher(texto).find();
        boolean contemBeneficiario = texto.contains("CAUA GOUVEA DO NASCIMENTO");
        boolean contemPalavraChave = texto.contains("PIX") || texto.contains("ID DA TRANSACAO") || texto.contains("AUTENTICACAO") || texto.contains("COMPROVANTE");

        return contemValor && contemBeneficiario && contemPalavraChave;
    }

    private void enviarEmailDeConfirmacao(String destinatario) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");

            helper.setTo(destinatario);
            helper.setSubject("Confirmação de Pagamento");
            helper.setText("Olá! O pagamento foi recebido com sucesso. Obrigado!", true);
            helper.setFrom(EMAIL);

            mailSender.send(message);
            System.out.println("📨 Email de confirmação enviado para: " + destinatario);
        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar email de confirmação:");
            e.printStackTrace();
        }
    }
}