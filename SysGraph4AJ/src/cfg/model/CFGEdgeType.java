package cfg.model;

import cfg.gui.CFGEdge;

/**
 * Representa o tipo relacionado a um nó pai com um nó filho.
 * 
 * @author robson
 *
 * @see CFGNode
 * @see CFGEdge
 */
public enum CFGEdgeType {
	
	TRY,
	
	CATCH,
	
	FINALLY,
	
	T,
	
	F,
	
	GOTO,
	
	CASE,
	
	DEFAULT,
	
	REFERENCE

}
