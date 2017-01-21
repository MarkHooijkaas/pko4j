package org.kisst.pko4j;

public interface PkoObject {
	public String getKey();
	public String getName();
	public PkoRef<?> getRef();

	public PkoObject changeField(String fieldName, Object value); // TODO: generic result type?
}
