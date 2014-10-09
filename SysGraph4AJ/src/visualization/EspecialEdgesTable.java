package visualization;

import java.util.Vector;

/** */
public class EspecialEdgesTable<V, E> {

	Vector<V> from = new Vector<V>();
	Vector<V> to = new Vector<V>();
	Vector<E> edge = new Vector<E>();

	public void add(E e, V from, V to){
		this.from.add(from);
		this.to.add(to);
		this.edge.add(e);

	}

	public void remove(E e, V from, V to){
		if(this.from.contains(from) && this.edge.contains(e) && this.to.contains(to)){
			this.from.remove(from);
			this.to.remove(to);
			this.edge.remove(e);
		}
	}

	public Vector<V> getFrom(){return from;}
	public Vector<V> getTo(){return to;}
	public Vector<E> getEdge(){return edge;}

	public void add(EspecialEdgesTable<V, E> especialEdges) {
		if(especialEdges!=null){
			Vector<V> from = especialEdges.getFrom();
			Vector<V> to = especialEdges.getTo();
			Vector<E> edge = especialEdges.getEdge();
			if(from.size()==to.size() && to.size()==edge.size()){
				for(int i=0;i<from.size();i++){
					this.from.add(from.get(i));
					this.to.add(to.get(i));
					this.edge.add(edge.get(i));
				}
			}
		}
	}
	
	public int size(){
		return edge.size();
	}
}
