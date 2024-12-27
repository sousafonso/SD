import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    public int registar(String nome, String senha) throws IOException {
        saida.writeUTF("REGISTAR");
        saida.writeUTF(nome);
        saida.writeUTF(senha);
        String resposta = entrada.readUTF();
        System.out.println("Resposta do servidor: " + resposta);
        if ("Utilizador já registado".equals(resposta)) {
            return 0;
        }
        else return 1;
    }

    public int autenticar(String nome, String senha) throws IOException {
        saida.writeUTF("AUTENTICAR");
        saida.writeUTF(nome);
        saida.writeUTF(senha);
        String resposta = entrada.readUTF();
        System.out.println("Resposta do servidor: " + resposta);
        if("Autenticação falhou.".equals(resposta)) {
            return 0;
        }
        else return 1;
    }

    public void put(String key, byte[] value) throws IOException {
        saida.writeUTF("PUT");
        saida.writeUTF(key);
        saida.writeInt(value.length);
        saida.write(value);
        System.out.println("Resposta do servidor: " + entrada.readUTF());
    }

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

    public Map<String, byte[]> multiGet(Set<String> keys) throws IOException {
        saida.writeUTF("MULTIGET");
        saida.writeInt(keys.size());
        for (String key : keys) {
            saida.writeUTF(key);
        }
        int numResultados = entrada.readInt();
        if (numResultados == 0) { 
            System.out.println("Não existe valor associado às chaves pedidas.");
        }
        Map<String, byte[]> resultados = new HashMap<>();
        for (int i = 0; i < numResultados; i++) {
            String chave = entrada.readUTF();
            int tamanhoValor = entrada.readInt();
            byte[] valor = new byte[tamanhoValor];
            entrada.readFully(valor);
            resultados.put(chave, valor);
        }
        return resultados;
    }

    public byte[] getWhen(String key, String keyCond, byte[] valueCond) throws IOException {
        saida.writeUTF("GETWHEN");
        saida.writeUTF(key);
        saida.writeUTF(keyCond);
        saida.writeInt(valueCond.length);
        saida.write(valueCond);
        int tamanho = entrada.readInt();
        if (tamanho != -1) {
            byte[] valor = new byte[tamanho];
            entrada.readFully(valor);
            return valor;
        } else {
            System.out.println("Condição não satisfeita.");
            return null;
        }
    }

    public void fechar() throws IOException {
        saida.writeUTF("EXIT");
        saida.flush();
    }
}