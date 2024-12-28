import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public class ClienteTeste {
    private static final int NUM_CLIENTES = 10; // Número de clientes simultâneos
    private static final int NUM_OPERACOES = 1000; // Operações por cliente
    private static final ConcurrentMap<String, LongAdder> temposTotais = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, LongAdder> contadoresOperacoes = new ConcurrentHashMap<>();

    static {
        // Inicializa os contadores
        temposTotais.put("PUT", new LongAdder());
        temposTotais.put("GET", new LongAdder());
        temposTotais.put("MULTIPUT", new LongAdder());
        temposTotais.put("MULTIGET", new LongAdder()); 
        temposTotais.put("GETWHEN", new LongAdder());
        contadoresOperacoes.put("PUT", new LongAdder());
        contadoresOperacoes.put("GET", new LongAdder());
        contadoresOperacoes.put("MULTIPUT", new LongAdder());
        contadoresOperacoes.put("MULTIGET", new LongAdder());
        contadoresOperacoes.put("GETWHEN", new LongAdder());
    }

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENTES);

        for (int i = 0; i < NUM_CLIENTES; i++) {
            System.out.println("Cliente " + i);
            executor.submit(() -> {
                try {
                    Cliente cliente = new Cliente(12345); // Porta do servidor
                    Random random = new Random();

                    for (int j = 0; j < NUM_OPERACOES; j++) {
                        int operacao = random.nextInt(5); // Escolher operação aleatória
                        String key = "chave" + random.nextInt(10);
                        String value = "valor" + random.nextInt(10);

                        long inicio, fim;

                        switch (operacao) {
                            case 0: // PUT
                                inicio = System.nanoTime();
                                cliente.put(key, value.getBytes());
                                fim = System.nanoTime();
                                temposTotais.get("PUT").add(fim - inicio);
                                contadoresOperacoes.get("PUT").increment();
                                break;
                            case 1: // GET
                                inicio = System.nanoTime();
                                cliente.get(key);
                                fim = System.nanoTime();
                                temposTotais.get("GET").add(fim - inicio);
                                contadoresOperacoes.get("GET").increment();
                                break;
                            case 2: // MULTIPUT
                                Map<String, byte[]> pairs = new HashMap<>();
                                for (int k = 0; k < 3; k++) {
                                    pairs.put("chave" + random.nextInt(10), ("valor" + random.nextInt(10)).getBytes());
                                }
                                inicio = System.nanoTime();
                                cliente.multiPut(pairs);
                                fim = System.nanoTime();
                                temposTotais.get("MULTIPUT").add(fim - inicio);
                                contadoresOperacoes.get("MULTIPUT").increment();
                                break;
                            case 3: // GETWHEN
                                String condKey = "chave" + random.nextInt(10);
                                String expectedValue = "valor" + random.nextInt(10);
                                cliente.put(condKey, expectedValue.getBytes());
                                cliente.put(key, value.getBytes());
                                inicio = System.nanoTime();
                                cliente.getWhen(key, condKey, expectedValue.getBytes());
                                fim = System.nanoTime();
                                temposTotais.get("GETWHEN").add(fim - inicio);
                                contadoresOperacoes.get("GETWHEN").increment();
                                break;
                            case 4:// MULTIGET
                                Set<String> keys = new HashSet<>();
                                for (int k = 0; k < 3; k++) {
                                    keys.add("chave" + random.nextInt(10));
                                }
                                inicio = System.nanoTime();
                                cliente.multiGet(keys);
                                fim = System.nanoTime();
                                temposTotais.get("MULTIGET").add(fim - inicio);
                                contadoresOperacoes.get("MULTIGET").increment();
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
        exibirResultados();
    }

    private static void exibirResultados() {
        temposTotais.forEach((operacao, tempoTotal) -> {
            long totalOperacoes = contadoresOperacoes.get(operacao).sum();
            if (totalOperacoes > 0) {
                System.out.println(operacao + " - Latência Média: " + (tempoTotal.sum() / totalOperacoes) + " ns");
            } else {
                System.out.println(operacao + " - Nenhuma operação realizada.");
            }
        });
    }
}
