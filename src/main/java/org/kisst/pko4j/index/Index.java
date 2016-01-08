package org.kisst.pko4j.index;

import org.kisst.pko4j.PkoObject;
import org.kisst.pko4j.PkoTable.ChangeHandler;
import org.kisst.pko4j.StorageOption;

public abstract class Index<T extends PkoObject> implements StorageOption<T>, ChangeHandler<T> {
	public final Class<T> recordClass;
	protected Index(Class<T> recordClass) { this.recordClass=recordClass;	}
	
	@Override public Class<T> getRecordClass() { return (Class<T>) this.recordClass; }
}
