// Servidor.java
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {
    private static final Map<String, byte[]> armazenamento = new ConcurrentHashMap<>(); // Simula uma base de dados
    private static final Set<String> utilizadores = ConcurrentHashMap.newKeySet(); // Simula uma base de dados de utilizadores
    private static final int MAX_SESSOES = 5; // Exemplo de limite de conexões concorrentes
    private static final Semaphore semaphore = new Semaphore(MAX_SESSOES); // Semáforo para controlar o número de sessões

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Servidor iniciado na porta 12345...");
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                if (semaphore.tryAcquire()) {
                    new Thread(new AtendedorDeCliente(clienteSocket)).start();
                } else {
                    clienteSocket.close();
                    System.out.println("Conexão recusada: limite de sessões atingido.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean autenticarUsuario(String nome, String senha) {
        return utilizadores.contains(nome + senha);
    }

    public static void registarUsuario(String nome, String senha) {
        utilizadores.add(nome + senha);
    }

    static class AtendedorDeCliente implements Runnable {
        private final Socket socket;

        AtendedorDeCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream entrada = new DataInputStream(socket.getInputStream());
                 DataOutputStream saida = new DataOutputStream(socket.getOutputStream())) {

                // Processamento de comandos
                while (true) {
                    String comando = entrada.readUTF();
                    switch (comando) {
                        case "REGISTAR":
                            String novoUsuario = entrada.readUTF();
                            String novaSenha = entrada.readUTF();
                            registarUsuario(novoUsuario, novaSenha);
                            saida.writeUTF("Registro bem-sucedido.");
                            break;
                        case "AUTENTICAR":
                            String usuario = entrada.readUTF();
                            String senha = entrada.readUTF();
                            if (!autenticarUsuario(usuario, senha)) {
                                saida.writeUTF("Autenticação falhou.");
                                return;
                            }
                            saida.writeUTF("Autenticação bem-sucedida.");
                            break;
                        case "PUT":
                            String chave = entrada.readUTF();
                            int tamanhoValor = entrada.readInt();
                            byte[] valor = new byte[tamanhoValor];
                            entrada.readFully(valor);
                            put(chave, valor);
                            saida.writeUTF("PUT OK");
                            break;
                        case "GET":
                            chave = entrada.readUTF();
                            byte[] resultado = get(chave);
                            if (resultado != null) {
                                saida.writeInt(resultado.length);
                                saida.write(resultado);
                            } else {
                                saida.writeInt(-1);
                            }
                            break;
                        // case "MULTIPUT":
                        //     int numPares = entrada.readInt();
                        //     Map<String, byte[]> pares = new HashMap<>();
                        //     for (int i = 0; i < numPares; i++) {
                        //         String chave = entrada.readUTF();
                        //         int tamanhoValor = entrada.readInt();
                        //         byte[] valor = new byte[tamanhoValor];
                        //         entrada.readFully(valor);
                        //         pares.put(chave, valor);
                        //     }
                        //     multiPut(pares);
                        //     saida.writeUTF("MULTIPUT OK");
                        //     break;
                        // case "MULTIGET":
                        //     int numChaves = entrada.readInt();
                        //     Set<String> chaves = new HashSet<>();
                        //     for (int i = 0; i < numChaves; i++) {
                        //         chaves.add(entrada.readUTF());
                        //     }
                        //     Map<String, byte[]> resultados = multiGet(chaves);
                        //     saida.writeInt(resultados.size());
                        //     for (Map.Entry<String, byte[]> entry : resultados.entrySet()) {
                        //         saida.writeUTF(entry.getKey());
                        //         saida.writeInt(entry.getValue().length);
                        //         saida.write(entry.getValue());
                        //     }
                        //     break;
                        // case "GETWHEN":
                        //     chave = entrada.readUTF();
                        //     String chaveCond = entrada.readUTF();
                        //     int tamanhoValorCond = entrada.readInt();
                        //     byte[] valorCond = new byte[tamanhoValorCond];
                        //     entrada.readFully(valorCond);
                        //     resultado = getWhen(chave, chaveCond, valorCond);
                        //     if (resultado != null) {
                        //         saida.writeInt(resultado.length);
                        //         saida.write(resultado);
                        //     } else {
                        //         saida.writeInt(-1);
                        //     }
                        //     break;
                        default:
                            saida.writeUTF("Comando desconhecido.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private synchronized void put(String key, byte[] value) {
            armazenamento.put(key, value);
        }

        private synchronized byte[] get(String key) {
            return armazenamento.get(key);
        }

        private synchronized void multiPut(Map<String, byte[]> pairs) {
            armazenamento.putAll(pairs);
        }

        private synchronized Map<String, byte[]> multiGet(Set<String> keys) {
            Map<String, byte[]> resultados = new HashMap<>();
            for (String key : keys) {
                if (armazenamento.containsKey(key)) {
                    resultados.put(key, armazenamento.get(key));
                }
            }
            return resultados;
        }

        private synchronized byte[] getWhen(String key, String keyCond, byte[] valueCond) {
            while (!Arrays.equals(armazenamento.get(keyCond), valueCond)) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return armazenamento.get(key);
        }
    }
}
