/*
 * Nome: Cliente.java
 * Descrição: implementação da comunicação entre o cliente e o servidor no serviço.
 */

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
 * Classe Cliente
 * 
 * Esta classe é responsável por enviar os comandos ao servidor e receber as respostas
 * deste, garantindo que as operações sejam realizadas corretamente.
 */

public class Cliente {
    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream saida;
    private static String HOST = "localhost"; 

    public Cliente(int porta) throws IOException {
        this.socket = new Socket(HOST, porta);
        this.entrada = new DataInputStream(socket.getInputStream());
        this.saida = new DataOutputStream(socket.getOutputStream());
    }

    /*
     * Método registar
     * 
     * Envia um pedido de registo ao servidor.
     */

    public int registar(String nome, String senha) throws IOException {
        saida.writeUTF("REGISTAR");
        saida.writeUTF(nome);
        saida.writeUTF(senha);
        String resposta = entrada.readUTF();
        System.out.println("Resposta do servidor: " + resposta);
        if ("Utilizador já registado".equals(resposta)) {
            return 0;
        }
        else return 1;     // registo bem sucedido
    }

    /*
     * Método autenticar
     * 
     * Verifica as credenciais do utilizador.
     */

    public int autenticar(String nome, String senha) throws IOException {
        saida.writeUTF("AUTENTICAR");
        saida.writeUTF(nome);
        saida.writeUTF(senha);
        String resposta = entrada.readUTF();
        System.out.println("Resposta do servidor: " + resposta);
        if("Autenticação falhou.".equals(resposta)) {
            return 0;
        }
        else return 1;      // autenticação bem sucedida
    }

    /*
     * Método put
     * 
     * Envia um par chave-valor ao servidor.
     */

    public void put(String key, byte[] value) throws IOException {
        saida.writeUTF("PUT");
        saida.writeUTF(key);
        saida.writeInt(value.length);
        saida.write(value);
        System.out.println("Resposta do servidor: " + entrada.readUTF());
    }

    /*
     * Método get
     * 
     * Obtém o valor associado a uma dada chave no servidor, devolvendo null caso
     * a chave não exista.
     */

    public byte[] get(String key) throws IOException {
        saida.writeUTF("GET");
        saida.writeUTF(key);
        int tamanho = entrada.readInt();
        if (tamanho != -1) {
            byte[] valor = new byte[tamanho];
            entrada.readFully(valor);
            return valor;
        } else {
            System.out.println("Chave não encontrada.");
            return null;
        }
    }

    /*
     * Método multiPut
     * 
     * Responsável por enviar vários pares chave-valor ao servidor.
     */

    public void multiPut(Map<String, byte[]> pairs) throws IOException {
        saida.writeUTF("MULTIPUT");
        saida.writeInt(pairs.size());
        for (Map.Entry<String, byte[]> entry : pairs.entrySet()) {
            saida.writeUTF(entry.getKey());
            saida.writeInt(entry.getValue().length);
            saida.write(entry.getValue());
        }
        System.out.println("Resposta do servidor: " + entrada.readUTF());
    }

    /*
     * Método multiGet
     * 
     * Obtém os valores associados a várias chaves no servidor.
     */

    public Map<String, byte[]> multiGet(Set<String> keys) throws IOException {
        saida.writeUTF("MULTIGET");
        saida.writeInt(keys.size());
        for (String key : keys) {
            saida.writeUTF(key);
        }
        int numResultados = entrada.readInt();     // lê o número de resultados
        if (numResultados == 0) { 
            System.out.println("Não existe valor associado às chaves pedidas.");
        }
        Map<String, byte[]> resultados = new HashMap<>();
        for (int i = 0; i < numResultados; i++) {       // para cada resultado lê a chave, o comprimento do valor e o valor
            String chave = entrada.readUTF(); 
            int tamanhoValor = entrada.readInt();
            byte[] valor = new byte[tamanhoValor];
            entrada.readFully(valor);
            resultados.put(chave, valor);
        }
        return resultados;
    }

    /*
     * Método getWhen
     * 
     * Bloqueia enquanto que uma condição seja satisfeita no servidor.
     */

    public byte[] getWhen(String key, String keyCond, byte[] valueCond) throws IOException {
        saida.writeUTF("GETWHEN");
        saida.writeUTF(key);
        saida.writeUTF(keyCond);
        saida.writeInt(valueCond.length);
        saida.write(valueCond);
        int tamanho = entrada.readInt();
        if (tamanho != -1) {
            byte[] valor = new byte[tamanho];     
            entrada.readFully(valor);           // lê o valor
            return valor;                       // retorna-o
        } else {
            System.out.println("Condição não satisfeita.");
            return null;
        }
    }

    /*
     * Método fechar
     * 
     * Envia o comando para fechar a conexão com o servidor.
     */

    public void fechar() throws IOException {
        saida.writeUTF("Sair");
        saida.flush();
    }
}