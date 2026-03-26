import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SistemaBancario {

    public record Transacao(
            String agencia, String conta, String banco, String titular,
            String tipo, LocalDateTime dataHora, BigDecimal valor
    ) {}

    public static void main(String[] args) {
        String caminhoArquivo = "operacoes_100.csv";
        // Ajustado para o formato ISO (com o 'T' no meio)
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        System.out.println(">>> Processando arquivo: " + caminhoArquivo);

        try {
            List<Transacao> listaLimpa = Files.lines(Paths.get(caminhoArquivo))
                    .skip(1)
                    .filter(linha -> !linha.isBlank())
                    .map(linha -> converterParaTransacao(linha, formatter))
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            // Agrupamento por Titular e Conta
            Map<String, BigDecimal> saldos = listaLimpa.stream()
                    .collect(Collectors.groupingBy(
                            t -> String.format("Titular: %-10s | Conta: %s (%s)", t.titular(), t.conta(), t.banco()),
                            Collectors.reducing(
                                    BigDecimal.ZERO,
                                    t -> t.tipo().equalsIgnoreCase("DEPOSITO") ? t.valor() : t.valor().negate(),
                                    BigDecimal::add
                            )
                    ));

            exibirRelatorio(saldos);

        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }
    }

    private static Transacao converterParaTransacao(String linha, DateTimeFormatter fmt) {
        try {
            String[] col = linha.split(",");
            // Mapeamento baseado na sua imagem:
            // 0:AGENCIA, 1:CONTA, 2:BANCO, 3:TITULAR, 4:OPERACAO, 5:DATAHORA, 6:VALOR
            return new Transacao(
                    col[0].trim(), col[1].trim(), col[2].trim(), col[3].trim(),
                    col[4].trim(), LocalDateTime.parse(col[5].trim(), fmt),
                    new BigDecimal(col[6].trim())
            );
        } catch (Exception e) {
            // Se houver erro em alguma linha, ele avisa aqui
            return null;
        }
    }

    private static void exibirRelatorio(Map<String, BigDecimal> saldos) {
        System.out.println("\n============================================================");
        System.out.println("              EXTRATO BANCÁRIO CONSOLIDADO              ");
        System.out.println("============================================================");
        saldos.forEach((info, saldo) ->
                System.out.printf("%-45s | R$ %10.2f%n", info, saldo)
        );
        System.out.println("============================================================\n");
    }
}