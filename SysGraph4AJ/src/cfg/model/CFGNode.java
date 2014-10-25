package cfg.model;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import model.IElement;
import model.SysMethod;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.ReturnInstruction;

import cfg.processing.CFGBuilder;
import cfg.processing.CFGProcessor;

/**
 * Entidade que representa um nó na estrutura de um grafo de fluxo de controle. 
 * <br>
 * A construção dessa estrutura é feita através das classes {@link CFGBuilder} e {@link CFGProcessor}.
 * 
 * @author robson
 *
 */
public class CFGNode implements IElement {
	
	private List<InstructionHandle> instructions;
	
	private Map<CFGNode, CFGEdgeType> childNodes;
	
	private Boolean tryStatement;
	
	private Boolean isReference;
	
	private boolean isFinallyNode;

	private boolean isCatchNode;
	
	private boolean isEndNode;
	
	private List<CFGNode> parentNode;
	
	private SysMethod sysMethod;
	
	public CFGNode() {
		this.instructions = new ArrayList<InstructionHandle>();
		this.childNodes = new HashMap<CFGNode, CFGEdgeType>();
		this.tryStatement = false;
		this.isReference = false;
		this.parentNode = new LinkedList<CFGNode>();
		this.isCatchNode = false;
		this.isFinallyNode = false;
	}
	
	/**
	 * Adiciona uma instrução do tipo {@link InstructionHandle} na lista de todas
	 * as instruções processadas nesse nó.
	 * 
	 * @param instruction
	 * 		instrução a ser adicionada na lista
	 */
	public void addInstruction(InstructionHandle instruction) {
		this.instructions.add(instruction);
	}

	/**
	 * 
	 * @return todas as instruções pertencentes a este nó
	 */
	public List<InstructionHandle> getInstructions() {
		return instructions;
	}

	/**
	 * adiciona um nó filho no grafo.
	 */
	public synchronized void addChildNode(CFGNode childNode, CFGEdgeType edgeType) {
		if(childNode != null && childNode.getParents().isEmpty()) {
			this.childNodes.put(childNode, edgeType);
			if(this.childNodes.containsKey(childNode)){
				System.out.println("[CFGNode] Child added!");
			} else {
				System.err.println("[CFGNode] Child not added!");
			}
		}
		
		if(childNode.getChildElements().isEmpty() || childNode.isFinallyNode()){
			childNode.setEndNode(true);
		}
		
		childNode.setOwner(this);
		this.setEndNode(false);
	}

	/**
	 * 
	 * @return o mapa de nós filhos
	 */
	public Map<CFGNode, CFGEdgeType> getChildNodes() {
		return childNodes;
	}
	
	public void setChildNodes(Map<CFGNode, CFGEdgeType> childNodes) {
		this.childNodes = childNodes;
		
		for(CFGNode childNode : this.childNodes.keySet()) {
			childNode.setOwner(this);
		}
	}

	/**
	 * 
	 * @return se o nó é pai de um block try/catch/finally
	 */
	public Boolean isTryStatement() {
		return tryStatement;
	}

	public void setTryStatement(Boolean tryStatement) {
		this.tryStatement = tryStatement;
	}

	/**
	 * 
	 * @return se o nó é uma referência a outro já processado
	 */
	public Boolean isReference() {
		return isReference;
	}

	public void setReference(Boolean isReference) {
		this.isReference = isReference;
	}

	public SysMethod getSysMethod() {
		return sysMethod;
	}

	public void setSysMethod(SysMethod sysMethod) {
		if(this.sysMethod == null) {
			this.sysMethod = sysMethod;
		}
	}

	public IElement getOwner() {
		if(!parentNode.isEmpty())
			return parentNode.get(0);
		else
			return null;
	}
	
	public boolean isFinallyNode() {
		return isFinallyNode;
	}

	public void setFinallyNode(boolean isFinallyNode) {
		this.isFinallyNode = isFinallyNode;
	}

	public boolean isCatchNode() {
		return isCatchNode;
	}

