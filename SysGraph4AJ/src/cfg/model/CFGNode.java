package cfg.model;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	private CFGNode parentNode;
	
	private SysMethod sysMethod;
	
	public CFGNode() {
		this.instructions = new ArrayList<InstructionHandle>();
		this.childNodes = new HashMap<CFGNode, CFGEdgeType>();
		this.tryStatement = false;
		this.isReference = false;
		this.parentNode = null;
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
	public void addChildNode(CFGNode childNode, CFGEdgeType edgeType) {
		if(childNode != null) {
			this.childNodes.put(childNode, edgeType);
			childNode.setOwner(this);
		}
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
		return parentNode;
	}

	public void setOwner(IElement parentNode) {
		this.parentNode = (CFGNode) parentNode;
	}
	
	@Override
	public String toString() {				
		return this.instructions.size() == 0 || this.tryStatement ? "" : 
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
		
	}

	@Override
	public Set<? extends IElement> getChildElements() {
		return this.getChildNodes().keySet();
	}
	
	public CFGEdgeType getChildTypeByNode(CFGNode childNode) {
		return this.childNodes.get(childNode);
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
}
