/*
 * Nome: BibliotecaCliente.java
 * Descrição: abstrai a comunicação com o servidor.
 */

import java.io.*;
import java.util.*;

/*
 * Classe BibliotecaCliente.
 * 
 * Esta classe facilita a troca de informação entre o cliente e o servidor, sem que a 
 * interface precise de lidar diretamente com os detalhes da comunicação. Para isso,
 * utiliza uma instância da classe Cliente para serem enviadas mensagens ao servidor.
 */

public class BibliotecaCliente {
    private static Cliente cliente; 

    // cria uma conexão com o servidor
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