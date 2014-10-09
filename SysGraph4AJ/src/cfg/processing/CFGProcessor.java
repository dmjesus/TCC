package cfg.processing;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.IElement;

import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.Select;
import org.apache.bcel.generic.TABLESWITCH;
import org.apache.bcel.verifier.structurals.ControlFlowGraph;

import cfg.model.CFGEdgeType;
import cfg.model.CFGNode;

/**
 * Classe responsável pelo processamento de uma instância de {@link CFGNode}
 * a partir de um método representado pela classe {@link MethodGen}
 * 
 * @author robson
 * 
 * @see CFGBuilder
 *
 */
public class CFGProcessor {

	/**
	 * 
	 * @param methodGen
	 * 		{@link ControlFlowGraph} representado de um método
	 * @return 
	 * 		{@link CFGNode} criado com toda a hierarquia de instruções
	 * 		que representam o grafo de fluxo de controle
	 */
	public CFGNode process(MethodGen methodGen) {
		InstructionHandle instruction = methodGen.getInstructionList().getStart();
		return this.processInstruction(instruction);

	}

	/**
	 * @see CFGProcessor#processInstruction(InstructionHandle, CFGNode, List)
	 * 
	 * @param instruction
	 * 		Uma instrução representada pela classe {@link InstructionHandle}
	 * @return 
	 * 		Nó raiz com todas as instruções armazenadas em seu grafo
	 */
	private CFGNode processInstruction(InstructionHandle instruction) {
		Map<Integer, Set<CFGNode>> instructionsHashTable = new HashMap<Integer, Set<CFGNode>>(); 
		Map<Integer, Integer> instructionsDeepLevel = new HashMap<Integer, Integer>(); 
		Set<Integer> referencedInstructionPositions = new HashSet<Integer>(); 
		Set<Integer> processedInstructionIds = new HashSet<Integer>();

		CFGNode root = this.processInstruction(instruction, null, processedInstructionIds);

		this.updateHashMaps(root, instructionsHashTable, instructionsDeepLevel, referencedInstructionPositions, 0);
		this.updateReferences(instructionsHashTable, instructionsDeepLevel, referencedInstructionPositions);

		return root;
	}

	/**
	 * Processa uma instrução e retorna o respectivo grafo representado pela classe
	 * {@link CFGNode}.
	 *  
	 * @param instructionHandle
	 * 		Uma instrução representada pela classe {@link InstructionHandle}
	 * @param root
	 * 		{@link CFGNode} já criado pelo builder, pode ser nulo 	
	 * @param processedInstructionIds
	 * 		Lista de instruções já processadas
	 * @return 
	 * 		Nó raiz com todas as instruções armazenadas em seu grafo
	 */ 
	private CFGNode processInstruction(InstructionHandle instructionHandle, 
			CFGNode root, 
			Set<Integer> processedInstructionIds) {

		boolean ifInstructionConditional, switchInstructionConditional, returnInstructionConditional, 
		notNullConditional, instructionWasNotProcessedConditional, gotoInstructionConditional;

		CFGNode blockNode = new CFGNode();

		if(root == null) {
			root = blockNode;
		} 

		//Para guardar somente a primeira instrução do bloco na lista de instruções processadas
		boolean isFirstIteration = true;

		do {

			List<CodeExceptionGen> exceptionBlocks = this.getExceptionBlocks(instructionHandle);
			if(!exceptionBlocks.isEmpty() && !root.isTryStatement()) {
				blockNode.setTryStatement(true);
				this.processTryCatchFinallyStatement(blockNode, processedInstructionIds, exceptionBlocks);
				instructionHandle = null;

			} else {

				boolean alreadyContainsKey = processedInstructionIds.contains(instructionHandle.getPosition());
				blockNode.addInstruction(instructionHandle);

				if(alreadyContainsKey) {
					blockNode.setReference(true);
					return blockNode;
				} 

				if(isFirstIteration) {
					processedInstructionIds.add(instructionHandle.getPosition());
					isFirstIteration = false;
				} 
				instructionHandle = instructionHandle instanceof BranchHandle ? ((BranchHandle) instructionHandle).getTarget() : instructionHandle.getNext();
			}

			notNullConditional = instructionHandle != null;
			instructionWasNotProcessedConditional = (notNullConditional && !processedInstructionIds.contains(instructionHandle.getPosition()));

			ifInstructionConditional = !(notNullConditional && instructionHandle.getInstruction() instanceof IfInstruction);
			gotoInstructionConditional = !(notNullConditional && instructionHandle.getInstruction() instanceof GotoInstruction);
			switchInstructionConditional = !(notNullConditional && instructionHandle.getInstruction() instanceof Select);
			returnInstructionConditional = !(notNullConditional && instructionHandle.getInstruction() instanceof ReturnInstruction);

		} while(notNullConditional && 
				returnInstructionConditional && 
				ifInstructionConditional && 
				gotoInstructionConditional && 
				instructionWasNotProcessedConditional && 
				switchInstructionConditional);

		if(notNullConditional) {

			if(!instructionWasNotProcessedConditional && !blockNode.isTryStatement() && !blockNode.isReference()) { //Se é uma instrução que já existe
				CFGNode childNode = new CFGNode();
				blockNode.addChildNode(childNode, CFGEdgeType.REFERENCE);
				childNode.addInstruction(instructionHandle);
				childNode.setReference(true);

			} else if(!gotoInstructionConditional) { //Se é uma instrução de goto 
				blockNode.addChildNode(this.processInstruction(instructionHandle.getNext(), blockNode, processedInstructionIds), 
						CFGEdgeType.GOTO);

			} else if(!ifInstructionConditional) { //Se é uma instrução de condicional 'if/else' ou 'if'
				this.processIfInstruction(blockNode, instructionHandle, processedInstructionIds);				

			} else if(!returnInstructionConditional) { //Se é um 'return algumaCoisa;'
				blockNode.addInstruction(instructionHandle);

			} else if(!switchInstructionConditional) { //Se é um 'switch'
				blockNode.addInstruction(instructionHandle);
				this.processSwitchInstruction(blockNode, instructionHandle, processedInstructionIds);
			}
		}
		return blockNode;
	}

