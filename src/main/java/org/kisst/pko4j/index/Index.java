package org.kisst.pko4j.index;

import org.kisst.item4j.Schema;
import org.kisst.pko4j.PkoModel;
import org.kisst.pko4j.PkoObject;
import org.kisst.pko4j.StorageOption;
import org.kisst.pko4j.PkoTable.ChangeHandler;

public abstract class Index<MT extends PkoModel, T extends PkoObject<MT,T>> implements StorageOption, ChangeHandler<MT, T> {
	public final Schema schema;
	protected Index(Schema schema) { this.schema=schema;	}
	
	@SuppressWarnings("unchecked")
	@Override public Class<T> getRecordClass() { return (Class<T>) this.schema.getJavaClass(); }
}