	public boolean isEndNode() {
		return isEndNode;
	}

	public void setEndNode(boolean isEndNode) {
		this.isEndNode = isEndNode;
	}

	public void setCatchNode(boolean isCatchNode) {
		this.isCatchNode = isCatchNode;
	}
	
	public IElement getOwner(int i) {
		if(parentNode.size() >= i){
			return parentNode.get(i);
		} else {
			return null;
		}
	}
	
	public List<CFGNode> getParents(){
		return parentNode;
	}

	public void setOwner(IElement parentNode) {
		this.parentNode.add((CFGNode) parentNode);
	}
	
	@Override
	public String toString() {				
		return this.instructions.size() == 0 || this.tryStatement ? "[out]" : 
			new StringBuffer("[").append(this.instructions.get(0).getPosition()).append("]").toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((instructions == null) ? 0 : instructions.hashCode());
		result = prime * result
				+ ((tryStatement == null) ? 0 : tryStatement.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CFGNode other = (CFGNode) obj;
		if (instructions == null) {
			if (other.instructions != null)
				return false;
		} else if (!instructions.equals(other.instructions))
			return false;
		if (sysMethod == null) {
			if (other.sysMethod != null)
				return false;
		} else if (!sysMethod.equals(other.sysMethod))
			return false;
		if (tryStatement == null) {
			if (other.tryStatement != null)
				return false;
		} else if (!tryStatement.equals(other.tryStatement))
			return false;
		return true;
	}

	@Override
	public void addChild(IElement e) {
		System.err.println("Filho não adicionado");
	}

	@Override
	public Set<? extends IElement> getChildElements() {
		return this.getChildNodes().keySet();
	}
	
	public CFGEdgeType getChildTypeByNode(CFGNode childNode) {
		CFGEdgeType edge = this.childNodes.get(childNode);
		
		if(edge != null){
			return edge;
		}
		else{
			System.err.println("[CFGNode.getChildTypeByNode] - " + childNode.toString() + 
					"\tAresta não encontrada com método tradicional");
			for (Map.Entry<CFGNode, CFGEdgeType> entry : this.childNodes.entrySet()){
			    if (entry.getKey().equals(childNode)) {
			    	edge = entry.getValue();         
			    }
			}
			return edge;
		}
	}

	public boolean hasEnd() {
		if(this.instructions != null && this.getChildElements().isEmpty()) {
			
			for(InstructionHandle instructionHandle : this.getInstructions()) {
				
				if(instructionHandle.getInstruction() instanceof ReturnInstruction) {
					return true;
				}
			}
		}
		return false;
	}

	public void referenceToLastChild(CFGNode leaf) {
		if(leaf.getChildElements().isEmpty() && !leaf.equals(this) && leaf.isEndNode()){
			if(leaf.isFinallyNode()){
				leaf.addChildNode(this, CFGEdgeType.FINALLY);
			} else {
				leaf.addChildNode(this, CFGEdgeType.REFERENCE);
			}
		} else if(leaf.equals(this)){
			return;
		} else {
			for(IElement child : leaf.getChildElements()){
				CFGNode cfgNode = (CFGNode) child;
				
				this.referenceToLastChild(cfgNode);
				
			}			
		}
	}

	public void referenceToLoopRoot(CFGNode leaf) {
		if(!containsCatchParent(leaf)){
			if(leaf.isEndNode()){
				leaf.addChildNode(this, CFGEdgeType.GOTO);
			} else{
				for(IElement child : leaf.getChildElements()){
					CFGNode cfgNode = (CFGNode) child;
					this.referenceToLastChild(cfgNode);
				}
			}
		} 		
	}

	private boolean containsCatchParent(CFGNode leaf) {
		if(!leaf.getParents().isEmpty()){
			for(CFGNode parent : leaf.getParents()){
				if(parent.isCatchNode()){
					return true;
				}
			}
			containsCatchParent(leaf.getParents().get(0));
		}
		return false;
	}
}
