package cfg.gui;


import java.util.ArrayList;
import java.util.List;

import model.SysMethod;
import cfg.model.CFGNode;
import cfg.processing.CFGBuilder;
import cfg.processing.CFGProcessor;

/**
 * Classe utilitária que armazena as informações de forma global para uso das clases responsáveis
 * pelo CFG.
 * 
 * @author robson
 *
 */
public class CFGUIContext {

	public static final CFGBuilder CFG_BUILDER = new CFGBuilder();

	public static final CFGProcessor CONTROL_FLOW_GRAPH_PROCESSOR = new CFGProcessor();
	
	public static SysMethod currentAnalysedMethod;
	
	public static List<CFGNode> allCurrentCFGNodes = new ArrayList<CFGNode>();
	
	public static List<SysMethod> allCurrentAnalysedMethods = new ArrayList<SysMethod>();

}
