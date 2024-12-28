import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ClienteTeste {
    private static final int NUM_CLIENTES = 10; // Número de clientes simultâneos
    private static final int NUM_OPERACOES = 10; // Operações por cliente

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENTES);

        for (int i = 0; i < NUM_CLIENTES; i++) {
            executor.submit(() -> {
                try {
                    Cliente cliente = new Cliente(12345); // Porta do servidor
                    Random random = new Random();

                    for (int j = 0; j < NUM_OPERACOES; j++) {
                        int operacao = random.nextInt(4); // Escolher operação aleatória
                        String key = "chave" + random.nextInt(10); // Usar um conjunto limitado de chaves
                        String value = "valor" + random.nextInt(10);

                        switch (operacao) {
                            case 0: // PUT
                                cliente.put(key, value.getBytes());
                                System.out.println("PUT: " + key + " -> " + value);
                                break;
                            case 1: // GET
                                byte[] resultado = cliente.get(key);
                                if (resultado != null) {
                                    System.out.println("GET: " + key + " -> " + new String(resultado));
                                }
                                break;
                            case 2: // MULTIPUT
                                Map<String, byte[]> pairs = new HashMap<>();
                                for (int k = 0; k < 3; k++) { // Adiciona 3 pares ao MULTIPUT
                                    String multiKey = "chave" + random.nextInt(10);
                                    String multiValue = "valor" + random.nextInt(10);
                                    pairs.put(multiKey, multiValue.getBytes());
                                }
                                cliente.multiPut(pairs);
                                System.out.println("MULTIPUT realizado.");
                                break;
                            case 3: // GETWHEN
                                // Garante que o condKey está configurado corretamente
                                String condKey = "chave" + random.nextInt(10);
                                String expectedValue = "valor" + random.nextInt(10);

                                // Configura a chave condicional
                                cliente.put(condKey, expectedValue.getBytes());
                                System.out.println("PUT para GETWHEN: " + condKey + " -> " + expectedValue);

                                // Garante que o valor condicional estará disponível
                                cliente.put(key, value.getBytes());

                                byte[] getWhenResult = cliente.getWhen(key, condKey, expectedValue.getBytes());
                                if (getWhenResult != null) {
                                    System.out.println("GETWHEN: " + key + " -> " + new String(getWhenResult));
                                }
                                break;
                        }
                    }

                    cliente.fechar();
                } catch (IOException e) {
                    System.err.println("Erro no cliente: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Testes concluídos.");
    }
}
