/*
 * Nome: Servidor.java
 * Descrição: implementação de um servidor para um sistema de armazenamento de dados partilhado.
 */


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Classe Servidor
 * 
 * Esta classe implementa o lado do servidor no sistema de armazenamento, suportando as 
 * funcionalidades PUT, GET, MULTIPUT, MULTIGET, GETWHEN e sendo capaz de realizar
 * comunicações com os clientes.
 */ 

public class Servidor {
    private static final Map<String, byte[]> armazenamento = new ConcurrentHashMap<>();
    private static final Map<String, String> utilizadores = new ConcurrentHashMap<>(); 
    private static final int MAX_SESSOES = 100; // máximo de conexões silmutâneas
    private static final Lock lock = new ReentrantLock();
    private static final Lock keyValueStoreLock = new ReentrantLock(); // lock para operações no armazenamento
    private static final Condition condition = lock.newCondition();
    private static int currentSessions = 0;   // sessões ativas
    private static final Map<String, Map<String, Condition>> chaveValorConditions = new ConcurrentHashMap<>();    // usado para o GETWHEN


    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Servidor iniciado na porta 12345...");
            while (true) {
                Socket clienteSocket = serverSocket.accept();

                // gere o número máximo de sessões simultâneas
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
                            condition.signal();    // acorda uma thread à espera de conexão
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

    /*
     * Método autenticarUser
     * 
     * Gere a autenticação dos utilizadores e faz a sua verificação dado o seu nome
     * e palavra passe.
     */

    private static boolean autenticarUser(String nome, String senha) {
        return senha.equals(utilizadores.get(nome));
    }

    /*
     * Método registarUser
     * 
     * Gere o registo de utilizadores e armazena-os num mapa tendo em conta o seu
     * nome e palavra passe.
     */

    private static boolean registarUser(String nome, String senha) {
        return utilizadores.putIfAbsent(nome, senha) == null;
    }

    /*
     * Método handleClient
     * 
     * Este método permite tratar a troca de informação entre o cliente e o servidor,
     * sendo capaz de processar os comandos que são enviados pelos clientes.
     */

    public static void handleClient(Socket socket) {
        try (socket;
             DataInputStream entrada = new DataInputStream(socket.getInputStream());
             DataOutputStream saida = new DataOutputStream(socket.getOutputStream())) {
    
            while (true) {
                try {
                    String comando = entrada.readUTF();        // lê o comando enviado pelo cliente
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
                            saida.writeUTF("Comando PUT bem sucedido");
                            break;
    
                        case "GET":
                            chave = entrada.readUTF();
                            byte[] resultado = handleGet(chave);
                            if (resultado != null) {
                                saida.writeInt(resultado.length);
                                saida.write(resultado);
                            } else {
                                saida.writeInt(-1);     // chave não encontrada
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
    
                        case "Sair":
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

    /*
     * Método handlePut
     * 
     * Responsável por inserir ou atualizar um par chave-valor.
     */

    private static void handlePut(String key, byte[] value) {
        keyValueStoreLock.lock();
        try {
            armazenamento.put(key, value);
    
            Map<String, Condition> valorConditions = chaveValorConditions.get(key);
            
            // acorda threads para a condição específica
            if (valorConditions != null) {
                String valorString = Arrays.toString(value);
                Condition cond = valorConditions.get(valorString);
                if (cond != null) {
                    cond.signalAll(); 
                }
            }
        } finally {
            keyValueStoreLock.unlock();
        }
    }

    /*
     * Método handleGet
     * 
     * Devolve o valor associado a uma chave.
     */

    private static byte[] handleGet(String key) {
        keyValueStoreLock.lock();
        try {
            System.out.println("GET: Cliente pediu o valor da chave '" + key + "'.");
            return armazenamento.get(key);
        } finally {
            keyValueStoreLock.unlock();
        }
    }

    /*
     * Método handleMultiPut
     * 
     * Este método insere vários pares chave-valor de uma vez só e, em caso de erro,
     * reverte as alterações.
     */

    private static void handleMultiPut(DataInputStream in, DataOutputStream out) throws IOException {
        int numPairs = in.readInt();
        Map<String, byte[]> tempStore = new HashMap<>();
        Map<String, byte[]> backupStore = new HashMap<>();

        keyValueStoreLock.lock();
        try {
            // lê todas as entradas do cliente
            for (int i = 0; i < numPairs; i++) {
                String key = in.readUTF();
                int valueSize = in.readInt();
                byte[] value = new byte[valueSize];
                in.readFully(value);
                tempStore.put(key, value);
            }

            // faz backup das chaves existentes
            for (String key : tempStore.keySet()) {
                if (armazenamento.containsKey(key)) {
                    backupStore.put(key, armazenamento.get(key));
                } else {
                    backupStore.put(key, null);
                }
            }

            // atualiza o armazenamento
            for (Map.Entry<String, byte[]> entry : tempStore.entrySet()) {
                String key = entry.getKey();
                byte[] value = entry.getValue();

                armazenamento.put(key, value);
    
                Map<String, Condition> valorConditions = chaveValorConditions.get(key);

                if (valorConditions != null) {
                    String valorString = Arrays.toString(value);
                    Condition cond = valorConditions.get(valorString);
                    if (cond != null) {
                        cond.signalAll(); // acorda threads para a condição específica
                    }
                }
            }
    
            out.writeUTF("Operação MultiPut com sucesso.");
        } catch (Exception e) {
            // reverte as alterações em caso de erro
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

    /*
     * Método handleMultiGet
     * 
     * Devolve o conjunto de pares chave-valor associados a um conjunto de chaves.
     */

    private static void handleMultiGet(DataInputStream in, DataOutputStream out) throws IOException {
        int numKeys = in.readInt();
        List<String> keys = new ArrayList<>();
        Map<String, byte[]> result = new HashMap<>();
      
        try {
            for (int i = 0; i < numKeys; i++) {
                keys.add(in.readUTF());
            }
        
            keyValueStoreLock.lock(); 

            try {
                System.out.println("MULTIGET: Cliente pediu os valores das chaves " + keys + ".");
                for (String key : keys) {
                    byte[] value = armazenamento.get(key);
                    if (value != null) {
                        result.put(key, value);
                    }
                }
            } finally {
                keyValueStoreLock.unlock();     
            }
        
            // envia os resultados ao cliente
            out.writeInt(result.size());
            for (Map.Entry<String, byte[]> entry : result.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeInt(entry.getValue().length);
                out.write(entry.getValue());
            }
        } catch (Exception e) {
            out.writeInt(0);
        }
    }

    /*
     * Método handleGetWhen
     * 
     * Devolve o valor de uma chave "key" quando uma dada chave "keycond" assume um
     * valor específico "valuecond", bloqueando enquanto tal condição não é satisfeita.
     */

    private static byte[] handleGetWhen(String key, String keyCond, byte[] valueCond) {
        // Não bloqueie imediatamente para melhorar a concorrência
        Map<String, Condition> valorConditions;
        Condition cond;
        keyValueStoreLock.lock();
        try {
            System.out.println("GETWHEN: Cliente está à espera que a chave '" + keyCond + "' tenha o valor esperado.");
            
            // Verificação imediata da condição
            if (Arrays.equals(armazenamento.get(keyCond), valueCond)) {
                System.out.println("GETWHEN: Condição já satisfeita. Retornando o valor.");
                return armazenamento.get(key);
            }
    
            valorConditions = chaveValorConditions.computeIfAbsent(keyCond, k -> new HashMap<>());
            String valorCondString = Arrays.toString(valueCond);
            cond = valorConditions.computeIfAbsent(valorCondString, v -> keyValueStoreLock.newCondition());
        } finally {
            keyValueStoreLock.unlock();
        }
    
        // Espera pela condição sem bloquear outras operações
        keyValueStoreLock.lock();
        try {
            while (!Arrays.equals(armazenamento.get(keyCond), valueCond)) {
                try {
                    if (!cond.await(5, TimeUnit.SECONDS)) {
                        System.out.println("GETWHEN: Timeout ao esperar pela condição.");
                        return null; // Timeout, condição não satisfeita
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            System.out.println("GETWHEN: Condição satisfeita. Retornando o valor.");
            return armazenamento.get(key);
        } finally {
            keyValueStoreLock.unlock();
        }
    }
    

    
}
