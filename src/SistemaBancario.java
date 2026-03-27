import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SistemaBancario {

    /**
     * O Record já implementa equals() e hashCode() baseando-se em TODOS os campos.
     * Isso permite que o método .distinct() identifique transações idênticas.
     */
    public record Transacao(
            String agencia, String conta, String banco, String titular,
            String tipo, LocalDateTime dataHora, BigDecimal valor
    ) {}

    public static void main(String[] args) {
        String caminhoArquivo = "operacoes_100.csv";
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        Scanner scanner = new Scanner(System.in);

        try {
            // Lógica de Processamento Inicial
            List<Transacao> todasTransacoes = Files.lines(Paths.get(caminhoArquivo))
                    .skip(1) // Pula o cabeçalho
                    .filter(linha -> !linha.isBlank())
                    .map(linha -> converterParaTransacao(linha, formatter))
                    .filter(Objects::nonNull)
                    .distinct() // <--- REMOVE TRANSAÇÕES REPETIDAS (Mesmo dia, hora, valor e conta)
                    .collect(Collectors.toList());

            int opcao = 0;
            while (opcao != 3) {
                exibirMenuPrincipal();
                try {
                    opcao = scanner.nextInt();
                    scanner.nextLine(); // Limpa o buffer
                } catch (InputMismatchException e) {
                    System.err.println("[!] Erro: Digite apenas números (1, 2 ou 3).");
                    scanner.nextLine();
                    continue;
                }

                switch (opcao) {
                    case 1 -> exibirRelatorioGeral(todasTransacoes);
                    case 2 -> {
                        System.out.print("Digite o número da conta para extrato: ");
                        String contaBusca = scanner.nextLine();
                        exibirExtratoDetalhado(todasTransacoes, contaBusca);
                    }
                    case 3 -> System.out.println("Saindo... Até logo, Katia!");
                    default -> System.out.println("Opção inválida! Tente novamente.");
                }
            }

        } catch (IOException e) {
            System.err.println("Erro crítico: Não foi possível ler o arquivo " + caminhoArquivo);
        } finally {
            scanner.close();
        }
    }

    // --- MÉTODOS DE INTERFACE E LÓGICA ---

    private static void exibirMenuPrincipal() {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("      SISTEMA BANCÁRIO - MENU       ");
        System.out.println("=".repeat(40));
        System.out.println("1. Resumo de Saldos (Todos os Clientes)");
        System.out.println("2. Extrato Detalhado por Conta");
        System.out.println("3. Sair");
        System.out.print("Sua opção: ");
    }

    private static void exibirRelatorioGeral(List<Transacao> transacoes) {
        System.out.println("\n--- RELATÓRIO CONSOLIDADO DE SALDOS ---");

        Map<String, BigDecimal> saldos = transacoes.stream()
                .collect(Collectors.groupingBy(
                        t -> String.format("Titular: %-10s | Conta: %s (%s)", t.titular(), t.conta(), t.banco()),
                        Collectors.reducing(BigDecimal.ZERO,
                                t -> t.tipo().equalsIgnoreCase("DEPOSITO") ? t.valor() : t.valor().negate(),
                                BigDecimal::add)
                ));

        saldos.forEach((info, saldo) ->
                System.out.printf("%-45s | R$ %10.2f%n", info, saldo)
        );
    }

    private static void exibirExtratoDetalhado(List<Transacao> transacoes, String numConta) {
        // Filtra e ordena por data
        List<Transacao> filtradas = transacoes.stream()
                .filter(t -> t.conta().equals(numConta.trim()))
                .sorted(Comparator.comparing(Transacao::dataHora))
                .collect(Collectors.toList());

        if (filtradas.isEmpty()) {
            System.out.println("\n[!] Conta " + numConta + " não encontrada ou sem movimentações.");
            return;
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("EXTRATO DETALHADO - TITULAR: " + filtradas.get(0).titular());
        System.out.println("CONTA: " + numConta + " | BANCO: " + filtradas.get(0).banco());
        System.out.println("-".repeat(60));
        System.out.printf("%-18s | %-10s | %-12s%n", "Data/Hora", "Operação", "Valor");

        BigDecimal saldoAcumulado = BigDecimal.ZERO;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Transacao t : filtradas) {
            BigDecimal valorEfetivo = t.tipo().equalsIgnoreCase("DEPOSITO") ? t.valor() : t.valor().negate();
            saldoAcumulado = saldoAcumulado.add(valorEfetivo);

            System.out.printf("%s | %-10s | R$ %10.2f%n",
                    t.dataHora().format(dtf), t.tipo(), valorEfetivo);
        }

        System.out.println("-".repeat(60));
        System.out.printf("SALDO FINAL CONSOLIDADO:          R$ %10.2f%n", saldoAcumulado);
        System.out.println("=".repeat(60));
    }

    private static Transacao converterParaTransacao(String linha, DateTimeFormatter fmt) {
        try {
            String[] col = linha.split(",");
            // Ordem: AGENCIA(0), CONTA(1), BANCO(2), TITULAR(3), OPERACAO(4), DATAHORA(5), VALOR(6)
            return new Transacao(
                    col[0].trim(), col[1].trim(), col[2].trim(), col[3].trim(),
                    col[4].trim(), LocalDateTime.parse(col[5].trim(), fmt),
                    new BigDecimal(col[6].trim())
            );
        } catch (Exception e) {
            return null; // Linhas com erro de formato são ignoradas
        }
    }
}