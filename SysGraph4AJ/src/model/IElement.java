package model;

import java.util.Set;

public interface IElement {
	
	public IElement getOwner();	
	
	public void setOwner(IElement e);
	
	public void addChild(IElement e);
	
	/**gets all the child elements of this element. It may return an empty hash if there are no child element,
	 * even for this element can not hold a child or it hasn't one*/
	public Set<? extends IElement> getChildElements();

}
