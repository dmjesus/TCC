package cfg.processing;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
import org.objectweb.asm.tree.IntInsnNode;

import sun.org.mozilla.javascript.internal.RefCallable;

import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.xpath.internal.axes.ChildIterator;
import com.sun.xml.internal.messaging.saaj.util.FinalArrayList;

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

			List<CodeExceptionGen> exceptionBlocks = this.getExceptionBlocks(instructionHandle, processedInstructionIds);
			if(exceptionBlocks != null && !exceptionBlocks.isEmpty() && !root.isTryStatement()) {
				blockNode.setTryStatement(true);
				this.processTryCatchFinallyStatement(blockNode, processedInstructionIds, exceptionBlocks);
				instructionHandle = null;

			} else if(instructionHandle != null){

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
			} else {
				return null;
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
		
		if(exceptionBlocks.isEmpty()) {
			return;
		} else {
			
			CFGNode tryBlock = new CFGNode();
			
			CFGNode finallyBlock = null;
			List<CFGNode> catchs = new LinkedList<CFGNode>();
			
			for(CodeExceptionGen exception:exceptionBlocks){
				processedInstructionIds.add(exception.getHandlerPC().getPosition());
			}
			
			for (CodeExceptionGen exception:exceptionBlocks) {
				CodeExceptionGen codeException = exception;
				if(codeException.getCatchType() != null){
					CFGNode catchBlock = new CFGNode();
					
					if(tryBlock.getInstructions().isEmpty() || catchs.isEmpty()){
						InstructionHandle initTryBlockInst = codeException.getStartPC();
						InstructionHandle endTryBlockInst = codeException.getEndPC();
						
						for(InstructionHandle i = initTryBlockInst;
								i.getPosition() <=  endTryBlockInst.getPosition();
								i = i.getNext()){
							tryBlock.addInstruction(i);
						}
					}
					
					if(catchBlock.getInstructions().isEmpty()){
						InstructionHandle catchInstruction = codeException.getHandlerPC();
						for(InstructionHandle i = catchInstruction.getNext();
								i != null && !processedInstructionIds.contains(i.getPosition());
								i = i.getNext()){
							catchBlock.addInstruction(i);
						}
					}
					catchs.add(catchBlock);
				} else {
					
					finallyBlock = new CFGNode();
					
					if(tryBlock.getInstructions().isEmpty()){
						InstructionHandle initTryBlockInst = codeException.getStartPC();
						InstructionHandle endTryBlockInst = codeException.getEndPC();
						for(InstructionHandle i = initTryBlockInst;
								i.getPosition() <=  endTryBlockInst.getPosition();
								i = i.getNext()){
							tryBlock.addInstruction(i);
						}
					}
					
					if(finallyBlock.getInstructions().isEmpty()){
						InstructionHandle finallyInstruction = codeException.getHandlerPC();
						processedInstructionIds.add(finallyInstruction.getPosition());
						for(InstructionHandle i = finallyInstruction.getNext();
								i != null && !processedInstructionIds.contains(i.getPosition());
								i = i.getNext()){
							finallyBlock.addInstruction(i);
						}
					}
				}
			}
			
			// Add instructions inside try nodes
			for(CFGNode node : catchs){
				if(node != null && !node.getInstructions().isEmpty() && !node.getInstructions().isEmpty()){
					processInnerInformation(node, processedInstructionIds);
					tryBlock.addChildNode(node, CFGEdgeType.CATCH);
				}
			}
			if(finallyBlock != null && !finallyBlock.getInstructions().isEmpty()){
				processInnerInformation(finallyBlock, processedInstructionIds);
				tryBlock.addChildNode(finallyBlock, CFGEdgeType.FINALLY);
			}
			
			if(tryBlock != null && !tryBlock.getInstructions().isEmpty()){
				processInnerInformation(tryBlock, processedInstructionIds);
			}
			
			blockNode.addChildNode(tryBlock, CFGEdgeType.TRY);
			printBlock(blockNode);
		}

	}

	private void processInnerInformation(CFGNode root, Set<Integer> processedInstructionIds) {
	
		if(root == null){
			return;
		} else {
			List<InstructionHandle> instructionList = root.getInstructions();
			List<CodeExceptionGen> codeExceptionList = new ArrayList<CodeExceptionGen>();
			
			for(InstructionHandle i : instructionList){
				
				InstructionTargeter[] targeters = i.getTargeters();
				
				if(targeters != null){
					for(InstructionTargeter targeter : targeters){
						if(targeter != null && targeter instanceof CodeExceptionGen){
							CodeExceptionGen codeExceptionGen = (CodeExceptionGen) targeter;
							if(!processedInstructionIds.contains(codeExceptionGen.getHandlerPC().getPosition())){
								System.out.println("Adding exception: " + codeExceptionGen.getHandlerPC().getPosition());
								codeExceptionList.add(codeExceptionGen); // finally é um tipo de catch null
							} else {
								System.out.println("The exception: " + codeExceptionGen.getHandlerPC().getPosition() +
										" was processed already");
							}
						}
					}
					
					if(!codeExceptionList.isEmpty()){
						processTryCatchFinallyStatement(root, processedInstructionIds, codeExceptionList);
					}
				}
				
				if(!processedInstructionIds.contains(i.getPosition())){
					if(i.getInstruction() instanceof GotoInstruction){
						System.out.println("Instruction: " + i.getInstruction() + " is a GotoInstruction");
						root.addChildNode(new CFGNode(), CFGEdgeType.REFERENCE);
						processedInstructionIds.add(i.getPosition());
					} else if(i.getInstruction() instanceof IfInstruction) {
						System.out.println("Instruction: " + i.getInstruction() + " is a IfInstruction");
						root.addChildNode(new CFGNode(), CFGEdgeType.REFERENCE);
						processedInstructionIds.add(i.getPosition());
					} else if(i.getInstruction() instanceof Select){
						System.out.println("Instruction: " + i.getInstruction() + " is a Select");
						processedInstructionIds.add(i.getPosition());
					} else if(i.getInstruction() instanceof ReturnInstruction){
						System.out.println("Instruction: " + i.getInstruction() + " is a ReturnInstruction");
						root.addChildNode(new CFGNode(), CFGEdgeType.REFERENCE);
						processedInstructionIds.add(i.getPosition());
					} else {
						System.out.println("Instruction: " + i.getInstruction() + " is not qualified and has been registered");
						processedInstructionIds.add(i.getPosition());
					}
				} else {
					System.out.println("Instruction: " + i.getPosition() + " was process already");
				}
			}
		}
	}

	private void regCompInstructions(CFGNode node, Set<Integer> processedInstructionIds){
		if(node.getChildElements().isEmpty()){
			for(InstructionHandle inst : node.getInstructions()){
				if(!processedInstructionIds.contains(inst.getPosition()))
					processedInstructionIds.add(inst.getPosition());
			}
		} else {
			for(IElement child : node.getChildElements()){
				CFGNode cfgNode = (CFGNode) child;
				regCompInstructions(cfgNode, processedInstructionIds);
			}
		}
	}

	private void printBlock(CFGNode node){
		if(node == null)
			return;
		
		for(IElement child : node.getChildElements()){
			printBlock((CFGNode)child);
			System.out.println(child.getChildElements().size() + " pai: " + child.getOwner());
		}
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
	private List<CodeExceptionGen> getExceptionBlocks(InstructionHandle instructionHandle, Set<Integer> processedInstructionIds) {
		
		List<CodeExceptionGen> codeExceptionList = null;
		
		if(!processedInstructionIds.contains(instructionHandle.getPosition())){
			codeExceptionList = new ArrayList<CodeExceptionGen>();
	
			InstructionTargeter[] targeters = instructionHandle.getTargeters();
	
			if(targeters != null) {
				for(InstructionTargeter targeter : targeters) {
					if(targeter instanceof CodeExceptionGen) {
						CodeExceptionGen codeExceptionGen = (CodeExceptionGen) targeter;
	//					if(codeExceptionGen.getCatchType() != null) {
	//						codeExceptionList.add(codeExceptionGen);
	//					}
						codeExceptionList.add(codeExceptionGen); // finally é um tipo de catch null
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