	/**
	 * 
	 * @param blockNode
	 * 		bloco pai que irá adicionar os blocos try/catch/finally 
	 * @param processedInstructionIds
	 * 		lista de instruções já processadas
	 * @param exceptionBlocks
	 * 		Bloco que contem as instruções alvos do escopo de exceptions
	 */
	private void processTryCatchFinallyStatement(CFGNode blockNode, 
			Set<Integer> processedInstructionIds,
			List<CodeExceptionGen> exceptionBlocks) {
		
		CFGNode finallyBlock = null;
		CFGNode catchBlock = null;
		CFGNode tryBlock = null;

		Iterator<CodeExceptionGen> codeExceptionIterator = exceptionBlocks.iterator();
		while(codeExceptionIterator.hasNext()) {

			CodeExceptionGen codeException = codeExceptionIterator.next();
			if(processedInstructionIds.contains(codeException.getHandlerPC().getPosition())) {
				codeExceptionIterator.remove();
				
				CFGNode catchReference = new CFGNode();
				catchReference.addInstruction(codeException.getHandlerPC());
				catchReference.setReference(true);
				
				blockNode.addChildNode(catchReference, CFGEdgeType.CATCH);
			}
		}

		if(exceptionBlocks.isEmpty()) {
			return;
		}

		InstructionHandle catchOrFinallyWithoutCast = exceptionBlocks.get(0).getEndPC().getNext();
		InstructionHandle tryInstruction = exceptionBlocks.get(0).getStartPC();
		tryBlock = this.processInstruction(tryInstruction, blockNode, processedInstructionIds);
		
		if(catchOrFinallyWithoutCast instanceof BranchHandle) {
			BranchHandle catchOrFinally = (BranchHandle) catchOrFinallyWithoutCast;

			InstructionHandle finallyInstruction = catchOrFinally.getTarget();
			finallyBlock = this.processInstruction(finallyInstruction, blockNode, processedInstructionIds);	
			tryBlock.addChildNode(finallyBlock, CFGEdgeType.FINALLY);		
		}

		for(CodeExceptionGen codeException : exceptionBlocks) {
			InstructionHandle catchInstruction = codeException.getHandlerPC();

			catchBlock = this.processInstruction(catchInstruction, blockNode, processedInstructionIds);
			tryBlock.addChildNode(catchBlock, CFGEdgeType.CATCH);
		}

//		InstructionHandle tryInstruction = exceptionBlocks.get(0).getStartPC();
//		tryBlock = this.processInstruction(tryInstruction, blockNode, processedInstructionIds);		
		
		this.addFinallyChildToTryNodes(tryBlock, finallyBlock);

		//Para diferenciar no equals() e hashCode()
		blockNode.addInstruction(tryInstruction);

		blockNode.addChildNode(tryBlock, CFGEdgeType.TRY);

	}

