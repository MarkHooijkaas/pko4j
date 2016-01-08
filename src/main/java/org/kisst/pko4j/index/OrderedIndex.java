package org.kisst.pko4j.index;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListMap;

import org.kisst.pko4j.PkoModel.Index;
import org.kisst.pko4j.PkoObject;

public class OrderedIndex<T extends PkoObject> extends AbstractKeyedIndex<T> implements Index<T>, Iterable<T> {
	private final ConcurrentSkipListMap<String, T> map=new ConcurrentSkipListMap<String,T>();

	public OrderedIndex(Class<T> recordClass, KeyCalculator<T> keyCalculator) { 
		super(recordClass, keyCalculator);
	}


	@Override protected void add(String key, T record) { map.put(key, record); }
	@Override protected void remove(String key) { map.remove(key); }
	@Override public boolean keyExists(String key) { return map.containsKey(key); }

	@Override public Iterator<T> iterator() { return map.values().iterator(); }

	public Collection<T> tailList(String fromKey) { return map.tailMap(fromKey).values(); } 
	public Collection<T> headList(String toKey) { return map.headMap(toKey).values(); } 
	public Collection<T> subList(String fromKey,String toKey) { return map.subMap(fromKey, toKey).values(); } 
}
