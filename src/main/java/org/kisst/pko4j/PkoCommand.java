package org.kisst.pko4j;

public interface PkoCommand<T extends PkoObject> {
	public T target();
	public T apply();
	public void otherActions(boolean restart);
}