	private void addFinallyChildToTryNodes(CFGNode tryBlock, CFGNode finallyBlock) {
		if(finallyBlock == null || tryBlock.isReference()) {
			return;
		}
		
		for(IElement childNode : tryBlock.getChildElements()) {
			this.addFinallyChildToTryNodes((CFGNode) childNode, finallyBlock);
		}
		
		//TODO adicionar nos catch o finally
		tryBlock.addChildNode(finallyBlock, CFGEdgeType.FINALLY);
	}

	/**
	 * @param blockNode
	 * 		{@link CFGNode} processado no momento
	 * @param instructionHandle
	 * 		Uma instrução representada pela classe {@link InstructionHandle}
	 * @param processedInstructionIds
	 * 		Lista de instruções já processadas
	 * 
	 */
	private void processIfInstruction(CFGNode blockNode,
			InstructionHandle instructionHandle, 
			Set<Integer> processedInstructionIds) {

		blockNode.addInstruction(instructionHandle);
		BranchHandle branchHandle = (BranchHandle) instructionHandle;
		InstructionHandle ifTrueNextInstruction = branchHandle.getTarget();	

		blockNode.addChildNode(this.processInstruction(ifTrueNextInstruction, blockNode, processedInstructionIds), 
				CFGEdgeType.T);

		blockNode.addChildNode(this.processInstruction(instructionHandle.getNext(), blockNode, processedInstructionIds), 
				CFGEdgeType.F);
	}

	/**
	 * 
	 * @param blockNode
	 * 		{@link CFGNode} processado no momento
	 * @param instructionHandle
	 * 		Uma instrução representada pela classe {@link InstructionHandle}
	 * @param processedInstructionIds
	 * 		Lista de instruções já processadas
	 * 
	 */
	private void processSwitchInstruction(CFGNode blockNode,
			InstructionHandle instructionHandle,
			Set<Integer> processedInstructionIds) {

		TABLESWITCH switchInstruction = (TABLESWITCH) instructionHandle.getInstruction();
		InstructionHandle[] caseInstructions = switchInstruction.getTargets();
		for(InstructionHandle caseInstruction : caseInstructions) {
			blockNode.addChildNode(this.processInstruction(caseInstruction, blockNode, processedInstructionIds), 
					CFGEdgeType.CASE);
		}
		InstructionHandle defaultCaseInstruction = switchInstruction.getTarget();
		blockNode.addChildNode(this.processInstruction(defaultCaseInstruction, blockNode, processedInstructionIds), 
				CFGEdgeType.DEFAULT);
	}

	/**
	 * Obtém uma exceção a ser tratada em um block try/catch
	 * 
	 * @param instructionHandle 
	 * 		Uma instrução representada pela classe {@link InstructionHandle}
	 * @return 
	 * 		Instância de {@link CodeExceptionGen} com a respectiva exceçao, mas pode ser um valor nulo
	 */
	private List<CodeExceptionGen> getExceptionBlocks(InstructionHandle instructionHandle) {
		List<CodeExceptionGen> codeExceptionList = new ArrayList<CodeExceptionGen>();

		InstructionTargeter[] targeters = instructionHandle.getTargeters();

		if(targeters != null) {
			for(InstructionTargeter targeter : targeters) {
				if(targeter instanceof CodeExceptionGen) {
					CodeExceptionGen codeExceptionGen = (CodeExceptionGen) targeter;
					if(codeExceptionGen.getCatchType() != null) {
						codeExceptionList.add(codeExceptionGen);
					}  
				}
			}
		}
		return codeExceptionList;
	}


