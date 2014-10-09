package model;


public interface SysElement extends IElement {

	/**gets the element name*/
	public String getName();

	/**gets the fully qualified name*/
	public String getFullyQualifiedName();

	/**returns true if this element represents the same element of this instance*/
	public boolean equals(SysElement e);

	/**returns a partial clone of this element, usually just name and owner*/
	public SysElement partialClone();

	/**gets the required element. 
	 * @param thisName should be just a single name, not a fully qualified name*/
	public SysElement get(String thisName, String sig, boolean isLast);

	/**@return the maximum element given the fully qualified name*/
	public SysElement getMax(String called, String sig);
	
	/**@return String a string that represents all the details of this element*/
	public String viewState();
}
