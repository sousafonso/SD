/*
 * @description: Classe que implementa a interface de comunicação com o servidor.
 */

// BibliotecaCliente.java
import java.io.*;
import java.net.*;
import java.util.*;

public class BibliotecaCliente {
    private static Cliente cliente; // Instância da classe Cliente

    // Método para conectar ao servidor 
    public static void conectar(String host, int porta) throws IOException {
        cliente = new Cliente(host, porta);
    }

    // Método para registrar o utilizador no servidor
    public static void registrarUtilizador(String nome, String senha) throws IOException {
        cliente.registrar(nome, senha);
    }

    // Método para autenticar o utilizador no servidor
    public static void autenticarUtilizador(String nome, String senha) throws IOException {
        cliente.autenticar(nome, senha);
    }

    // Método para armazenar um valor associado a uma chave
    public static void put(String key, byte[] value) throws IOException {
        cliente.put(key, value);
    }

    // Método para obter o valor associado a uma chave
    public static byte[] get(String key) throws IOException {
        return cliente.get(key);
    }

    // Método para armazenar múltiplos pares chave-valor
    public static void multiPut(Map<String, byte[]> pairs) throws IOException {
        cliente.multiPut(pairs);
    }

    // Método para obter múltiplos pares chave-valor
    public static Map<String, byte[]> multiGet(Set<String> keys) throws IOException {
        return cliente.multiGet(keys);
    }

    // Método para obter valor condicionalmente
    public static byte[] getWhen(String key, String keyCond, byte[] valueCond) throws IOException {
        return cliente.getWhen(key, keyCond, valueCond);
    }

    // Método para fechar a conexão com o servidor
    public static void fecharConexao() throws IOException {
        cliente.fechar();
    }
}