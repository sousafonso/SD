import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Servidor {
    private static final Map<String, byte[]> armazenamento = new ConcurrentHashMap<>(); // Simula uma base de dados
    private static final Map<String, String> utilizadores = new ConcurrentHashMap<>(); 
    private static final int MAX_SESSOES = 1; // limite de conexões concorrentes
    private static final Lock lock = new ReentrantLock();
    // private static final Lock sessionLock = new ReentrantLock();
    private static final Lock keyValueStoreLock = new ReentrantLock();
    // private static final Condition sessionCondition = sessionLock.newCondition();
    private static final Condition condition = lock.newCondition();
    private static int currentSessions = 0;
    private static final Map<String, Condition> chaveConditions = new HashMap<>();

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
                       handleClient(clienteSocket);
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
        return senha.equals(utilizadores.get(nome));
    }

    private static boolean registarUser(String nome, String senha) {
        return utilizadores.putIfAbsent(nome, senha) == null;
    }

    public static void handleClient(Socket socket) {
        try (socket;
             DataInputStream entrada = new DataInputStream(socket.getInputStream());
             DataOutputStream saida = new DataOutputStream(socket.getOutputStream())) {
    
            // Processamento de comandos
            while (true) {
                try {
                    String comando = entrada.readUTF();
                    switch (comando) {
                        case "REGISTAR":
                            String novoUtilizador = entrada.readUTF();
                            String novaSenha = entrada.readUTF();
                            if (registarUser(novoUtilizador, novaSenha)) {
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
                                break;
                            } else {
                                saida.writeUTF("Autenticação bem-sucedida.");
                            }
                            break;
    
                        case "PUT":
                            String chave = entrada.readUTF();
                            int tamanhoValor = entrada.readInt();
                            byte[] valor = new byte[tamanhoValor];
                            entrada.readFully(valor);
                            handlePut(chave, valor);
                            saida.writeUTF("PUT OK");
                            break;
    
                        case "GET":
                            chave = entrada.readUTF();
                            byte[] resultado = handleGet(chave);
                            if (resultado != null) {
                                saida.writeInt(resultado.length);
                                saida.write(resultado);
                            } else {
                                saida.writeInt(-1);
                            }
                            break;
    
                        case "MULTIPUT":
                            handleMultiPut(entrada, saida);
                            break;
    
                        case "MULTIGET":
                            handleMultiGet(entrada,saida);
                            break;
                        case "GETWHEN":
                            chave = entrada.readUTF();
                            String chaveCond = entrada.readUTF();
                            int tamanhoValorCond = entrada.readInt();
                            byte[] valorCond = new byte[tamanhoValorCond];
                            entrada.readFully(valorCond);
                            resultado = handleGetWhen(chave, chaveCond, valorCond);
                            if (resultado != null) {
                                saida.writeInt(resultado.length);
                                saida.write(resultado);
                            } else {
                                saida.writeInt(-1);
                            }
                            break;
    
                        case "EXIT":
                            System.out.println("Cliente desconectado.");
                            return; 
    
                        default:
                            saida.writeUTF("Comando desconhecido.");
                    }
                } catch (EOFException e) {
                    System.out.println("Conexão encerrada pelo cliente.");
                    return; 
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void handlePut(String key, byte[] value) {
        keyValueStoreLock.lock();
        try {
            armazenamento.put(key, value);

            Condition cond = chaveConditions.get(key);
            if (cond != null) {
                cond.signalAll();
            }
        } finally {
            keyValueStoreLock.unlock();
        }
    }

    private static byte[] handleGet(String key) {
        keyValueStoreLock.lock();
        try {
            return armazenamento.get(key);
        } finally {
            keyValueStoreLock.unlock();
        }
    }

    private static void handleMultiPut(DataInputStream in, DataOutputStream out) throws IOException {
        int numPairs = in.readInt();
        Map<String, byte[]> tempStore = new HashMap<>();
        Map<String, byte[]> backupStore = new HashMap<>();
        // List<String> lockedKeys = new ArrayList<>();

        try {
            // Lê todas as entradas do cliente
            for (int i = 0; i < numPairs; i++) {
                String key = in.readUTF();
                int valueSize = in.readInt();
                byte[] value = new byte[valueSize];
                in.readFully(value);
                tempStore.put(key, value);
            }

            // bloqueio global para evitar inconsistências
            keyValueStoreLock.lock();

            // fazer backup das chaves existentes
            for (String key : tempStore.keySet()) {
                if (armazenamento.containsKey(key)) {
                    backupStore.put(key, armazenamento.get(key));
                } else {
                    backupStore.put(key, null);
                }
            }
            for (Map.Entry<String, byte[]> entry : tempStore.entrySet()) {
                armazenamento.put(entry.getKey(), entry.getValue());
    
                // Notificar threads associadas à chave alterada
                Condition cond = chaveConditions.get(entry.getKey());
                if (cond != null) {
                    cond.signalAll();
                }
            }
    
            out.writeUTF("MULTIPUT OK");
        } catch (Exception e) {
            // Reverter alterações em caso de falha
            for (Map.Entry<String, byte[]> entry : backupStore.entrySet()) {
                if (entry.getValue() == null) {
                    armazenamento.remove(entry.getKey());
                } else {
                    armazenamento.put(entry.getKey(), entry.getValue());
                }
            }
            out.writeUTF("Erro ao executar MULTIPUT. Alterações revertidas.");
        } finally {
            keyValueStoreLock.unlock();
        }
    }

    private static void handleMultiGet(DataInputStream in, DataOutputStream out) throws IOException {
        int numKeys = in.readInt();
        List<String> keys = new ArrayList<>();
        Map<String, byte[]> result = new HashMap<>();
      
        try {
            // Ler as chaves solicitadas do cliente
            for (int i = 0; i < numKeys; i++) {
                keys.add(in.readUTF());
            }
        
            keyValueStoreLock.lock(); 

            try {
                // Ler valores correspondentes às chaves
                for (String key : keys) {
                    byte[] value = armazenamento.get(key);
                    if (value != null) {
                        result.put(key, value);
                    }
                }
            } finally {
                keyValueStoreLock.unlock(); 
            }
        
            // Enviar os resultados ao cliente
            out.writeInt(result.size());
            for (Map.Entry<String, byte[]> entry : result.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeInt(entry.getValue().length);
                out.write(entry.getValue());
            }
        } catch (Exception e) {
            // Em caso de erro, não enviar resultados parciais
            out.writeInt(0);
        }
    }

    private static byte[] handleGetWhen(String key, String keyCond, byte[] valueCond) {
        keyValueStoreLock.lock();
        try {
            Condition cond = chaveConditions.computeIfAbsent(keyCond, k -> keyValueStoreLock.newCondition());
            
            // Bloqueia enquanto a condição não for satisfeita
            while (!Arrays.equals(armazenamento.get(keyCond), valueCond)) {
                try {
                    cond.await(); // espera por mudanças específicas em keyCond
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); 
                    return null;
                }
            }
            // Retorna o valor associado à chave solicitada
            return armazenamento.get(key);
        } finally {
            keyValueStoreLock.unlock();
        }
    }
    
}
        
        /*
         * 
         private static byte[] handleGetWhen(String key, String keyCond, byte[] valueCond) {
        keyValueStoreLock.lock(); // Bloqueia o acesso ao armazenamento
        try {
            while (!Arrays.equals(armazenamento.get(keyCond), valueCond)) {
                try {
                    condition.await(); // Espera por uma alteração no armazenamento
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); 
                    return null;
                }
            }
            // dá o valor associado à chave solicitada
            return armazenamento.get(key);
        } finally {
            keyValueStoreLock.unlock(); 
        }
    }
         */