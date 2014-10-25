package cfg.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.Select;
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
//		Map<Integer, Set<CFGNode>> instructionsHashTable = new HashMap<Integer, Set<CFGNode>>(); 
//		Map<Integer, Integer> instructionsDeepLevel = new HashMap<Integer, Integer>(); 
//		Set<Integer> referencedInstructionPositions = new HashSet<Integer>(); 
		Set<Integer> processedInstructionIds = new HashSet<Integer>();
		
		CFGNode root = new CFGNode();
		processInnerInformation(root, instruction, processedInstructionIds);
		
		if(root.getChildElements().isEmpty()){
			System.err.println("[ERROR - CFGProcessos] - Root without childs");
		}

//		this.updateHashMaps(root, instructionsHashTable, instructionsDeepLevel, referencedInstructionPositions, 0);
//		this.updateReferences(instructionsHashTable, instructionsDeepLevel, referencedInstructionPositions);

		return root;
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
	private synchronized void processTryCatchFinallyStatement(CFGNode blockNode, 
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
					catchBlock.setCatchNode(true);
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
					finallyBlock.setFinallyNode(true);
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
						for(InstructionHandle i = finallyInstruction.getNext();
								i != null &&
//										!(i.getInstruction() instanceof ReturnInstruction) &&
										!processedInstructionIds.contains(i.getPosition());
								i = i.getNext()){
							finallyBlock.addInstruction(i);
						}
					}
				}
			}
			
			if(finallyBlock != null){
				removeFinallyBlockInformation(catchs, finallyBlock, processedInstructionIds);
			}
			
			// Add instructions inside try nodes
			for(CFGNode node : catchs){
				if(node != null && !node.getInstructions().isEmpty() && !node.getInstructions().isEmpty()){
					tryBlock.addChildNode(node, CFGEdgeType.CATCH);
					processInnerInformation(node, null, processedInstructionIds);
				}
			}
			
			if(finallyBlock != null && !finallyBlock.getInstructions().isEmpty()){
				tryBlock.addChildNode(finallyBlock, CFGEdgeType.FINALLY);
				processInnerInformation(finallyBlock, null, processedInstructionIds);
			}
			
			if(tryBlock != null && !tryBlock.getInstructions().isEmpty()){
				processInnerInformation(tryBlock, null, processedInstructionIds);
			}
			
			if(finallyBlock != null && !finallyBlock.getInstructions().isEmpty()){
				finallyBlock.referenceToLastChild(tryBlock);
			}
			
			blockNode.addChildNode(tryBlock, CFGEdgeType.TRY);
		}

	}

	private void removeFinallyBlockInformation(List<CFGNode> catchs,
			CFGNode finallyBlock, Set<Integer> processedInstructionIds) {
		
		InstructionHandle lastInst1 = finallyBlock.getInstructions().get(0);
		List<InstructionHandle> lastInstList = null;
		
		if(!catchs.isEmpty()){
			lastInstList = new LinkedList<InstructionHandle>();
			for(int i = 0; i < catchs.size(); i++){
				lastInstList.add(catchs.get(i).getInstructions().get(catchs.get(i).getInstructions().size()-1));
			}
		}
		
		while(!lastInst1.getInstruction().toString().contains("athrow")){
			processedInstructionIds.add(lastInst1.getPosition());
			lastInst1 = lastInst1.getNext();
			
			if(lastInstList != null && !lastInstList.isEmpty()){
				for(int i = 0; i < lastInstList.size(); i++){
					processedInstructionIds.add(lastInstList.get(i).getPosition());
					lastInstList.set(i, lastInstList.get(i).getPrev());
				}
			}
		}
		return;
	}

	private synchronized void processInnerInformation(CFGNode root, InstructionHandle instructionHandle,
			Set<Integer> processedInstructionIds) {

		if (root == null) {
			System.err.println("Enter with a valid CFGNode object - root = null");
			return;
		} else {

			if (instructionHandle != null) {
				for (InstructionHandle i = instructionHandle; i != null; i = i.getNext()) {
					if (!processedInstructionIds.contains(i.getPosition())) {
						root.addInstruction(i);
					} else {
						System.err.println("[CFGProcessor] A instruction "
								+ i.getPosition() + " já foi processada\n");
					}
				}
			}

			List<InstructionHandle> instructionList = root.getInstructions();
			
			if(instructionList.isEmpty()){
				return;
			}
			
			for (InstructionHandle i : instructionList) {

				InstructionTargeter[] targeters = i.getTargeters();

				if (targeters != null) {
					
					List<CodeExceptionGen> codeExceptionList = new ArrayList<CodeExceptionGen>();
					
					for (InstructionTargeter targeter : targeters) {
						if (targeter != null
								&& targeter instanceof CodeExceptionGen) {
							CodeExceptionGen codeExceptionGen = (CodeExceptionGen) targeter;
							if (!processedInstructionIds.contains(codeExceptionGen.getHandlerPC().getPosition())) {
								System.out.println("[CFGProcessor] Adding exception: "
												+ codeExceptionGen.getHandlerPC().getPosition());
								codeExceptionList.add(codeExceptionGen); 
							} else {
								System.out.println("[CFGProcessor] The exception: "
												+ codeExceptionGen.getHandlerPC().getPosition()
												+ " was processed already");
							}
						}
					}
					if (!codeExceptionList.isEmpty()) {
						processTryCatchFinallyStatement(root, processedInstructionIds, codeExceptionList);
					}
				}

				if (!processedInstructionIds.contains(i.getPosition())) {
					
					if (i.getInstruction() instanceof GotoInstruction) {
						
						GotoInstruction goToIns = (GotoInstruction) i.getInstruction();
						
						if (!processedInstructionIds.contains(goToIns.getTarget().getPrev().getPosition())
							&& goToIns.getTarget().getPosition() > i.getPosition()
							&& !(goToIns.getTarget().getInstruction() instanceof ReturnInstruction)
							) {
							
							/* Tratamento do for, while*/
							
							CFGNode whileRoot = new CFGNode();
							whileRoot.addInstruction(i);
							processedInstructionIds.add(i.getPosition());
							CFGNode whileIntern = new CFGNode();
							CFGNode whileOut = new CFGNode();
							
							InstructionHandle lastInst = null;
							for(InstructionHandle j = goToIns.getTarget();
									!(j instanceof BranchHandle);
									j = j.getNext()){
								whileRoot.addInstruction(j);
								processedInstructionIds.add(j.getPosition());
								lastInst = j;
							}
							lastInst = lastInst.getNext();
							whileRoot.addInstruction(lastInst);
							processedInstructionIds.add(lastInst.getPosition());
							lastInst = lastInst.getNext();
							
							whileOut.addInstruction(lastInst);
							processedInstructionIds.add(lastInst.getPosition());
							
							for(InstructionHandle j = i.getNext();
									j.getPosition() < goToIns.getTarget().getPosition();
									j = j.getNext()){
								whileIntern.addInstruction(j);
							}
							
							root.addChildNode(whileRoot, CFGEdgeType.LOOP);
							whileRoot.addChildNode(whileIntern, CFGEdgeType.T);
							processInnerInformation(whileIntern, null, processedInstructionIds);
							whileRoot.referenceToLoopRoot(whileIntern);
							
							whileRoot.addChildNode(whileOut, CFGEdgeType.F);
							
							processInnerInformation(whileOut, lastInst.getNext(), processedInstructionIds);
							return;
							
						}
					}  else if (i.getInstruction() instanceof IfInstruction){
						
						/* Exitem outros tipos de objetos tipo IfInstruction que não são tratados aqui */
						
						IfInstruction ifInst = (IfInstruction) i.getInstruction();
						
						/* Tratamento do condicional if, if/else e if/elseIf/else */
						
						if(ifInst.getTarget().getPosition() > i.getPosition()){
							
							CFGNode ifNodeRoot = new CFGNode();
							CFGNode ifNodeTrue = new CFGNode();
							CFGNode ifNodeFalse = new CFGNode();
							
							ifNodeRoot.addInstruction(i);
							processedInstructionIds.add(i.getPosition());
							
							for(InstructionHandle j = i.getNext();
									j.getPosition() < ifInst.getTarget().getPosition();
									j = j.getNext()){
								ifNodeTrue.addInstruction(j);
							}
							
							if(ifNodeTrue.getInstructions().get(ifNodeTrue.getInstructions().size()-1).getInstruction() instanceof GotoInstruction){
								processedInstructionIds.add(ifNodeTrue.getInstructions().get(ifNodeTrue.getInstructions().size()-1).getPosition());
							}
							
							processInnerInformation(ifNodeTrue, null, processedInstructionIds);
							ifNodeRoot.addChildNode(ifNodeTrue, CFGEdgeType.T);
							ifNodeRoot.addChildNode(ifNodeFalse, CFGEdgeType.F);
							ifNodeFalse.referenceToLastChild(ifNodeTrue);
							root.addChildNode(ifNodeRoot, CFGEdgeType.IF);
							
							for(InstructionHandle inst = ifInst.getTarget(); inst != null &&
									inst.getPosition() <= instructionList.get(instructionList.size()-1).getPosition();
									inst = inst.getNext()){
								ifNodeFalse.addInstruction(inst);
							}
							
							processInnerInformation(ifNodeFalse, null, processedInstructionIds);
							
							return;
							
						/* Tratamento do do */
							
						} else if(ifInst.getTarget().getPosition() < i.getPosition()){
							
							CFGNode doRoot = new CFGNode();
							CFGNode doInternInst = new CFGNode();
							CFGNode doOut = new CFGNode();
							CFGNode doLeaf = new CFGNode();
							
							doRoot.addInstruction(i);
							doLeaf.addInstruction(i.getPrev());
							processedInstructionIds.add(i.getPrev().getPosition());
							processedInstructionIds.add(i.getPosition());
							
							InstructionHandle j = ifInst.getTarget();
							for(;j.getPosition() < i.getPosition();
									j = j.getNext()){
								doInternInst.addInstruction(j);
							}
							
							root.addChildNode(doRoot, CFGEdgeType.LOOP);
							processInnerInformation(doInternInst, null, processedInstructionIds);
							doRoot.addChildNode(doInternInst, CFGEdgeType.T);
							doLeaf.referenceToLastChild(doInternInst);
							doRoot.referenceToLoopRoot(doLeaf);
							doLeaf.addChildNode(doOut, CFGEdgeType.F);
							
							for(InstructionHandle inst = j; inst != null &&
									inst.getPosition() <= instructionList.get(instructionList.size()-1).getPosition();
									inst = inst.getNext()){
								doOut.addInstruction(inst);
							}
							
							processInnerInformation(doOut, null, processedInstructionIds);
							
							return;
							
						}
							
					/* Tratamento do switch */
						
					} else if(i.getInstruction() instanceof Select){
						
						Select selectInst = (Select) i.getInstruction();
						
						InstructionHandle defaultInst = selectInst.getTarget();
						InstructionHandle[] selectInstCases = selectInst.getTargets();
						
						CFGNode switchNode = new CFGNode();

						switchNode.addInstruction(i);
						processedInstructionIds.add(i.getPosition());
						root.addChildNode(switchNode, CFGEdgeType.SWITCH);
						
						CFGNode[] cases = new CFGNode[selectInstCases.length];
						
						for(int j = 0; j < cases.length; j++){
							cases[j] = new CFGNode();
						}
						
						for(int k = 1; k < selectInstCases.length; k++){
							for(InstructionHandle inst = selectInstCases[k-1];
									inst != null
									&& inst.getPosition() < selectInstCases[k].getPosition();
									inst = inst.getNext()){
								cases[k-1].addInstruction(inst);
							}
							if(selectInstCases[k].getPrev().getInstruction() instanceof GotoInstruction){
								processedInstructionIds.add(selectInstCases[k].getPrev().getPosition());
							}
							processInnerInformation(cases[k-1], null, processedInstructionIds);
							switchNode.addChildNode(cases[k-1], CFGEdgeType.CASE);
						}
						
						for(InstructionHandle inst = selectInstCases[selectInstCases.length-1]; 
								inst.getPosition() <= defaultInst.getPosition(); 
								inst = inst.getNext()){
							if(inst.getPosition() == defaultInst.getPrev().getPosition() && 
									(inst.getInstruction() instanceof GotoInstruction)){
								processedInstructionIds.add(inst.getPosition());
								break;
							}
							cases[cases.length-1].addInstruction(inst);
						}
						
						if(defaultInst.getPrev().getInstruction() instanceof GotoInstruction){
							processedInstructionIds.add(defaultInst.getPrev().getPosition());
						}
						
						switchNode.addChildNode(cases[cases.length-1], CFGEdgeType.CASE);
						
						processInnerInformation(cases[cases.length-1], null, processedInstructionIds);
						
						for(InstructionHandle inst = defaultInst; inst != null &&
								inst.getPosition() <= instructionList.get(instructionList.size()-1).getPosition();
								inst = inst.getNext()){
							switchNode.addInstruction(inst);
						}
						
						processInnerInformation(switchNode, null, processedInstructionIds);
						
						return;
						
						
					}else if (i.getInstruction() instanceof ReturnInstruction) {
						CFGNode returnNode = new CFGNode();
						returnNode.addInstruction(i);
						root.addChildNode(returnNode, CFGEdgeType.REFERENCE);
						processedInstructionIds.add(i.getPosition());
						
						return;
						
					} else {
						System.out.println("[UNKNOW]Instruction: "
										+ "[" + i.getPosition() + "] "
										+ i.getInstruction() + " não é qualificada e foi registrada");
						processedInstructionIds.add(i.getPosition());
					}
				} else {
					System.out.println("Instruction: " + i.getPosition() + " já foi processada");
				}
			}
		}
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
		
		/* A primeira iteração todos os argumentos não tem informações, exceto o root*/

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