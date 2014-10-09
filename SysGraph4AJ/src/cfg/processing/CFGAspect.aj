package cfg.processing;

import java.util.ArrayList;

import model.SysMethod;
import cfg.gui.CFGUIContext;
import cfg.model.CFGNode;

/**
 * Aspecto responsável por afetar as operações referentes à CFG no sistema.
 * 
 * @author robson
 *
 */
public aspect CFGAspect {
	
	pointcut reloadAllLists(gui.MainWindow m) : execution(public void analyse()) && target(m);
	
	after(gui.MainWindow m) : reloadAllLists(m) {
		CFGUIContext.currentAnalysedMethod = null;
		CFGUIContext.allCurrentCFGNodes = new ArrayList<CFGNode>();
		CFGUIContext.allCurrentAnalysedMethods = new ArrayList<SysMethod>();
	}
}
