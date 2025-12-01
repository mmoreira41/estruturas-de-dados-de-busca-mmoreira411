import java.text.NumberFormat;

public class Fornecedor implements Comparable<Fornecedor> {
	
	private static int ultimoID = 10_000;
	
	private String nome;
	private int documento;
	private Lista<Produto> produtos;
	
	/**
	 * Cria um fornecedor a partir do nome informado.
	 * O nome deve conter ao menos duas palavras; caso contrário, é lançada IllegalArgumentException.
	 * O documento é gerado sequencialmente a partir do contador estático.
	 * @param nome Nome completo do fornecedor.
	 */
	public Fornecedor(String nome) {
		
		if (nome == null || nome.trim().split("\\s+").length < 2) {
			throw new IllegalArgumentException("Nome do fornecedor inválido. Informe pelo menos duas palavras.");
		}
		
		this.nome = nome.trim();
		this.documento = ultimoID++;
		this.produtos = new Lista<>();
	}
	
	/**
	 * Insere um novo produto no histórico do fornecedor.
	 * Não é permitido inserir produtos nulos.
	 * @param novo Produto a ser associado ao fornecedor.
	 */
	public void adicionarProduto(Produto novo) {
		if (novo == null) {
			throw new IllegalArgumentException("Produto inválido (nulo).");
		}
		produtos.inserirFinal(novo);
	}
	
	public String getNome() {
		return nome;
	}
	
	public int getDocumento() {
		return documento;
	}
	
	public Lista<Produto> getProdutos() {
		return produtos;
	}
	
	@Override
	public String toString() {
		
		NumberFormat inteiro = NumberFormat.getIntegerInstance();
		
		StringBuilder sb = new StringBuilder();
		sb.append("FORNECEDOR: ").append(nome)
		  .append(" | DOC: ").append(inteiro.format(documento))
		  .append("\nProdutos associados:\n")
		  .append(produtos.toString());
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		return documento;
	}
	
	@Override
	public boolean equals(Object obj) {
		try {
			Fornecedor outro = (Fornecedor) obj;
			return this.hashCode() == outro.hashCode();
		} catch (ClassCastException ex) {
			return false;
		}
	}
	
	@Override
	public int compareTo(Fornecedor outro) {
		if (this.documento == outro.documento)
			return 0;
		else if (this.documento < outro.documento)
			return -1;
		else
			return 1;
	}
}


