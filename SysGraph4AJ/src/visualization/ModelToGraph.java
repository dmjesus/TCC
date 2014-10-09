package visualization;

import java.util.Vector;

import model.IElement;
import model.SysAdvice;
import model.SysAspect;
import model.SysClass;
import model.SysElement;
import model.SysMethod;
import model.SysPackage;
import model.SysRoot;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;

public class ModelToGraph {

	public static DelegateTree<IElement, Float> delegateTree_justFirstChildren(IElement root) {
		DelegateTree<IElement, Float> dt = new DelegateTree<IElement, Float>();
		dt.addVertex(root);
		for(IElement e : root.getChildElements())
			dt.addChild((float)dt.getEdgeCount(), root, e);
				return dt;
	}

	public static DelegateTree<IElement, Object> delegateTree_fullGraph(IElement root) {
		DelegateTree<IElement, Object> dt = new DelegateTree<IElement, Object>();
		dt.addVertex(root);
		dt = putAllChildren(dt, root);
		return dt;
	}

	public static DelegateTree<IElement, Object> putAllChildren_SysRoot(DelegateTree<IElement, Object> dt, SysRoot e){
		if(e != null){
			for(SysElement e1 : e.getPackages()){
				dt.addChild((float)dt.getEdgeCount(), e, e1);
				dt = putAllChildren(dt, e1);
			}
		}
		return dt;
	}

	public static DelegateTree<IElement, Object> putAllChildren(DelegateTree<IElement, Object> dt, IElement e){
		if(e != null){
			
			for(IElement e1 : e.getChildElements()){
				dt.addChild((float)dt.getEdgeCount(), e, e1);
				dt = putAllChildren(dt, e1);
			}
		}
		return dt;
	}

	public static EspecialEdgesTable<IElement, Object> getEspecialEdges(SysRoot r, Graph g){
		if(r!=null){
			EspecialEdgesTable<IElement, Object> et = new EspecialEdgesTable<IElement, Object>();
			for(SysPackage p : r.getPackages()){
				et.add(getEspecialEdges(p,g));
			}
			return et;
		}
		else return null;
	}

	private static EspecialEdgesTable<IElement, Object> getEspecialEdges(SysPackage pack, Graph g) {
		if(pack!=null){
			EspecialEdgesTable<IElement, Object> et = new EspecialEdgesTable<IElement, Object>();
			for(SysPackage p : pack.getPackages()){
				et.add(getEspecialEdges(p,g));
			}
			for(SysClass c : pack.getClasses()){
				et.add(getEspecialEdges(c,g));
			}
			for(SysAspect a : pack.getAspects()){
				et.add(getEspecialEdges(a,g));
			}
			return et;
		}
		return null;
	}

	private static EspecialEdgesTable<IElement, Object> getEspecialEdges(SysAspect c, Graph g) {
		EspecialEdgesTable<IElement, Object> e = getEspecialEdges((SysClass) c, g);
		if(e==null) e = new EspecialEdgesTable<IElement, Object>();
		for(SysAdvice ad : c.getAdvice()){
			e.add(getEspecialEdges(ad,g));
		}
		return e;
	}

	private static EspecialEdgesTable<IElement, Object> getEspecialEdges(SysClass c, Graph g) {
		if(c!=null){
			EspecialEdgesTable<IElement, Object> et = new EspecialEdgesTable<IElement, Object>();
			for(SysMethod m : c.getMethods()){
				et.add(getEspecialEdges(m,g));
			}
			return et;
		}
		return null;
	}

	private static EspecialEdgesTable<IElement, Object> getEspecialEdges(SysMethod m, Graph g) {
		if(m!=null){
			EspecialEdgesTable<IElement, Object> et = new EspecialEdgesTable<IElement, Object>();
			for(SysMethod s : m.getCalls()){
				et.add((float)g.getEdgeCount()+1,m,s);
			}
			return et;
		}
		return null;
	}

	private static EspecialEdgesTable<IElement, Object> getEspecialEdges(SysAdvice m, Graph g) {
		EspecialEdgesTable<IElement, Object> e = getEspecialEdges((SysMethod)m,g);
		if(e==null) e= new EspecialEdgesTable<IElement,Object>();
		for(SysMethod me: m.getAffecteds()){
			e.add((float)g.getEdgeCount()+1, m, me);
		}
		return e;
	}

	public static void addEspecialEdges(Forest<IElement, Object> f, EspecialEdgesTable<IElement, Object> et){
		if(et!=null && f!=null){
			Vector<IElement> from = et.getFrom();
			Vector<IElement> to = et.getTo();
			Vector<Object> edge = et.getEdge();
			for(int i=0;i<edge.size();i++){
				f.addEdge((float)f.getEdgeCount()+1.5f, from.get(i), to.get(i));
			}
		}else System.err.println("et==null || f==null");
	}
	
	public static void removeEspecialEdges(DelegateForest<IElement, Object> f, EspecialEdgesTable<IElement, Object> et) {
		
		if(et != null) {
			Vector<Object> edges = et.getEdge();
			for(Object edge : edges) {
				float modifiedEdge = (Float)edge + 0.5f;
				f.removeEdge(modifiedEdge, false);
			}
		}
	}

	public static DelegateForest<IElement, Object> tree_to_forest(DelegateTree dt){
		if(dt==null)return null;
		DelegateForest df = new DelegateForest();
		for(Object v1 : dt.getVertices()){
			df.addVertex(v1);
		}
		Pair p;
		for(Object e1 : dt.getEdges()){
			p = dt.getEndpoints(e1);
			df.addEdge(e1, p.getFirst(), p.getSecond());
		}
		return df;		
	}






}
