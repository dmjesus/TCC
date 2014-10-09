package visualization;

import java.util.Iterator;

import model.IElement;
import model.SysAdvice;
import model.SysMethod;
import model.SysRoot;
import analysis.MethodAnalysis;
import edu.uci.ics.jung.algorithms.layout.AggregateLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.visualization.VisualizationViewer;

/**the class that constructs a call chain graph*/
public class CallChainM2G {
	
	EspecialEdgesTable<IElement, Object> table = new EspecialEdgesTable<IElement, Object>();
	private int deltaX = 100;
	private int deltaY = 80;

	/**constructs an AggregateLayout with the given SysMethod*/
	public AggregateLayout<IElement, Object> doAggregateLayout( SysRoot root, SysMethod m) {
		MethodAnalysis.analyseMethod(m, root);
		DelegateTree<IElement, Object> dt = new DelegateTree<IElement, Object>();
		dt.addVertex(m);
		dt = addChildToGraph(dt,m);
		Forest<IElement, Object> df = ModelToGraph.tree_to_forest(dt);
		AggregateLayout<IElement, Object> al = new AggregateLayout<IElement, Object>(new TreeLayout<IElement, Object>(df, deltaX, deltaY));
		df=addEspecialEdges(df, table);
		return al;
	}
	
	
	/**constructs a VisualizationViewer from an AggregateLayout*/
	public VisualizationViewer<IElement, Object> makeVV(AggregateLayout<IElement, Object> al){
		VisualizationViewer<IElement, Object> vv =new VisualizationViewer<IElement, Object>(al);		
		return vv;
	}
	
	/**add the vertex that may broke a tree structure*/
	private static Forest<IElement, Object> addEspecialEdges(Forest<IElement, Object> df, EspecialEdgesTable<IElement, Object> table) {
		Iterator<IElement> from = table.getFrom().iterator();
		Iterator<IElement> to = table.getTo().iterator();
		Iterator<Object> edge = table.getEdge().iterator();
		while(from.hasNext() && to.hasNext() && edge.hasNext()){
			//df.addEdge(df.getEdgeCount()+1.5f, from.next(), to.next());
			df.addEdge(((Float)edge.next())+0.5f, from.next(), to.next());
		}
		return df;
	}

	/**add the caller child to the graph*/
	public DelegateTree<IElement, Object> addChildToGraph(DelegateTree<IElement, Object> g, SysMethod caller){
		for(SysMethod m : caller.getCalls()){
			if(g.containsVertex(m)){
				this.table.add(g.getEdgeCount()*1.0f+1.0f+this.table.size(), caller, m);
			} else {
				//g.addChild(g.getVertexCount()*1.0f+1.5f, caller, m);
				g.addChild(g.getEdgeCount()*1.0f+1.5f+this.table.size(), caller, m);
				g = addChildToGraph(g,m);//recursive call to complete the graph and add the needed edges to the table
			}
		}
		for(SysAdvice ad : caller.getAffecters()){
			if(g.containsVertex(ad)){
				this.table.add(g.getEdgeCount()*1.0f+1.0f+this.table.size(), caller, ad);
			} else {
				//g.addChild(g.getVertexCount()*1.0f+1.5f, caller, ad);
				g.addChild(g.getEdgeCount()*1.0f+1.5f + this.table.size(), caller, ad);
				g = addChildToGraph(g,ad);//recursive call to complete the graph and add the needed edges to the table
			}
		}
		return g;
		
	}
	
	



}
