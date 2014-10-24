package cfg.gui;

import edu.uci.ics.jung.algorithms.layout.AggregateLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import gui.GUIWindowInterface;
import gui.SysUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import model.IElement;
import model.SysMethod;
import model.SysRoot;

import org.apache.bcel.generic.InstructionHandle;

import visualization.EspecialEdgesTable;
import visualization.ModelToGraph;
import cfg.model.CFGEdgeType;
import cfg.model.CFGNode;
import cfg.processing.CFGProcessor;

/**
 * Classe responsável pelas operações que relacionam um {@link CFGNode} e alguma interface gráfica.
 *  
 * @author robson
 *
 */
public class CFGModelToGraph {

	/**
	 * @param root
	 * 		nó raiz que está sendo renderizado na {@link GUIWindowInterface}
	 * @param method
	 * 		nó filho do tipo {@link SysMethod} que contém o método que será utilzado na construção da CFG
	 * @param windowInterface
	 * 		interface gráfica que será atualizada com as informações do CFG
	 */
	public static void addCFGToWindowInterface(SysRoot root, SysMethod method, GUIWindowInterface windowInterface) {
		if(CFGUIContext.allCurrentAnalysedMethods.contains(method)) {
			return;
		}
		//get elapsed time to perform a CFG construction 
		long start = System.nanoTime();
		CFGNode cfg = CFGUIContext.CFG_BUILDER.build(method);
		cfg.setSysMethod(CFGUIContext.currentAnalysedMethod);
		CFGUIContext.allCurrentCFGNodes.add(cfg);
		//get elapsed time to perform a CFG construction
		long end = System.nanoTime();
		String elapsed = "Elapsed time to perform action: "+(end-start)/1000000.0d+" ms\n";
		windowInterface.getTextArea().append(elapsed);
      
		reloadMainGraphWithCFGInformations(root, windowInterface, cfg);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void reloadMainGraphWithCFGInformations(SysRoot root, GUIWindowInterface windowInterface, IElement targetElement) {
		//Obtendo referência para a floresta populada na janela
		//No momento de analise de classes eu tenho apenas classes e pacotes. Não deveria se ter todo o grafo que representa o código em root já?
		VisualizationViewer<IElement, Object> visualizationViewer = (VisualizationViewer<IElement, Object>) windowInterface.getCenter();
		Layout<IElement, Object> graphLayout = visualizationViewer.getGraphLayout();
		DelegateForest<IElement, Object> delegateForest = (DelegateForest<IElement, Object>) graphLayout.getGraph();
		
		//Adicionando vertices do nó raiz analisado
		DelegateTree<IElement, Object> delegateTree = new DelegateTree<IElement, Object>();
		delegateTree.addVertex(root);
		delegateTree = ModelToGraph.putAllChildren_SysRoot(delegateTree, root);

		//Adicionando vertices dos CFGs analisados
		delegateForest = new DelegateForest<IElement, Object>();
		CFGModelToGraph.addAllCFGNodesToDelegateTree(delegateTree, CFGUIContext.allCurrentCFGNodes);
		delegateForest.addTree(delegateTree);

		//Criando um novo visualizationViewer
		AggregateLayout<IElement, Object> aggregateLayout = new AggregateLayout<IElement, Object>(new TreeLayout<IElement, Object>(delegateForest, 100, 100));
		VisualizationViewer<IElement, Object> newVisualizationViewer = new VisualizationViewer<IElement, Object>(aggregateLayout);

		//Aplica estilo no grafo, como centralizar o nó analisado e adicionar cores aos vertices
		windowInterface.setCenterPanel(newVisualizationViewer);
		SysUtils.makeGoodVisual(newVisualizationViewer, windowInterface);
		windowInterface.makeMenuBar(newVisualizationViewer);
		SysUtils.setAtCenter(targetElement, aggregateLayout, windowInterface.getFrame(), newVisualizationViewer);

		//Adiciona arestas de referências após a criação da árvore, pois essas arestas formam 'ciclos' na floresta, 'quebrando' a árvore
		EspecialEdgesTable<IElement, Object> et = ModelToGraph.getEspecialEdges((SysRoot) root, delegateForest);
		ModelToGraph.addEspecialEdges(delegateForest, et);
		CFGModelToGraph.addAllReferenceEdgesFromCFGToDelegateForest(delegateForest, CFGUIContext.allCurrentCFGNodes);

		windowInterface.getTextArea().append("Analysing: " + targetElement.toString()+"\n");
		
		//Adiciona ao prompt da tela as informações dos nós analisados
		((VisualizationViewer) windowInterface.getCenter()).updateUI();
	}
	
	/**
	 * 
	 * @param delegateTree
	 * 		árvore referenciada na adição dos nós
	 * @param nodes
	 * 		lista de {@link CFGNode} a serem adicionados na {@link DelegateTree}.
	 */
	public static void addAllCFGNodesToDelegateTree(DelegateTree<IElement, Object> delegateTree, List<CFGNode> nodes) {
		if(delegateTree != null && nodes != null) {
			for(CFGNode node : nodes) {
				delegateTree.addChild(UUID.randomUUID(), node.getSysMethod(), node);
				addCFGNodeAndItsChildrenToTree(node, delegateTree);
			}
		}
	}

	/**
	 * 
	 * @param delegateForest
	 * 		floresta referenciada na adição das arestas
	 * @param nodes
	 * 		lista de {@link CFGNode} a serem adicionados na {@link DelegateTree}.
	 */
	public static void addAllReferenceEdgesFromCFGToDelegateForest(DelegateForest<IElement, Object> delegateForest, List<CFGNode> nodes) {
		if(delegateForest != null && nodes != null) {
			for(CFGNode node : nodes) {
				addReferenceEdgesToForest(node, delegateForest);
			}
		}
	}

	/**
	 * 
	 * @param root
	 * 		nó que será adicionado na árvore
	 * @param delegateTree
	 * 		árvore que será atualizada com o nó e seus respectivos filhos
	 */
	@SuppressWarnings("unchecked")
	public static void addCFGNodeAndItsChildrenToTree(CFGNode root, DelegateTree<IElement, Object> delegateTree) {
		if(delegateTree.getVertexCount() == 0) {
			delegateTree.addVertex(root);
		}
		
		if(root == null){
			return;
		}

		for(IElement node : root.getChildElements()) {
			CFGNode childNode = (CFGNode) node;
			CFGEdge edge = new CFGEdge(root, childNode, root.getChildTypeByNode(childNode));
			childNode.setSysMethod(CFGUIContext.currentAnalysedMethod);
			
			if(!childNode.isReference() && !delegateTree.containsVertex(childNode)) {
				delegateTree.addChild(edge, root, childNode);
				addCFGNodeAndItsChildrenToTree(childNode, delegateTree);
			} 
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void addReferenceEdgesToForest(final CFGNode root, DelegateForest<IElement, Object> delegateForest) {
		if(root == null){
			return;
		} else {
//			Iterator<CFGNode> childNodes = (Iterator<CFGNode>) root.getChildElements().iterator();
			
			for(IElement node : root.getChildElements()){
				CFGNode cfgNode = (CFGNode) node;
				if(cfgNode.getParents().size() > 1){
					List<CFGNode> parents = cfgNode.getParents();
					for(CFGNode parent : parents){
						CFGEdge edge = new CFGEdge(parent, cfgNode, parent.getChildTypeByNode(cfgNode));
						delegateForest.addEdge(edge, parent, cfgNode);
					}
				}
				addReferenceEdgesToForest(cfgNode,delegateForest);
			}
			
//			while(childNodes.hasNext()){
//				CFGNode node = childNodes.next();
//				if(node.getParents().size() > 1){
//					List<CFGNode> parents = node.getParents();
//					for(CFGNode parent : parents){
//						CFGEdge edge = new CFGEdge(parent, node, parent.getChildTypeByNode(node));
//						delegateForest.addEdge(edge, parent, node);
//					}
//				}
//				addReferenceEdgesToForest(node,delegateForest);
//			}
		}
	}

//	/**
//	 * 
//	 * @param root
//	 * 		nó raiz
//	 * @param delegateForest
//	 * 		floresta que será atualizada com as arestas remanescentes
//	 */
//	@SuppressWarnings("unchecked")
//	public static void addReferenceEdgesToForest(final CFGNode root, DelegateForest<IElement, Object> delegateForest) {
//		Iterator<CFGNode> childNodes = (Iterator<CFGNode>) root.getChildElements().iterator(); 
//
//		while(childNodes.hasNext()) {
//
//			CFGNode childNode = childNodes.next();
//
//			if(childNode.isReference() && root.getOwner() != null) {
////			if(childNode.isReference()) {
//				CFGNode parentNode = (CFGNode) root.getOwner();
////				CFGNode parentNode = (CFGNode) childNode.getOwner();
//
//				CFGNode referencedNode = null;
//				boolean referenceFound = false;
//				InstructionHandle instructionToBeReferenced = childNode.getInstructions().get(0);
//
//				do {
//					for(CFGNode childNodesToCheck : parentNode.getChildNodes().keySet()) {
//
//						if(!childNodesToCheck.isReference() 
//								&& !childNodesToCheck.getInstructions().isEmpty() 
//								&& childNodesToCheck.getInstructions().get(0).equals(instructionToBeReferenced)
//								&& !childNodesToCheck.isTryStatement()) {
//							referenceFound = true;
//							referencedNode = childNodesToCheck;
//							break;
//						}
//					}		
//
//					if(!referenceFound) {
//						parentNode = (CFGNode) parentNode.getOwner();
//					}
//
//				} while(parentNode != null && !referenceFound);
//
//				if(referenceFound) {
//					CFGNode parentNodeFromChild = (CFGNode) childNode.getOwner();
//					CFGEdge edge = new CFGEdge(parentNodeFromChild, referencedNode, parentNodeFromChild.getChildTypeByNode(childNode));
//
//					if(!parentNodeFromChild.equals(referencedNode)) {				
//						delegateForest.addEdge(edge, parentNodeFromChild, referencedNode);
//
//					} else {
//						delegateForest.addEdge(edge, parentNodeFromChild, parentNodeFromChild);
//					}
//				}
//			} else {
//				addReferenceEdgesToForest(childNode, delegateForest);
//			}
//
//		}
//	}
}
