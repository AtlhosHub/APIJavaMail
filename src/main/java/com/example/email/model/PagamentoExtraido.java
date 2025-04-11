package com.example.email.model;

public class PagamentoExtraido {
    public String nome_remetente;
    public String nome_destinatario;
    public String valor;
    public String data_hora;
    public String tipo;
    public String banco_origem;
    public String banco_destino;
    public String codigo_transacao;

    @Override
    public String toString() {
        return "PagamentoExtraido{" +
                "nome_remetente='" + nome_remetente + '\'' +
                ", nome_destinatario='" + nome_destinatario + '\'' +
                ", valor='" + valor + '\'' +
                ", data_hora='" + data_hora + '\'' +
                ", tipo='" + tipo + '\'' +
                ", banco_origem='" + banco_origem + '\'' +
                ", banco_destino='" + banco_destino + '\'' +
                ", codigo_transacao='" + codigo_transacao + '\'' +
                '}';
    }
}
