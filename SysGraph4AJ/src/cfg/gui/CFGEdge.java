package cfg.gui;

import cfg.model.CFGEdgeType;
import cfg.model.CFGNode;
import edu.uci.ics.jung.graph.DelegateTree;

/**
 * Representa uma aresta no grafo de fluxo de controle. Essa classe é utilizada somente no que diz
 * respeito a renderização de um CFG em uma {@link DelegateTree}.<br>
 * Na construção da estrutura {@link CFGNode} não é necessário essa definição.
 *  
 * @author robson
 *
 */
public class CFGEdge {

	private CFGNode parentNode;
	
	private CFGNode childNode;
	
	private CFGEdgeType edgeType;

	public CFGEdge(CFGNode parentNode, CFGNode childNode, CFGEdgeType edgeType) {
		this.parentNode = parentNode;
		this.childNode = childNode;
		this.edgeType = edgeType;
	}

	public CFGNode getParentNode() {
		return parentNode;
	}

	public CFGNode getChildNode() {
		return childNode;
	}

	public CFGEdgeType getEdgeType() {
		return edgeType;
	}

}