	/**
	 * Atualiza as informações das hashtables para a atualização das referências posteriormente.
	 * 
	 * @param root
	 * 		Nó raiz a ser referenciado.
	 * @param instructionsHashTable
	 * 		Mapa onde a chave é o nível da árvore e o valor é a lista de nós de do nível da árvore de {@link CFGNode}
	 * @param instructionsDeepLevel
	 * 		Mapa onde a chave é a posição da {@link InstructionHandle} e o valor é o nível mais próximo da raiz em que se encontra essa instrução.
	 * @param referencedInstructionPositions
	 * 		Lista de todas as instruções que contém referências no grafo.
	 * @param currentLevel
	 * 		Nível atual da árvore
	 */
	@SuppressWarnings("unchecked")
	private void updateHashMaps(CFGNode root,
			Map<Integer, Set<CFGNode>> instructionsHashTable,
			Map<Integer, Integer> instructionsDeepLevel,
			Set<Integer> referencedInstructionPositions, 
			int currentLevel) {

		//updating instructionsHashTable
		Set<CFGNode> nodeList = instructionsHashTable.get(currentLevel);

		if(nodeList == null) {
			nodeList = new HashSet<CFGNode>();
			instructionsHashTable.put(currentLevel, nodeList);
		}

		nodeList.add(root);

		List<InstructionHandle> instructions = root.getInstructions();

		if(!instructions.isEmpty() && !root.isTryStatement()) {

			//updating instructionsDeepLevel
			InstructionHandle instructionHandle = instructions.get(0);
			if(instructionHandle != null) {
				Integer deepLevel = instructionsDeepLevel.get(instructionHandle.getPosition());
				if(deepLevel == null || deepLevel > currentLevel) {
					instructionsDeepLevel.put(instructionHandle.getPosition(), currentLevel);
				}
			}

			//updating referencedInstructionPositions
			if(root.isReference()) {
				referencedInstructionPositions.add(instructionHandle.getPosition());
			}
		}

		//updating childNodes
		Set<CFGNode> childNodes = (Set<CFGNode>) root.getChildElements();
		for(CFGNode childNode : childNodes) {
			this.updateHashMaps(childNode, instructionsHashTable, instructionsDeepLevel, referencedInstructionPositions, currentLevel + 1);
		}
	}

	/**
	 * Atualiza as referências de modo a substituir todos os nós que são referências mas que estão 
	 * mais próximos do nó raiz com um nó que não é referência e que está mais afastado do nó raiz. 
	 * 
	 * @param instructionsHashTable
	 * 		Mapa onde a chave é o nível da árvore e o valor é a lista de nós de do nível da árvore de {@link CFGNode}
	 * @param instructionsDeepLevel
	 * 		Mapa onde a chave é a posição da {@link InstructionHandle} e o valor é o nível mais próximo da raiz em que se encontra essa instrução.
	 * @param referencedInstructionPositions
	 * 		Lista de todas as instruções que contém referências no grafo.
	 *  
	 */
	private void updateReferences(Map<Integer, Set<CFGNode>> instructionsHashTable,
			Map<Integer, Integer> instructionsDeepLevel, 
			Set<Integer> referencedInstructionPositions) {

		int treeDeep = 0;

		for(Integer level : instructionsHashTable.keySet()) {
			if(treeDeep < level) {
				treeDeep = level;
			}
		}

		for(Integer referencedInstructionPosition : referencedInstructionPositions) {
			Integer nearestDeepLevel = instructionsDeepLevel.get(referencedInstructionPosition);
			Set<CFGNode> nodes = instructionsHashTable.get(nearestDeepLevel);
			boolean alreadyReferenced = false;
			CFGNode referencedNode = null;

			for(CFGNode node : nodes) {
				List<InstructionHandle> instructions = node.getInstructions();
				if(!instructions.isEmpty() && instructions.get(0).getPosition() == referencedInstructionPosition) {
					referencedNode = node;

					if(!referencedNode.isReference()) {
						alreadyReferenced = true;
						break;	
					}
				}
			}

			if(!alreadyReferenced) {
				int i = nearestDeepLevel + 1;
				boolean foundNotReferencedNode = false; 

				while(!foundNotReferencedNode && i <= treeDeep) {

					Iterator<CFGNode> nodesFromSpecifiedDeepLevel = instructionsHashTable.get(i).iterator();

					while(nodesFromSpecifiedDeepLevel.hasNext()) {

						CFGNode nodeFromSpecifiedDeepLevel = nodesFromSpecifiedDeepLevel.next();						
						List<InstructionHandle> instructionsFromNode = nodeFromSpecifiedDeepLevel.getInstructions();

						if(!nodeFromSpecifiedDeepLevel.isReference() && 
								!instructionsFromNode.isEmpty() && 
								instructionsFromNode.get(0).getPosition() == referencedInstructionPosition &&
								!nodeFromSpecifiedDeepLevel.isTryStatement()) {
							
							Map<CFGNode, CFGEdgeType> childNodes = nodeFromSpecifiedDeepLevel.getChildNodes();
							nodeFromSpecifiedDeepLevel.setChildNodes(new HashMap<CFGNode, CFGEdgeType>());
							referencedNode.setChildNodes(childNodes);

							referencedNode.setReference(false);
							nodeFromSpecifiedDeepLevel.setReference(true);

							foundNotReferencedNode = true;

							break;
						}
					}

					i++;
				}
			}
		}
	}
}