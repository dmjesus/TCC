package cfg.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

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
			
			CFGNode tryBlock = new CFGNode(); // vertice try
			CFGNode outTryBlock = new CFGNode(); // vertice de saida do bloco try
			List<CFGNode> catchs = new LinkedList<CFGNode>(); // vertices catch
			CFGNode finallyBlock = null; // vertice finally
			
			InstructionHandle lastIndexOfCatchs = null; // referencia para instrucao alvo de saida do vertice try
			outTryBlock.setOutTryNode(true);
			
			/* Processando enderenco das excecoes */
			for(CodeExceptionGen exception:exceptionBlocks){
				processedInstructionIds.add(exception.getHandlerPC().getPosition());
			}
			
			for (CodeExceptionGen exception:exceptionBlocks) {
				
				CodeExceptionGen codeException = exception;
				InstructionHandle startPcInstExc = codeException.getStartPC(); // Inicio do bloco catch inclusive
				InstructionHandle endPcInstExc = codeException.getEndPC().getNext(); // Final do bloco catch inclusive
				InstructionHandle handlerInstExc = codeException.getHandlerPC().getNext();
				
				if(codeException.getCatchType() != null){
					
					if(catchs.isEmpty()){
						
						/* Reset do vértice try e do vértice de saída
						 * A adicao de informações nesta parte é prioritaria as outras */
						if(!tryBlock.getInstructions().isEmpty()){
							tryBlock = new CFGNode();
						}
						
						for(InstructionHandle i = startPcInstExc;
								endPcInstExc.getNext() != null &&
								i.getPosition() <= endPcInstExc.getPosition();
								i = i.getNext()){
							if(!processedInstructionIds.contains(i.getPosition())){
								tryBlock.addInstruction(i);
							}
						}
						
						if(endPcInstExc != null &&
								endPcInstExc.getInstruction() instanceof GotoInstruction){
							
							BranchHandle returnInst = (BranchHandle) endPcInstExc;
							lastIndexOfCatchs = returnInst.getTarget();
							processedInstructionIds.add(endPcInstExc.getPosition());
							
						} else if(!(endPcInstExc.getInstruction() instanceof GotoInstruction)) {
							
							System.err.println("[CFGProcessor] A ultima instrucao do bloco try nao era GOTO");
							System.exit(53);
						}
					}
					
					/* Montagem dos vértices tipo catch */
					CFGNode catchBlock = new CFGNode();
					catchBlock.setCatchNode(true);
					
					/* Adiciona informações ao vértice tipo catch */
					if(catchBlock.getInstructions().isEmpty()){

						InstructionHandle i = handlerInstExc;
						for(;i != null && !processedInstructionIds.contains(i.getPosition()) &&
							 i.getPosition() < lastIndexOfCatchs.getPosition() && 
							 !(i.getInstruction() instanceof ReturnInstruction);
							 i = i.getNext()){
							catchBlock.addInstruction(i);
						}
						
						if(i.getPrev().getInstruction() instanceof GotoInstruction){
							BranchHandle goToInst = (BranchHandle) i.getPrev();
							if(outTryBlock.getInstructions().isEmpty()){
								
								outTryBlock.addInstruction(goToInst.getTarget().getPrev());
								processedInstructionIds.add(goToInst.getTarget().getPrev().getPosition());
								for(InstructionHandle j = goToInst.getTarget();
										j != null &&
										!processedInstructionIds.contains(j.getPosition());
										j = j.getNext()){
									outTryBlock.addInstruction(j);
								}
								
							}
							processedInstructionIds.add(i.getPrev().getPosition());
						} else {
							outTryBlock.addInstruction(i.getPrev());
							processedInstructionIds.add(i.getPrev().getPosition());
							for(InstructionHandle j = i;
									j != null &&
									!processedInstructionIds.contains(j.getPosition());
									j = j.getNext()){
								outTryBlock.addInstruction(j);
							}
						}
					}
					
					/* Adiciona vértice a lista de vértices tipo catch */
					catchs.add(catchBlock);
					
				} else {
					
					/* Montagem do vértice tipo finally */
					
					finallyBlock = new CFGNode();
					finallyBlock.setFinallyNode(true);
					int sizeFinallyBlock = 0;
					
					/* Adiciona informações no bloco try caso nenhum catch o tenha feito ainda */
					if(tryBlock.getInstructions().isEmpty()){
						
						for(InstructionHandle i = startPcInstExc;
								i.getNext() != null &&
								i.getPosition() <  endPcInstExc.getPosition();
								i = i.getNext()){
							tryBlock.addInstruction(i);
						}
						
						if(endPcInstExc.getInstruction() instanceof GotoInstruction){
							processedInstructionIds.add(endPcInstExc.getPosition());
						} 
					}
					
					/* Adiciona informações ao bloco finally */
					if(finallyBlock.getInstructions().isEmpty()){
						InstructionHandle i = handlerInstExc.getNext();
						for(;i != null &&
							 !(i.getInstruction().toString().contains("athrow")) &&
							 !processedInstructionIds.contains(i.getPosition());
							 i = i.getNext()){
							
							finallyBlock.addInstruction(i);
							sizeFinallyBlock++;
						}
						
						for(int j = 0; j < sizeFinallyBlock + 1 && i != null; j++){
							processedInstructionIds.add(i.getPosition());
							i = i.getNext();
						}
						
						if(i.getPrev() != null){
							outTryBlock.addInstruction(i.getPrev());
							processedInstructionIds.add(i.getPrev().getPosition());
							for(InstructionHandle j = i;
									j != null &&
									!processedInstructionIds.contains(j.getPosition());
									j = j.getNext()){
								outTryBlock.addInstruction(j);
							}
						}
					}
				}
			}
			
			/* Remove informações consideradas duplicadas nesta lógica */
			if(finallyBlock != null){
				removeFinallyBlockInformation(catchs, finallyBlock, processedInstructionIds);
			}
			System.out.println("nada");
			/* Processamento de instruções dentro dos vértices tipo catch */
			for(CFGNode node : catchs){
				if(node != null && !node.getInstructions().isEmpty() && !node.getInstructions().isEmpty()){
					tryBlock.addChildNode(node, CFGEdgeType.CATCH);
					processInnerInformation(node, null, processedInstructionIds);
				}
			}
			
			/* Processamento de instruções dentro dos vértices tipo try */
			if(tryBlock != null && !tryBlock.getInstructions().isEmpty()){
				tryBlock.setTryStatement(true);
				processInnerInformation(tryBlock, null, processedInstructionIds);
			} 
			
			/* Processamento de instruções dentro dos vértices tipo finally */
			if(finallyBlock != null && !finallyBlock.getInstructions().isEmpty()){
				tryBlock.addChildNode(finallyBlock, CFGEdgeType.FINALLY);
				processInnerInformation(finallyBlock, null, processedInstructionIds);
				
				finallyBlock.getRefToLeaves(tryBlock, CFGEdgeType.FINALLY);
				outTryBlock.getRefToLeaves(finallyBlock, CFGEdgeType.OUT_TRY);
				
			} else if (finallyBlock == null){
				/* Adiciona arestas de retorno para o vértice de saída,
				 * pois não existe vértice finally */
				outTryBlock.getRefToLeaves(tryBlock, CFGEdgeType.OUT_TRY);
			}
			
			processInnerInformation(outTryBlock, null, processedInstructionIds);
			
			/* Adiciona o vértice tipo try no nó raiz inicial*/
			blockNode.addChildNode(tryBlock, CFGEdgeType.TRY);
			
		}

	}

	private void removeFinallyBlockInformation(List<CFGNode> catchs,
			CFGNode finallyBlock, Set<Integer> processedInstructionIds) {
		
		if(finallyBlock != null){
			int finallyBlockInstSize = finallyBlock.getInstructions().size();
			
			for(int i = finallyBlockInstSize; i >= 0; i--){
				for(CFGNode catchNode : catchs){
					catchNode.getInstructions().remove(catchNode.getInstructions().size()-1);
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
						
						int targetPosition = getTargetPosition(i, targeter);
						
						if((targeter instanceof IfInstruction) && !processedInstructionIds.contains(targetPosition)){
							
							if(targetPosition > i.getPosition() &&
									!processedInstructionIds.contains(targetPosition)){
							
								/* Tratamento do do */
							
								InstructionHandle doInternInst = i;
								int endPosition = targetPosition;
								
								CFGNode doRootNode = new CFGNode();
								doRootNode.setReference(true);
								CFGNode doInternNode = new CFGNode();
								CFGNode doOutNode = new CFGNode();
								
								doRootNode.addInstruction(doInternInst.getPrev());
								processedInstructionIds.add(doInternInst.getPosition());
								
								InstructionHandle inst = doInternInst;
								for(;inst.getPosition() <= endPosition;
										inst = inst.getNext()){
									
									if(inst.getPosition() < endPosition - 5){
										doInternNode.addInstruction(inst);
									} else {
										doRootNode.addInstruction(inst);
										processedInstructionIds.add(inst.getPosition());
									}
									
								}
								
								for(;inst != null; inst = inst.getNext()){
									
									doOutNode.addInstruction(inst);
								
								}
								
								root.addChildNode(doRootNode, CFGEdgeType.LOOP);
								doRootNode.addChildNode(doInternNode, CFGEdgeType.REFERENCE);
								
								processInnerInformation(doInternNode, null, processedInstructionIds);
								
								doRootNode.getRefToLeaves(doInternNode, CFGEdgeType.T);
								doInternNode.setTrueNode(true);
								if(doInternNode.getChildElements().isEmpty()){
									doInternNode.setEndNode(true);
								} else {
									doRootNode.getParents().get(1).setEndNode(true);
								}
								doOutNode.getRefToLeaves(doInternNode, CFGEdgeType.F);
								doOutNode.setOutRefNode(true);
								
								processInnerInformation(doOutNode, null, processedInstructionIds);
								
								return;
							}
							
						} else if(targeter != null && targeter instanceof CodeExceptionGen){
							
							CodeExceptionGen codeExceptionGen = (CodeExceptionGen) targeter;
							
							if (!processedInstructionIds.contains(codeExceptionGen.getHandlerPC().getPosition())) {
								System.out.println("[CFGProcessor] Adicionando exceção: " + codeExceptionGen.getHandlerPC().getPosition());
								codeExceptionList.add(codeExceptionGen);
								
							} else {
								System.out.println("[CFGProcessor] A exceção: " + codeExceptionGen.getHandlerPC().getPosition() + " já foi processada");
							}
						}
					}
					if (!codeExceptionList.isEmpty()) {
						processTryCatchFinallyStatement(root, processedInstructionIds, codeExceptionList);
						return;
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
									!(j instanceof BranchHandle) ||
									!(j.getInstruction() instanceof IfInstruction);
									j = j.getNext()){
								whileRoot.addInstruction(j);
								processedInstructionIds.add(j.getPosition());
								lastInst = j;
							}
							lastInst = lastInst.getNext();
							whileRoot.addInstruction(lastInst);
							processedInstructionIds.add(lastInst.getPosition());
							lastInst = lastInst.getNext();
							
//							whileOut.addInstruction(lastInst);
//							processedInstructionIds.add(lastInst.getPosition());
							
							for(InstructionHandle j = i.getNext();
									j.getPosition() < goToIns.getTarget().getPosition() ||
									 !(j.getInstruction() instanceof IfInstruction) ;
									j = j.getNext()){
								whileIntern.addInstruction(j);
							}
							
							root.addChildNode(whileRoot, CFGEdgeType.LOOP);
							whileRoot.setReference(true);
							
							whileRoot.addChildNode(whileIntern, CFGEdgeType.T);
							whileIntern.setTrueNode(true);
							
							processInnerInformation(whileIntern, null, processedInstructionIds);
							whileRoot.getRefToLoopRoot(whileIntern, CFGEdgeType.GOTO);
							
							whileRoot.addChildNode(whileOut, CFGEdgeType.F);
							whileOut.setOutRefNode(true);
							
							for(InstructionHandle inst = goToIns.getTarget(); inst != null &&
									inst.getPosition() <= instructionList.get(instructionList.size()-1).getPosition();
									inst = inst.getNext()){
								whileOut.addInstruction(inst);
							}
							
							processInnerInformation(whileOut, null, processedInstructionIds);
							
							return;
							
						}
					}  else if (i.getInstruction() instanceof IfInstruction){
						
						/* Exitem outros tipos de objetos tipo IfInstruction que não são tratados aqui */
						
						IfInstruction ifInst = (IfInstruction) i.getInstruction();
						
						/* Tratamento do condicional if, if/else e if/elseIf/else */
						
						if(ifInst.getTarget().getPosition() > i.getPosition() && 
								!processedInstructionIds.contains(ifInst.getTarget().getPosition())){
							
							CFGNode ifNodeRoot = new CFGNode();
							CFGNode ifNodeTrue = new CFGNode();
							CFGNode ifNodeFalse = new CFGNode();
							
							ifNodeRoot.addInstruction(i);
							processedInstructionIds.add(i.getPosition());
							processedInstructionIds.add(ifInst.getTarget().getPosition());
							
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
							ifNodeTrue.setTrueNode(true);
							
							ifNodeRoot.addChildNode(ifNodeFalse, CFGEdgeType.F);
							ifNodeFalse.getRefToLeaves(ifNodeTrue, CFGEdgeType.GOTO);
							ifNodeFalse.setFalseNode(true);
							
							root.addChildNode(ifNodeRoot, CFGEdgeType.IF);
							
							for(InstructionHandle inst = ifInst.getTarget(); inst != null &&
									inst.getPosition() <= instructionList.get(instructionList.size()-1).getPosition();
									inst = inst.getNext()){
								ifNodeFalse.addInstruction(inst);
							}
							
							processInnerInformation(ifNodeFalse, null, processedInstructionIds);
							
							return;
							
						
							
						} else if(ifInst.getTarget().getPosition() < i.getPosition()){
							
							JOptionPane.showMessageDialog(
							        null, "[CFGProcessor] O código não deveria seguir esse caminho", "Falha - analise laço DO", JOptionPane.ERROR_MESSAGE);
							System.exit(12);
							
						}
							
					/* Tratamento do switch */
						
					} else if(i.getInstruction() instanceof Select){
						
						Select selectInst = (Select) i.getInstruction();
						
						InstructionHandle defaultInst = selectInst.getTarget();
						InstructionHandle[] selectInstCases = selectInst.getTargets();
						
						CFGNode switchNode = new CFGNode();
						CFGNode switchOutNode = new CFGNode();
						switchNode.setSwitchNode(true);
						switchOutNode.setOutSwitchNode(true);

						switchNode.addInstruction(i);
						processedInstructionIds.add(i.getPosition());
						root.addChildNode(switchNode, CFGEdgeType.SWITCH);
						
						CFGNode[] cases = new CFGNode[selectInstCases.length];
						
						for(int j = 0; j < cases.length; j++){
							cases[j] = new CFGNode();
							cases[j].setCaseNode(true);
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
							switchOutNode.addInstruction(inst);
						}
						switchNode.addChildNode(switchOutNode, CFGEdgeType.OUT_SW);
						processInnerInformation(switchOutNode, null, processedInstructionIds);
						switchOutNode.getRefToLeaves(switchNode, CFGEdgeType.OUT_SW);
						return;
						
						
					}else if (i.getInstruction() instanceof ReturnInstruction) {
						
						CFGNode returnNode = new CFGNode();
						returnNode.addInstruction(i);
						root.getInstructions().remove(i);
						root.addChildNode(returnNode, CFGEdgeType.RETURN);
						processedInstructionIds.add(i.getPosition());
						
						return;
						
					} else {
						System.out.println("[UNKNOW]Instrução: "
										+ "[" + i.getPosition() + "] "
										+ i.getInstruction() + " não é qualificada e foi registrada");
						processedInstructionIds.add(i.getPosition());
					}
				} else {
					System.out.println("Instrução: " + i.getPosition() + " já foi processada");
				}
			}
		}
	}

	public static Integer getTargetPosition(InstructionHandle instructions, InstructionTargeter targeter) {
		
		InstructionHandle inst = instructions;
		while(inst != null && !(inst.getInstruction().hashCode() == targeter.hashCode()) ){
			inst = inst.getNext();
		}
		
		if(inst != null){
			return inst.getPosition();
		}
		
		inst = instructions;
		while(inst != null && !(inst.getInstruction().hashCode() == targeter.hashCode()) ){
			inst = inst.getPrev();
		}
		
		if(inst != null){
			return inst.getPosition();
		}
		
		return -1;
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