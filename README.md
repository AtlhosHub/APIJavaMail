# 📨 Email OCR Payment Validator

Este projeto é um serviço Spring Boot que automatiza a leitura de emails com comprovantes de pagamento anexados (PDF, JPG, PNG) e realiza OCR (Reconhecimento Óptico de Caracteres) para identificar valores pagos e remetentes. O objetivo é auxiliar o controle de mensalidades de um clube de tênis de mesa.

---

## ⚙️ Pré-requisitos

### 1. Java
- Java 21 instalado
- Variável de ambiente `JAVA_HOME` configurada
- Você provavelmente já tem isso :) 
### 2. Maven
- Maven 3.8+ instalado e disponível no terminal
- Você também já tem isso!!

### 3. Tesseract OCR

#### Windows
- Baixe o instalador do Tesseract:
  [https://github.com/tesseract-ocr/tesseract/releases](https://github.com/tesseract-ocr/tesseract/releases)
- Instale o Tesseract em: `C:/Program Files/Tesseract-OCR`
- Verifique se o diretório `tessdata` existe dentro dessa pasta
- Configure a variável de ambiente:
  - Nome: `TESSDATA_PREFIX`
  - Valor: `C:/Program Files/Tesseract-OCR`

#### Teste a instalação:
```bash
tesseract -v
```

---

## 🚀 Rodando o projeto

1. Clone o repositório
```bash
git clone https://github.com/AtlhosHub/APIJavaMail.git
```

2. Configure o arquivo `application.properties` em `src/main/resources` com os dados do Gmail:

```
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=seu-email@gmail.com
spring.mail.password=sua-senha-de-app
spring.mail.protocol=smtp
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.default-encoding=UTF-8
```

> ⚠️ Use uma **senha de aplicativo** do Gmail. [Como gerar](https://support.google.com/mail/answer/185833?hl=pt-BR)

3. Compile e rode a aplicação:
```bash
```

---

## 📩 Como funciona

- A cada 5 minutos, o sistema acessa a caixa de entrada do Gmail
- Procura por emails **não lidos** com o assunto `"Pagamento"` ou `"Boleto"`
- Lê os anexos, extrai o texto via OCR e verifica se:
  - Existe um valor esperado (ex: R$ 120,00 ou múltiplos)
  - O nome do pagador está presente
- Se tudo for validado, o sistema envia uma **confirmação automática por email**

---

## ✨ Futuras melhorias

- Integração com banco de dados de alunos
- Painel administrativo com histórico de pagamentos
- Processamento de múltiplos boletos/mensalidades
- Detecção automática de períodos atrasados

---

## 🧠 Tecnologias

- Java 21
- Spring Boot 3.4.x
- Jakarta Mail (JavaMail)
- Apache PDFBox
- Tess4J (wrapper Java para Tesseract OCR)

---

## 👤 Autor

Feito com ☕ e 💻 por Thomas, yay
