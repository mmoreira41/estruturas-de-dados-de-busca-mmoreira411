import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Function;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class App {


	/** Nome do arquivo de dados. O arquivo deve estar localizado na raiz do projeto */
    static String nomeArquivoDados;
    
    /** Scanner para leitura de dados do teclado */
    static Scanner teclado;

    /** Quantidade de produtos cadastrados atualmente na lista */
    static int quantosProdutos = 0;

    static AVL<String, Produto> produtosBalanceadosPorNome;
    
    static AVL<Integer, Produto> produtosBalanceadosPorId;
    
    static TabelaHash<Produto, Lista<Pedido>> pedidosPorProduto;
    
    static String nomeArquivoFornecedores;
    
    static AVL<Integer, Fornecedor> fornecedoresBalanceadosPorDocumento;
    
    static TabelaHash<Produto, Lista<Fornecedor>> fornecedoresPorProduto;
    
    static void limparTela() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /** Gera um efeito de pausa na CLI. Espera por um enter para continuar */
    static void pausa() {
        System.out.println("Digite enter para continuar...");
        teclado.nextLine();
    }

    /** Cabeçalho principal da CLI do sistema */
    static void cabecalho() {
        System.out.println("AEDs II COMÉRCIO DE COISINHAS");
        System.out.println("=============================");
    }
   
    static <T extends Number> T lerOpcao(String mensagem, Class<T> classe) {
        
    	T valor;
        
    	System.out.println(mensagem);
    	try {
            valor = classe.getConstructor(String.class).newInstance(teclado.nextLine());
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException 
        		| InvocationTargetException | NoSuchMethodException | SecurityException e) {
            return null;
        }
        return valor;
    }
    
    /** Imprime o menu principal, lê a opção do usuário e a retorna (int).
     * Perceba que poderia haver uma melhor modularização com a criação de uma classe Menu.
     * @return Um inteiro com a opção do usuário.
    */
    static int menu() {
        cabecalho();
        System.out.println("1 - Procurar produto, por id");
        System.out.println("2 - Gravar, em arquivo, pedidos de um produto");
        System.out.println("3 - Relatório de fornecedor, por documento");
        System.out.println("4 - Gravar, em arquivo, fornecedores de um produto");
        System.out.println("0 - Sair");
        System.out.print("Digite sua opção: ");
        try {
        	return Integer.parseInt(teclado.nextLine());
        } catch (NumberFormatException ex) {
        	System.out.println("Opção inválida. Tente novamente.");
        	return -1;
        }
    }
    
    /**
     * Lê os dados de um arquivo-texto e retorna uma árvore de produtos. Arquivo-texto no formato
     * N (quantidade de produtos) <br/>
     * tipo;descrição;preçoDeCusto;margemDeLucro;[dataDeValidade] <br/>
     * Deve haver uma linha para cada um dos produtos. Retorna uma árvore vazia em caso de problemas com o arquivo.
     * @param nomeArquivoDados Nome do arquivo de dados a ser aberto.
     * @return Uma árvore com os produtos carregados, ou vazia em caso de problemas de leitura.
     */
    static <K> AVL<K, Produto> lerProdutos(String nomeArquivoDados, Function<Produto, K> extratorDeChave) {
    	
    	Scanner arquivo = null;
    	int numProdutos;
    	String linha;
    	Produto produto;
    	AVL<K, Produto> produtosCadastrados;
    	K chave;
    	
    	try {
    		arquivo = new Scanner(new File(nomeArquivoDados), Charset.forName("UTF-8"));
    		
    		numProdutos = Integer.parseInt(arquivo.nextLine());
    		produtosCadastrados = new AVL<K, Produto>();
    		
    		for (int i = 0; i < numProdutos; i++) {
    			linha = arquivo.nextLine();
    			produto = Produto.criarDoTexto(linha);
    			// extrai a nova chave (id, nome, etc.) para organizar a árvore conforme a consulta alvo
    			chave = extratorDeChave.apply(produto);
    			produtosCadastrados.inserir(chave, produto);
    		}
    		quantosProdutos = numProdutos;
    		
    	} catch (IOException excecaoArquivo) {
    		produtosCadastrados = new AVL<K, Produto>();
    	} finally {
    		if (arquivo != null) {
    			try {
    				arquivo.close();
    			} catch (Exception ignore) {}
    		}
    	}
    	
    	return produtosCadastrados;
    }
    
    /**
     * Lê os dados de um arquivo-texto de fornecedores e retorna uma árvore AVL.
     * Formato:
     * N (quantidade de fornecedores)
     * nome do fornecedor
     * No momento da criação, são selecionados aleatoriamente até 6 produtos para o fornecedor,
     * e o fornecedor é associado aos seus produtos na tabela hash Produto -> Lista<Fornecedor>.
     */
    static <K> AVL<K, Fornecedor> lerFornecedores(String nomeArquivoDados, Function<Fornecedor, K> extratorDeChave) {
    	
    	Scanner arquivo = null;
    	int numFornecedores;
    	String linha;
    	Fornecedor fornecedor;
    	AVL<K, Fornecedor> fornecedoresCadastrados;
    	K chave;
    	Random sorteio = new Random(84);
    	
    	try {
    		arquivo = new Scanner(new File(nomeArquivoDados), Charset.forName("UTF-8"));
    		
    		numFornecedores = Integer.parseInt(arquivo.nextLine());
    		fornecedoresCadastrados = new AVL<K, Fornecedor>();
    		
    		for (int i = 0; i < numFornecedores; i++) {
    			linha = arquivo.nextLine();
    			fornecedor = new Fornecedor(linha);
    			
    			// associa aleatoriamente até 6 produtos ao fornecedor
    			int quantidade = sorteio.nextInt(7); // 0..6
    			for (int j = 0; j < quantidade; j++) {
    				int id = sorteio.nextInt(quantosProdutos) + 10_000;
    				try {
    					Produto produto = produtosBalanceadosPorId.pesquisar(id);
    					fornecedor.adicionarProduto(produto);
    					associarFornecedorAoProduto(produto, fornecedor);
    				} catch (NoSuchElementException ignore) {
    					// id inexistente: ignora
    				}
    			}
    			
    			chave = extratorDeChave.apply(fornecedor);
    			fornecedoresCadastrados.inserir(chave, fornecedor);
    		}
    		
    	} catch (IOException excecaoArquivo) {
    		fornecedoresCadastrados = new AVL<K, Fornecedor>();
    	} finally {
    		if (arquivo != null) {
    			try {
    				arquivo.close();
    			} catch (Exception ignore) {}
    		}
    	}
    	
    	return fornecedoresCadastrados;
    }
    
    private static void associarFornecedorAoProduto(Produto produto, Fornecedor fornecedor) {
    	
    	Lista<Fornecedor> fornecedoresDoProduto;
    	try {
    		fornecedoresDoProduto = fornecedoresPorProduto.pesquisar(produto);
    	} catch (NoSuchElementException excecao) {
    		fornecedoresDoProduto = new Lista<>();
    		fornecedoresPorProduto.inserir(produto, fornecedoresDoProduto);
    	}
    	fornecedoresDoProduto.inserirFinal(fornecedor);
    }
    
    static <K> Produto localizarProduto(ABB<K, Produto> produtosCadastrados, K procurado) {
    	
    	Produto produto;
    	
    	cabecalho();
    	System.out.println("Localizando um produto...");
    	
    	try {
    		produto = produtosCadastrados.pesquisar(procurado);
    	} catch (NoSuchElementException excecao) {
    		produto = null;
    	}
    	
    	System.out.println("Número de comparações realizadas: " + produtosCadastrados.getComparacoes());
    	System.out.println("Tempo de processamento da pesquisa: " + produtosCadastrados.getTempo() + " ms");
        
    	return produto;
    	
    }
    
    static <K> Fornecedor localizarFornecedor(ABB<K, Fornecedor> fornecedoresCadastrados, K procurado) {
    	
    	Fornecedor fornecedor;
    	
    	cabecalho();
    	System.out.println("Localizando um fornecedor...");
    	
    	try {
    		fornecedor = fornecedoresCadastrados.pesquisar(procurado);
    	} catch (NoSuchElementException excecao) {
    		fornecedor = null;
    	}
    	
    	System.out.println("Número de comparações realizadas: " + fornecedoresCadastrados.getComparacoes());
    	System.out.println("Tempo de processamento da pesquisa: " + fornecedoresCadastrados.getTempo() + " ms");
    	
    	return fornecedor;
    }
    
    /** Localiza um produto na árvore de produtos organizados por id, a partir do código de produto informado pelo usuário, e o retorna. 
     *  Em caso de não encontrar o produto, retorna null */
    static Produto localizarProdutoID(ABB<Integer, Produto> produtosCadastrados) {
        
        Integer idProduto = lerOpcao("Digite o identificador do produto desejado: ", Integer.class);
        
        if (idProduto == null) {
        	System.out.println("Identificador inválido.");
        	return null;
        }
        return localizarProduto(produtosCadastrados, idProduto);
    }
    
    static Fornecedor localizarFornecedorID(ABB<Integer, Fornecedor> fornecedoresCadastrados) {
    	
    	Integer doc = lerOpcao("Digite o documento do fornecedor desejado: ", Integer.class);
    	
    	if (doc == null) {
    		System.out.println("Documento inválido.");
    		return null;
    	}
    	return localizarFornecedor(fornecedoresCadastrados, doc);
    }
    
    /** Localiza um produto na árvore de produtos organizados por nome, a partir do nome de produto informado pelo usuário, e o retorna. 
     *  A busca não é sensível ao caso. Em caso de não encontrar o produto, retorna null */
    static Produto localizarProdutoNome(ABB<String, Produto> produtosCadastrados) {
        
    	String descricao;
    	
    	System.out.println("Digite o nome ou a descrição do produto desejado:");
        descricao = teclado.nextLine();
        
        return localizarProduto(produtosCadastrados, descricao);
    }
    
    private static void mostrarProduto(Produto produto) {
    	
        cabecalho();
        String mensagem = "Dados inválidos para o produto!";
        
        if (produto != null){
            mensagem = String.format("Dados do produto:\n%s", produto);
        }
        
        System.out.println(mensagem);
    }
    
    private static Lista<Pedido> gerarPedidos(int quantidade) {
        Lista<Pedido> pedidos = new Lista<>();
        Random sorteio = new Random(42);
        int quantProdutos;
        int formaDePagamento;
        for (int i = 0; i < quantidade; i++) {
        	formaDePagamento = sorteio.nextInt(2) + 1;
            Pedido pedido = new Pedido(LocalDate.now(), formaDePagamento);
            quantProdutos = sorteio.nextInt(8) + 1;
            for (int j = 0; j < quantProdutos; j++) {
                int id = sorteio.nextInt(7750) + 10_000;
                try {
                	Produto produto = produtosBalanceadosPorId.pesquisar(id);
                	pedido.incluirProduto(produto);
                	// indexa o pedido por produto na tabela hash (endereçamento separado)
                	inserirNaTabela(produto, pedido);
                } catch (NoSuchElementException ignore) {
                	// id sorteado inexistente: ignora
                }
            }
            pedidos.inserirFinal(pedido);
        }
        return pedidos;
    }
    
    private static void inserirNaTabela(Produto produto, Pedido pedido) {
        
    	Lista<Pedido> pedidosDoProduto;
    	
    	try {
    		pedidosDoProduto = pedidosPorProduto.pesquisar(produto);
    	} catch (NoSuchElementException excecao) {
    		// primeira ocorrência deste produto: cria o "bucket" encadeado que armazenará os pedidos
    		pedidosDoProduto = new Lista<>();
    		pedidosPorProduto.inserir(produto, pedidosDoProduto);
    	}
    	// adiciona o pedido ao bucket do produto
    	pedidosDoProduto.inserirFinal(pedido);
    }
    
    static void pedidosDoProduto() {
    	
    	Lista<Pedido> pedidosDoProduto;
    	Produto produto = localizarProdutoID(produtosBalanceadosPorId);
    	if (produto == null) {
    		System.out.println("Produto não encontrado.");
    		return;
    	}
    	String nomeArquivo = "RelatorioProduto" + produto.hashCode() + ".txt";  
    	
        FileWriter arquivoRelatorio = null;
        try {
        	arquivoRelatorio = new FileWriter(nomeArquivo, Charset.forName("UTF-8"));
        	try {
        		// consulta O(1) média: acesso direto aos pedidos pelo produto como chave
        		pedidosDoProduto = pedidosPorProduto.pesquisar(produto);
        		arquivoRelatorio.append(pedidosDoProduto.toString()).append("\n");
        	} catch (NoSuchElementException e) {
        		arquivoRelatorio.append("Nenhum pedido encontrado para o produto informado.\n");
        	}
            System.out.println("Dados salvos em " + nomeArquivo);
        } catch(IOException excecao) {
            System.out.println("Problemas para criar o arquivo " + nomeArquivo + ". Tente novamente");        	
        } finally {
        	if (arquivoRelatorio != null) {
        		try {
        			arquivoRelatorio.close();
        		} catch (IOException ignore) {}
        	}
        }
    }
    
    static String relatorioDeFornecedor() {
    	
    	Fornecedor fornecedor = localizarFornecedorID(fornecedoresBalanceadosPorDocumento);
    	if (fornecedor == null) {
    		return "Fornecedor não encontrado.";
    	}
    	return fornecedor.toString();
    }
    
    static void fornecedoresDoProduto() {
    	
    	Produto produto = localizarProdutoID(produtosBalanceadosPorId);
    	if (produto == null) {
    		System.out.println("Produto não encontrado.");
    		return;
    	}
    	
    	String nomeArquivo = "FornecedoresProduto" + produto.hashCode() + ".txt";
    	
    	FileWriter arquivoRelatorio = null;
    	try {
    		arquivoRelatorio = new FileWriter(nomeArquivo, Charset.forName("UTF-8"));
    		try {
    			Lista<Fornecedor> fornecedores = fornecedoresPorProduto.pesquisar(produto);
    			arquivoRelatorio.append(fornecedores.toString()).append("\n");
    		} catch (NoSuchElementException e) {
    			arquivoRelatorio.append("Nenhum fornecedor encontrado para o produto informado.\n");
    		}
    		System.out.println("Dados salvos em " + nomeArquivo);
    	} catch (IOException excecao) {
    		System.out.println("Problemas para criar o arquivo " + nomeArquivo + ". Tente novamente");
    	} finally {
    		if (arquivoRelatorio != null) {
    			try {
    				arquivoRelatorio.close();
    			} catch (IOException ignore) {}
    		}
    	}
    }
    
	public static void main(String[] args) {
		teclado = new Scanner(System.in, Charset.forName("UTF-8"));
        nomeArquivoDados = "produtos.txt";
        nomeArquivoFornecedores = "fornecedores.txt";
        // Árvore por ID: leitura do arquivo e indexação por chave numérica (id)
        produtosBalanceadosPorId = lerProdutos(nomeArquivoDados, Produto::hashCode);
        // Árvore por nome: reindexação (sem IO), aproveitando os mesmos objetos Produto
        produtosBalanceadosPorNome = new AVL<>(produtosBalanceadosPorId, produto -> produto.descricao, String::compareTo);
        // Tabela hash Produto -> Lista<Pedido>: separação por encadeamento; fator de carga controlado
        pedidosPorProduto = new TabelaHash<>((int)(quantosProdutos * 1.25));
        // Tabela hash Produto -> Lista<Fornecedor>
        fornecedoresPorProduto = new TabelaHash<>((int)(quantosProdutos * 1.25));
        // Árvore de fornecedores por documento
        fornecedoresBalanceadosPorDocumento = lerFornecedores(nomeArquivoFornecedores, Fornecedor::hashCode);
        
        gerarPedidos(25_000);
       
        int opcao = -1;
      
        do {
            opcao = menu();
            switch (opcao) {
            	case 1 -> mostrarProduto(localizarProdutoID(produtosBalanceadosPorId));
            	case 2 -> pedidosDoProduto(); 
            	case 3 -> System.out.println(relatorioDeFornecedor());
            	case 4 -> fornecedoresDoProduto();
            }
            pausa();
        } while(opcao != 0);       

        teclado.close();    
    }
}