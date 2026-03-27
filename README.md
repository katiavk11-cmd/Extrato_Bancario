# 🏦 Sistema de Extrato Bancário em Java

## 🛠️ Arquitetura e Decisões Técnicas

Para garantir a integridade dos dados e a fluidez do sistema, as seguintes escolhas foram feitas:

### 1. Estrutura de Dados: O uso de `Records`
A base do projeto utiliza **Java Records**. Escolhemos esta estrutura por ser imutável e concisa.
* **Vantagem:** O Record implementa automaticamente `equals()` e `hashCode()` baseando-se em todos os campos da transação (Agência, Conta, Valor, etc.). Isso é essencial para a nossa estratégia de limpeza de dados.

### 2. Eliminação de Duplicados: Estratégia de Integridade
Arquivos CSV brutos podem conter registros redundantes por erros de exportação.
* **Estratégia:** Utilizamos o método `.distinct()` da **Java Stream API**.
* **Como funciona:** Como usamos Records, o `.distinct()` compara cada campo. Se duas linhas tiverem exatamente a mesma conta, valor e data/hora, o Java mantém apenas uma, garantindo a correção do saldo final.



### 3. Ordenação Cronológica

* **Estratégia:** Utilizamos `Comparator.comparing(Transacao::dataHora)`.
* **Benefício:** Mesmo que o arquivo CSV esteja fora de ordem, o extrato detalhado sempre apresentará os eventos do mais antigo para o mais recente, facilitando a conferência do saldo progressivo.