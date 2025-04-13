# 🤖 Leitor de Emails com Integração Gemini AI (Comprovantes de Pagamento)

Este projeto Java + Spring Boot tem como objetivo automatizar a leitura de comprovantes de pagamento enviados por email (ex: PIX, boletos, TED) e extrair as informações através de OCR ou inteligência artificial. A extração e interpretação dos dados é feita com a ajuda da API Gemini (Google AI).

## 📌 Funcionalidades

- Acessa automaticamente a caixa de entrada via IMAP
- Lê apenas emails não lidos com assunto "Pagamento" ou "Boleto"
- Extrai imagens ou PDFs anexados
- Envia o anexo diretamente à API do Gemini para interpretação
- Retorna um JSON com:
  - nome_remetente
  - nome_destinatario
  - valor
  - data_hora
  - tipo
  - banco_origem
  - banco_destino
  - codigo_transacao
- Envia um email de confirmação ao remetente

## ⚙️ Requisitos

- Java 17+
- Spring Boot
- Maven
- Conta Gmail habilitada para IMAP
- Conta Google com acesso à API Gemini (plano gratuito disponível)

## 🔧 Configuração do Ambiente

### 1. Clone o projeto
```bash
git clone https://github.com/AtlhosHub/APIJavaMail.git
cd APIJavaMail
```

### 2. Baixe e configure o Tesseract OCR (caso queira usar fallback por texto)

- Download: https://github.com/tesseract-ocr/tesseract
- Instale e copie o caminho para o diretório de dados `tessdata`.
- Altere no código, se estiver usando o modo OCR:
```java
tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
```

### 3. Gere e configure sua API KEY Gemini

- Documentação oficial: https://ai.google.dev/gemini-api/docs
- Crie um projeto no [Google Cloud Console](https://console.cloud.google.com)
- Ative a API **Generative Language API**
- Gere uma chave de API
- Substitua no código (ex: `enviarParaGemini(...)`):
```java
String apiKey = "SUA_CHAVE_AQUI";
```

### 4. Configuração do Email

Edite a classe `EmailReaderService.java` com suas credenciais:
```java
private static final String EMAIL = "seuemail@gmail.com";
private static final String PASSWORD = "sua_senha_de_aplicativo";
```
> Use senhas de aplicativo do Gmail para maior segurança.

### 5. Dependências Maven (Já inclusas no projeto)
- Spring Boot Starter Mail
- Tesseract Tess4J
- PDFBox
- GSON
- OkHttp3 (para chamadas à API Gemini)

## 🧪 Teste Local
- Envie um email para o seu próprio Gmail com assunto "Pagamento"
- Anexe um PDF ou imagem de comprovante
- Veja no console a resposta JSON retornada
- Verifique se o email de confirmação foi enviado de volta ao remetente

## 🧠 Prompt Gemini utilizado
```text
Você receberá a imagem de um comprovante de pagamento.
Extraia e retorne **somente um JSON puro**, sem explicações ou marcações.

Campos esperados:
- nome_remetente
- nome_destinatario
- valor
- data_hora
- tipo
- banco_origem
- banco_destino
- codigo_transacao

Se algum campo não estiver claro, use null. Leia a imagem com atenção. Responda apenas o JSON.
```

## 📂 Exemplo de JSON gerado
```json
{
  "nome_remetente": "Bianca Borges de Souza",
  "nome_destinatario": "Rosilene Bispo Vieira",
  "valor": "R$ 652,00",
  "data_hora": "09/04/2025, 08:21",
  "tipo": "PIX",
  "banco_origem": "Mercado Pago",
  "banco_destino": "Nu Pagamentos S.A.",
  "codigo_transacao": "E10573521202504091121quToRolYbND"
}
```

---

## ✨ Autor
Desenvolvido por Thomas :).

