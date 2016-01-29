package org.kisst.pko4j;

import org.kisst.item4j.seq.TypedSequence;

public interface StructStorage<T extends PkoObject> extends StorageOption<T> {
	public Class<T> getRecordClass();
	public void create(T value);
	public void update(T oldValue, T newValue);
	public void delete(T oldValue);

	public TypedSequence<T> findAll(PkoModel model);

	public String readBlob(String key, String path);
	public void writeBlob(String key, String path, String blob);
	public void appendBlob(String key, String path, String blob);
}
