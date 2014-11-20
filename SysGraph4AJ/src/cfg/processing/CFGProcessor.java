package cfg.processing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
		Set<Integer> processedInstructionIds = new HashSet<Integer>();
		
		CFGNode root = new CFGNode();
		processInnerInformation(root, instruction, processedInstructionIds);
		
		if(root.getChildElements().isEmpty()){
			System.err.println("[ERRO - CFGProcessor] - Raiz sem filhos");
		} else {
			CFGNode returnNode = new CFGNode();
			mergeNodesToReturn(root, returnNode);
		}

		return root;
	}

	private void mergeNodesToReturn(CFGNode root, CFGNode returnNode) {
		
		if(root == null){
			return;
		}
		
		if(returnNode.getInstructions().isEmpty()){
			InstructionHandle retInst = root.getInstructions().get(0);
			while(retInst.getNext() != null){
				retInst = retInst.getNext();
			}
			returnNode.addInstruction(retInst);
		}
		
		if (root.isEndNode()){
			
			root.addChildNode(returnNode, CFGEdgeType.RETURN);
			
		} 
			
		Iterator<? extends IElement> it = root.getChildElements().iterator();
		
		while(it.hasNext()){
			CFGNode node = (CFGNode) it.next();
			mergeNodesToReturn(node, returnNode);
		}
		
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
								i.getPosition() <= lastInstPosOf(blockNode) &&
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
							 i.getPosition() <= lastInstPosOf(blockNode) &&
							 i.getPosition() < lastIndexOfCatchs.getPosition();
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
										j.getPosition() <= lastInstPosOf(blockNode) &&
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
									j.getPosition() <= lastInstPosOf(blockNode) &&
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
								i.getPosition() <= lastInstPosOf(blockNode) &&
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
						
						for(int j = 0; j < sizeFinallyBlock + 1 && i != null &&
								i.getPosition() <= lastInstPosOf(blockNode); j++){
							processedInstructionIds.add(i.getPosition());
							i = i.getNext();
						}
						
						if(i.getPrev() != null){
							outTryBlock.addInstruction(i.getPrev());
							processedInstructionIds.add(i.getPrev().getPosition());
							for(InstructionHandle j = i;
									j.getPosition() <= lastInstPosOf(blockNode) &&
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
				
				if(outTryBlock.getInstructions().size() > 1){
					outTryBlock.getRefToLeaves(finallyBlock, CFGEdgeType.OUT_TRY);
				} 
				
			} else if (finallyBlock == null){
				/* Adiciona arestas de retorno para o vértice de saída,
				 * pois não existe vértice finally */
				if(outTryBlock.getInstructions().size() > 1){
					outTryBlock.getRefToLeaves(tryBlock, CFGEdgeType.OUT_TRY);
				} 
			}
			if(outTryBlock.getInstructions().size() > 1){
				processInnerInformation(outTryBlock, null, processedInstructionIds);
			} else {
				processedInstructionIds.add(outTryBlock.getInstructions().get(0).getPosition());
			}
			
			/* Adiciona o vértice tipo try no nó raiz inicial*/
			blockNode.addChildNode(tryBlock, CFGEdgeType.TRY);
			
		}

	}

	

	private synchronized void processInnerInformation(CFGNode root, InstructionHandle instructionHandle,
			Set<Integer> processedInstructionIds) {

		if (root == null) {
			System.err.println("Enter with a valid CFGNode object - root = null");
			return;
		} else {

			if (instructionHandle != null) {
				for (InstructionHandle i = instructionHandle; i != null && 
					!(i.getInstruction() instanceof ReturnInstruction);
					i = i.getNext()) {
					
					if (!processedInstructionIds.contains(i.getPosition())) {
						root.addInstruction(i);
					} else {
						System.err.println("[CFGProcessor] A instruction "
								+ i.getPosition()
								+ " já foi processada\n");
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
						
						if((targeter instanceof IfInstruction) &&
								!targeter.toString().contains("ifge") &&
								targetPosition <= lastInstPosOf(root) &&
								!processedInstructionIds.contains(targetPosition)){
							
							if(targetPosition > i.getPosition()
								&& !processedInstructionIds.contains(targetPosition)){
							
								/* Tratamento do do */
								System.out.println("\n[CFGProcessor] Instrução "+ i +" capturada como laço do");
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
								
								for(;inst != null &&
										inst.getPosition() <= lastInstPosOf(root);
										inst = inst.getNext()){
										
										doOutNode.addInstruction(inst);
									
								}
								
								if(doRootNode.getInstructions().size() > 1 && doOutNode.getChildElements().isEmpty()){
									doOutNode.addInstruction(doRootNode.getInstructions().get(doRootNode.getInstructions().size()-1));
								} 
								
								root.addChildNode(doRootNode, CFGEdgeType.LOOP);
								doRootNode.addChildNode(doInternNode, CFGEdgeType.REFERENCE);
								
								processInnerInformation(doInternNode, null, processedInstructionIds);
								
								doRootNode.getRefToLeaves(doInternNode, CFGEdgeType.T);
								doInternNode.setLoopTrueNode(true);
								if(doInternNode.getChildElements().isEmpty()){
									doInternNode.setEndNode(true);
								} else {
									doRootNode.getParents().get(1).setEndNode(true);
								}
								
								if(doOutNode.getInstructions().size() >= 1){
									doOutNode.getRefToLeaves(doInternNode, CFGEdgeType.OUT_L);
									doOutNode.setLoopFalseNode(true);
									processInnerInformation(doOutNode, null, processedInstructionIds);
								} 
								
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
						System.out.println("\n[CFGProcessor] Instrução "+ i +" capturada como laço while/for");
						GotoInstruction goToIns = (GotoInstruction) i.getInstruction();
						
						if (!processedInstructionIds.contains(goToIns.getTarget().getPrev().getPosition())
							&& goToIns.getTarget().getPosition() > i.getPosition()){
							
							/* Tratamento do for, while*/
							
							if(root.getInstructions().get(0).equals(i)){
								i = i.getNext();
							}
							
							CFGNode whileRoot = new CFGNode();
							whileRoot.addInstruction(i);
							processedInstructionIds.add(i.getPosition());
							
							CFGNode whileIntern = new CFGNode();
							CFGNode whileOut = new CFGNode();
							
							/* Foram encontrados dois tipos de estruturação desses laços
							 * 1 - goto salta sobre a instrução de retono
							 * 2 - goto salta para um pouco antes da instrução de retorno */
							
							InstructionHandle lastInst = goToIns.getTarget();
							// Este if é para a primeira situação
							if(lastInst.getPrev().getInstruction() instanceof IfInstruction){
								whileOut.addInstruction(lastInst.getPrev());
								processedInstructionIds.add(goToIns.getTarget().getPrev().getPosition());
							} else {
								// Este while é para segunda situação
								while(!(lastInst instanceof BranchHandle) || 
										(lastInst instanceof BranchHandle &&
												((BranchHandle)lastInst).getTarget().getPosition() >
										lastInst.getPosition())){

									whileOut.addInstruction(lastInst);
									processedInstructionIds.add(lastInst.getPosition());
									lastInst = lastInst.getNext();
								}
							}
							
							whileRoot.addInstruction(lastInst);
							processedInstructionIds.add(lastInst.getPosition());
							
							for(InstructionHandle j = i.getNext();
									j.getPosition() < goToIns.getTarget().getPosition();
									j = j.getNext()){
								whileIntern.addInstruction(j);
							}
							
							root.addChildNode(whileRoot, CFGEdgeType.LOOP);
							whileRoot.setReference(true);
							
							whileRoot.addChildNode(whileIntern, CFGEdgeType.T);
							whileIntern.setLoopTrueNode(true);
							
							processInnerInformation(whileIntern, null, processedInstructionIds);
							whileRoot.getRefToLoopRoot(whileIntern, CFGEdgeType.GOTO);
							
							for(InstructionHandle inst = lastInst.getNext();
									inst != null &&
									inst.getPosition() <= lastInstPosOf(root);
									inst = inst.getNext()){
								whileOut.addInstruction(inst);
							}
							
							if(whileOut.getInstructions().size() > 1){
								whileRoot.addChildNode(whileOut, CFGEdgeType.OUT_L);
								whileOut.setLoopFalseNode(true);
								processInnerInformation(whileOut, null, processedInstructionIds);
							} else {
								whileRoot.setEndNode(true);
							}
							
							return;
							
						}
					}  else if (i.getInstruction() instanceof IfInstruction){
						System.out.println("\n[CFGProcessor] Instrução "+ i +" capturada como if");
						/* Exitem outros tipos de objetos tipo IfInstruction que não são tratados aqui */
						
						IfInstruction ifInst = (IfInstruction) i.getInstruction();
						
						/* Tratamento do condicional if, if/else e if/elseIf/else */
						
						if(root.getInstructions().get(0).equals(i)){
							i = i.getNext();
						}
						
						if(ifInst.getTarget().getPosition() > i.getPosition() && 
								!processedInstructionIds.contains(ifInst.getTarget().getPosition())){
							
							CFGNode ifNodeRoot = new CFGNode();
							CFGNode ifNodeTrue = new CFGNode();
							CFGNode ifNodeFalse = null;
							CFGNode ifOutNode = new CFGNode();
							
							ifNodeRoot.addInstruction(i);
							processedInstructionIds.add(i.getPosition());
							
							for(InstructionHandle j = i.getNext();
									j.getPosition() < ifInst.getTarget().getPosition();
									j = j.getNext()){
								ifNodeTrue.addInstruction(j);
							}
							
							/* Em condições de if com else a última instrução é um goto.
							 * Para que esse goto não seja confundido com um laço a instrução é marcada
							 * como processada.*/
							InstructionHandle lastTruNodeInst = ifNodeTrue.getInstructions().get(ifNodeTrue.getInstructions().size()-1);
							if(lastTruNodeInst.getInstruction() instanceof GotoInstruction){
								GotoInstruction gotoInst = (GotoInstruction) lastTruNodeInst.getInstruction();
								processedInstructionIds.add(lastTruNodeInst.getPosition());
								ifNodeFalse = new CFGNode();
								
								for(InstructionHandle j = ifInst.getTarget();
										j != null &&
										j.getPosition() < gotoInst.getTarget().getPosition();
										j = j.getNext()){
									ifNodeFalse.addInstruction(j);
								}
								
								for(InstructionHandle j = gotoInst.getTarget();
										j != null && 
										j.getPosition() <= lastInstPosOf(root);
										j = j.getNext()){
									ifOutNode.addInstruction(j);
								}
								
							} else {
								InstructionHandle lastInstIf = ifNodeTrue.getInstructions().get(ifNodeTrue.getInstructions().size()-1);
								BranchHandle handle = null;
								for(int j = 0; j < ifNodeTrue.getInstructions().size(); j++){
									InstructionHandle instInsideIf = ifNodeTrue.getInstructions().get(j);
									 
									if(instInsideIf instanceof BranchHandle){
										handle = (BranchHandle) instInsideIf;
										if(lastInstIf.getPosition() < handle.getTarget().getPosition()){
											ifNodeFalse = new CFGNode();
											break;
										}
									}
								}
								
								if(ifNodeFalse != null){
									for(InstructionHandle j = (InstructionHandle) handle;
											j.getPosition() < handle.getTarget().getPosition() &&
											j.getPosition() <= lastInstPosOf(root);
											j = j.getNext()){
										ifNodeFalse.addInstruction(j);
										ifNodeTrue.getInstructions().remove(j);
									}
									
									for(InstructionHandle j = handle.getTarget();
											j != null &&
											j.getPosition() <= lastInstPosOf(root);
											j = j.getNext()){
										ifOutNode.addInstruction(j);
									}
								} else {
									for(InstructionHandle j = lastInstIf.getNext();
											j != null &&
											j.getPosition() <= lastInstPosOf(root);
											j = j.getNext()){
										ifOutNode.addInstruction(j);
									}
								}
								
							}
							
							ifNodeRoot.addChildNode(ifNodeTrue, CFGEdgeType.T);
							processInnerInformation(ifNodeTrue, null, processedInstructionIds);
							ifNodeRoot.setIfNode(true);
							ifNodeTrue.setIfTrueNode(true);

							if(ifNodeFalse != null){
								ifNodeRoot.addChildNode(ifNodeFalse, CFGEdgeType.F);
								ifNodeFalse.setIfFalseNode(true);
								processInnerInformation(ifNodeFalse, null, processedInstructionIds);
							} 
							
							if(ifOutNode.getInstructions().isEmpty() && ifNodeFalse != null && ifNodeFalse.getInstructions().size() > 2){
								ifOutNode.addInstruction(ifNodeTrue.getInstructions().get(2));
							} else if(ifOutNode.getInstructions().isEmpty() && ifNodeTrue.getInstructions().size() > 2){
								ifOutNode.addInstruction(ifNodeTrue.getInstructions().get(2));
							}
							
							if(ifOutNode.getInstructions().size() >= 1){
								if(ifNodeFalse == null){
									ifNodeRoot.addChildNode(ifOutNode, CFGEdgeType.OUT_IF);
								}
								
								processInnerInformation(ifOutNode, null, processedInstructionIds);
								ifOutNode.getRefToLeaves(ifNodeRoot, CFGEdgeType.OUT_IF);
								ifOutNode.setOutIfNode(true);
								
							} else if(ifNodeFalse == null){
								ifNodeRoot.setEndNode(true);
							}
							
							root.addChildNode(ifNodeRoot, CFGEdgeType.IF);
							
							return;
							
						} else if(ifInst.getTarget().getPosition() < i.getPosition()){
							
							JOptionPane.showMessageDialog(
							        null, "[CFGProcessor] O código não deveria seguir esse caminho", "Falha - analise laço DO", JOptionPane.ERROR_MESSAGE);
							System.exit(12);
							
						}
							
					/* Tratamento do switch */
						
					} else if(i.getInstruction() instanceof Select){
						System.out.println("\n[CFGProcessor] Instrução "+ i +"capturada como switch");
						Select selectInst = (Select) i.getInstruction();
						
						if(root.getInstructions().get(0).equals(i)){
							i = i.getNext();
						}
						
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
									inst != null &&
									inst.getPosition() <= lastInstPosOf(root) &&
									inst.getPosition() < selectInstCases[k].getPosition();
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
								inst.getPosition() <= lastInstPosOf(root);
								inst = inst.getNext()){
							switchOutNode.addInstruction(inst);
						}
						
						if(switchOutNode.getInstructions().size() > 1){
							switchNode.addChildNode(switchOutNode, CFGEdgeType.OUT_SW);
							processInnerInformation(switchOutNode, null, processedInstructionIds);
							switchOutNode.getRefToLeaves(switchNode, CFGEdgeType.OUT_SW);
						} else {
							switchNode.setEndNode(true);
						}
						
						return;
						
						
					}else if (i.getInstruction() instanceof ReturnInstruction) {
						
//						CFGNode returnNode = new CFGNode();
//						returnNode.addInstruction(i);
//						root.getInstructions().remove(i);						
//						root.addChildNode(returnNode, CFGEdgeType.RETURN);
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
		while(inst != null &&
				!(inst.getInstruction() instanceof ReturnInstruction) && 
				!(inst.getInstruction().hashCode() == targeter.hashCode()) ){
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
	
	private int lastInstPosOf(CFGNode blockNode) {
		if(!blockNode.getInstructions().isEmpty()){
			return blockNode.getInstructions().get(blockNode.getInstructions().size()-1).getPosition();
		}
		return 0;
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
}