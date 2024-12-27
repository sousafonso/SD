/*
 * @description: Classe que implementa a interface do cliente.
 */

// InterfaceCliente.java
import java.io.*;
import java.util.*;
import java.net.Socket;

public class InterfaceCliente {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);

            System.out.print("Digite a porta do servidor: ");
            int porta = Integer.parseInt(scanner.nextLine());

            // Estabelecer conexão
            BibliotecaCliente.conectar(porta);

            System.out.println("1. Registar\n2. Autenticar\n3. Sair");
            System.out.print("Escolha uma opção: ");
            int opcaoInicial = Integer.parseInt(scanner.nextLine());

            if (opcaoInicial == 1) {
                System.out.println("Digite o seu nome de utilizador: ");
                String nome = scanner.nextLine();
                System.out.println("Insira a sua senha: ");
                String senha = scanner.nextLine();
                BibliotecaCliente.registarUtilizador(nome, senha);
                System.out.println("Registo bem sucedido. Efetue a autenticação");                
            } else if (opcaoInicial == 2) {
                System.out.println("Digite o seu nome de utilizador: ");
                String nome = scanner.nextLine();
                System.out.println("Insira a sua senha: ");
                String senha = scanner.nextLine();
                BibliotecaCliente.autenticarUtilizador(nome, senha);
                System.out.println("Auntenticação bem-sucedida.");
            } else if (opcaoInicial == 3) {
                BibliotecaCliente.fecharConexao();
                System.out.println("Conexão encerrada");
                return;
            } else {
                System.out.println("Opção inválida.");
                return;
            }

            boolean executando = true;
            while (executando) {
                System.out.println("1. PUT\n2. GET\n3. MULTIPUT\n4. MULTIGET\n5. GETWHEN\n6. Sair");
                System.out.print("Escolha uma opção: ");
                int opcao = Integer.parseInt(scanner.nextLine());

                switch (opcao) {
                    case 1:
                        System.out.print("Digite a chave: ");
                        String chave = scanner.nextLine();
                        System.out.print("Digite o valor: ");
                        String valor = scanner.nextLine();
                        BibliotecaCliente.put(chave, valor.getBytes());
                        break;
                    case 2:
                        System.out.print("Digite a chave: ");
                        chave = scanner.nextLine();
                        byte[] resultado = BibliotecaCliente.get(chave);
                        if (resultado != null) {
                            System.out.println("Valor: " + new String(resultado));
                        }
                        break;
                    case 3:
                        Map<String, byte[]> pares = new HashMap<>();
                        System.out.print("Digite o número de pares: ");
                        int numPares = Integer.parseInt(scanner.nextLine());
                        for (int i = 0; i < numPares; i++) {
                            System.out.print("Digite a chave: ");
                            chave = scanner.nextLine();
                            System.out.print("Digite o valor: ");
                            valor = scanner.nextLine();
                            pares.put(chave, valor.getBytes());
                        }
                        BibliotecaCliente.multiPut(pares);
                        break;
                    case 4:
                        Set<String> chaves = new HashSet<>();
                        System.out.print("Digite o número de chaves: ");
                        int numChaves = Integer.parseInt(scanner.nextLine());
                        for (int i = 0; i < numChaves; i++) {
                            System.out.print("Digite a chave: ");
                            chave = scanner.nextLine();
                            chaves.add(chave);
                        }
                        Map<String, byte[]> resultados = BibliotecaCliente.multiGet(chaves);
                        for (Map.Entry<String, byte[]> entry : resultados.entrySet()) {
                            System.out.println("Chave: " + entry.getKey() + ", Valor: " + new String(entry.getValue()));
                        }
                        break;
                    case 5:
                        System.out.print("Digite a chave: ");
                        chave = scanner.nextLine();
                        System.out.print("Digite a chave condicional: ");
                        String chaveCond = scanner.nextLine();
                        System.out.print("Digite o valor condicional: ");
                        valor = scanner.nextLine();
                        resultado = BibliotecaCliente.getWhen(chave, chaveCond, valor.getBytes());
                        if (resultado != null) {
                            System.out.println("Valor: " + new String(resultado));
                        }
                        break;
                    case 6:
                        executando = false;
                        BibliotecaCliente.fecharConexao();
                        System.out.println("Conexão encerrada");
                        break;
                    default:
                        System.out.println("Opção inválida.");
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
