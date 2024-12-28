/*
 * Nome: InterfaceCliente.java
 * Descrição: permite a interação entre o utilizador e o serviço utilizando.
 */

import java.io.*;
import java.util.*;

/*
 * Classe InterfaceCliente
 * 
 * Esta classe proporciona um menu para que o utilizador interaja com o sistema, através
 * da utilização de métodos de uma biblioteca relativa ao cliente.
 */

public class InterfaceCliente {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.print("Digite a porta do servidor: ");
            int porta = Integer.parseInt(scanner.nextLine());

            // estabelece conexão
            BibliotecaCliente.conectar(porta);
            
            int flag = 0;      
            while (flag == 0){
                System.out.println("1. Registar\n2. Autenticar\n3. Sair");
                System.out.print("Escolha uma opção: ");
                int opcaoInicial = Integer.parseInt(scanner.nextLine());
            
                if (opcaoInicial == 1) {
                    System.out.print("Digite o seu nome de utilizador: ");
                    String nome = scanner.nextLine();
                    System.out.print("Insira a sua senha: ");
                    String senha = scanner.nextLine();
                    int res = BibliotecaCliente.registarUtilizador(nome, senha);
                    if (res == 1){
                        flag = 1;
                    }    
                } else if (opcaoInicial == 2) {
                    System.out.print("Digite o seu nome de utilizador: ");
                    String nome = scanner.nextLine();
                    System.out.print("Insira a sua senha: ");
                    String senha = scanner.nextLine();
                    int res = BibliotecaCliente.autenticarUtilizador(nome, senha);
                    if (res == 1){
                        flag = 1;
                    }
                } else if (opcaoInicial == 3) {
                    BibliotecaCliente.fecharConexao();
                    System.out.println("Conexão encerrada");
                    return;
                } else {
                    System.out.println("Opção inválida.");
                    return;
                }
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
        } finally {
            scanner.close();
        }
    }
}