package org.kisst.pko4j.index;

import java.util.concurrent.ConcurrentHashMap;

import org.kisst.item4j.ImmutableSequence;
import org.kisst.pko4j.PkoModel.Index;
import org.kisst.pko4j.PkoObject;

public class UniqueIndex<T extends PkoObject> extends AbstractKeyedIndex<T> implements Index<T> {
	private final boolean ignoreCase;
	private final ConcurrentHashMap<String, T> map = new ConcurrentHashMap<String, T>();
	
	public UniqueIndex(Class<T> recordClass, boolean ignoreCase, KeyCalculator<T> keyCalculator) { 
		super(recordClass,keyCalculator);
		this.ignoreCase=ignoreCase;
	}
	
	private String changeCase(String key) { return ignoreCase ? key.toLowerCase() :  key; }
	
	@Override protected void add(String key, T record) { map.put(changeCase(key), record); }
	@Override protected void remove(String key) { map.remove(changeCase(key)); }
	@Override public boolean keyExists(String key) { return map.containsKey(changeCase(key)); }

	public ImmutableSequence<T> getAll() { 
		return ImmutableSequence.smartCopy(null/*schema.model*/, recordClass, map.values());
	}

	public T get(String key) { return map.get(changeCase(key)); }

}
