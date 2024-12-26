// Servidor.java
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Servidor {
    private static final Map<String, byte[]> armazenamento = new ConcurrentHashMap<>(); // Simula uma base de dados
    private static final Set<String> utilizadores = ConcurrentHashMap.newKeySet(); // Simula uma base de dados de utilizadores
    private static final int MAX_SESSOES = 5; // Exemplo de limite de conexões concorrentes
    private static final Semaphore semaphore = new Semaphore(MAX_SESSOES); // Semáforo para controlar o número de sessões
    private static final Lock lock = new ReentrantLock();
    private static final Lock sessionLock = new ReentrantLock();
    private static final Condition sessionCondition = sessionLock.newCondition();
    private static final Condition condition = lock.newCondition();
    private static int currentSessions = 0;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Servidor iniciado na porta 12345...");
            while (true) {
                Socket clienteSocket = serverSocket.accept();

                new Thread(() -> {
                    lock.lock();
                    try {
                        while (currentSessions >= MAX_SESSOES) {
                            System.out.println("Sessões máximas atingidas. Cliente à espera.");
                            condition.await();
                        }
                        currentSessions++;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    } finally {
                        lock.unlock();
                    }

                    try {
                        new AtendedorDeCliente(clienteSocket);
                    } finally {
                        lock.lock();
                        try {
                            currentSessions--;
                            condition.signal();
                        } finally {
                            lock.unlock();
                        }
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean autenticarUser(String nome, String senha) {
        String chaveUtilizador = nome + ":" + senha; 
        return utilizadores.contains(chaveUtilizador);
    }

    public static boolean registarUser(String nome, String senha) {
        String chaveUtilizador = nome + ":" + senha;
        if (utilizadores.contains(chaveUtilizador)) return false;
        utilizadores.add(chaveUtilizador);
        return true;
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
                            String novoUtilizador = entrada.readUTF();
                            String novaSenha = entrada.readUTF();
                            if (registarUser(novoUtilizador, novaSenha)){
                                saida.writeUTF("Registo bem-sucedido.");
                            } else {
                                saida.writeUTF("Utilizador já registado");
                            }
                            break;
                        case "AUTENTICAR":
                            String utilizador = entrada.readUTF();
                            String senha = entrada.readUTF();
                            if (!autenticarUser(utilizador, senha)) {
                                saida.writeUTF("Autenticação falhou.");
                                return;
                            } else {
                                saida.writeUTF("Autenticação bem-sucedida.");

                            }
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
                        case "MULTIPUT":
                            int numPares = entrada.readInt();
                            Map<String, byte[]> pares = new HashMap<>();
                            for (int i = 0; i < numPares; i++) {
                                chave = entrada.readUTF();
                                tamanhoValor = entrada.readInt();
                                valor = new byte[tamanhoValor];
                                entrada.readFully(valor);
                                pares.put(chave, valor);
                            }
                            multiPut(pares);
                            saida.writeUTF("MULTIPUT OK");
                            break;
                        case "MULTIGET":
                            int numChaves = entrada.readInt();
                            Set<String> chaves = new HashSet<>();
                            for (int i = 0; i < numChaves; i++) {
                                chaves.add(entrada.readUTF());
                            }
                            Map<String, byte[]> resultados = multiGet(chaves);
                            saida.writeInt(resultados.size());
                            for (Map.Entry<String, byte[]> entry : resultados.entrySet()) {
                                saida.writeUTF(entry.getKey());
                                saida.writeInt(entry.getValue().length);
                                saida.write(entry.getValue());
                            }
                            break;
                        case "GETWHEN":
                            chave = entrada.readUTF();
                            String chaveCond = entrada.readUTF();
                            int tamanhoValorCond = entrada.readInt();
                            byte[] valorCond = new byte[tamanhoValorCond];
                            entrada.readFully(valorCond);
                            resultado = getWhen(chave, chaveCond, valorCond);
                            if (resultado != null) {
                                saida.writeInt(resultado.length);
                                saida.write(resultado);
                            } else {
                                saida.writeInt(-1);
                            }
                            break;
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

        private void put(String key, byte[] value) {
            lock.lock();
            try {
                armazenamento.put(key, value);
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }

        private byte[] get(String key) {
            lock.lock();
            try {
                return armazenamento.get(key);
            } finally {
                lock.unlock();
            }
        }

        private void multiPut(Map<String, byte[]> pairs) {
            lock.lock();
            try {
                armazenamento.putAll(pairs);
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }

        private Map<String, byte[]> multiGet(Set<String> keys) {
            lock.lock();
            try {
                Map<String, byte[]> resultados = new HashMap<>();
                for (String key : keys) {
                    if (armazenamento.containsKey(key)) {
                        resultados.put(key, armazenamento.get(key));
                    }
                }
                return resultados;
            } finally {
                lock.unlock();
            }
        }

        private byte[] getWhen(String key, String keyCond, byte[] valueCond) {
            lock.lock();
            try {
                while (!Arrays.equals(armazenamento.get(keyCond), valueCond)) {
                    condition.await();
                }
                return armazenamento.get(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                lock.unlock();
            }
        }
    }
}
