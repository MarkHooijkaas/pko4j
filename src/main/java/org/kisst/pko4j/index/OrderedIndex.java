package org.kisst.pko4j.index;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListMap;

import org.kisst.item4j.Schema;
import org.kisst.pko4j.PkoModel;
import org.kisst.pko4j.PkoObject;
import org.kisst.pko4j.PkoSchema;
import org.kisst.pko4j.PkoModel.Index;

public class OrderedIndex<MT extends PkoModel, T extends PkoObject<MT,T>> extends AbstractKeyedIndex<MT, T> implements Index<T>, Iterable<T> {
	private final ConcurrentSkipListMap<String, T> map=new ConcurrentSkipListMap<String,T>();
	private final boolean ignoreCase;
	private final FieldList fields;

	public OrderedIndex(PkoSchema<MT, T> schema, boolean ignoreCase, Schema.Field<?> ... fields) { 
		super(schema);
		this.ignoreCase=ignoreCase;
		this.fields=new FieldList(fields);
	}

	private String changeCase(String key) { return ignoreCase ? key.toLowerCase() :  key; }
	@Override public String calcUniqueKey(T record) { return changeCase(fields.getKey(record)); }

	@Override protected void add(String key, T record) { map.put(key, record); }
	@Override protected void remove(String key) { map.remove(key); }
	@Override public boolean keyExists(String key) { return map.containsKey(key); }

	public Schema.Field<?>[] fields() { return fields.fields(); }
	
	@Override public Iterator<T> iterator() { return map.values().iterator(); }

	public Collection<T> tailList(String fromKey) { return map.tailMap(fromKey).values(); } 
	public Collection<T> headList(String toKey) { return map.headMap(toKey).values(); } 
	public Collection<T> subList(String fromKey,String toKey) { return map.subMap(fromKey, toKey).values(); } 
}
