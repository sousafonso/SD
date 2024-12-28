/*
 * @description: Classe que implementa a interface de comunicação com o servidor.
 */

import java.io.*;
import java.util.*;

public class BibliotecaCliente {
    private static Cliente cliente; 

    public static void conectar(int porta) throws IOException {
        cliente = new Cliente(porta);
    }

    public static int registarUtilizador(String nome, String senha) throws IOException {
        return cliente.registar(nome, senha);
    }

    public static int autenticarUtilizador(String nome, String senha) throws IOException {
        return cliente.autenticar(nome, senha);
    }

    public static void put(String key, byte[] value) throws IOException {
        cliente.put(key, value);
    }

    public static byte[] get(String key) throws IOException {
        return cliente.get(key);
    }

    public static void multiPut(Map<String, byte[]> pairs) throws IOException {
        cliente.multiPut(pairs);
    }

    public static Map<String, byte[]> multiGet(Set<String> keys) throws IOException {
        return cliente.multiGet(keys);
    }

    public static byte[] getWhen(String key, String keyCond, byte[] valueCond) throws IOException {
        return cliente.getWhen(key, keyCond, valueCond);
    }

    public static void fecharConexao() throws IOException {
        cliente.fechar();
    }
}