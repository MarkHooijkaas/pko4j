package org.kisst.pko4j.index;

import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.kisst.item4j.ImmutableSequence;
import org.kisst.pko4j.PkoObject;
import org.kisst.pko4j.PkoTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public  class MultiIndex<T extends PkoObject> implements PkoTable.ChangeHandler<T> {
	// TODO: improve to only  object Refs instead of Objects

	public final static Logger logger = LoggerFactory.getLogger(MultiIndex.class);
	private final ConcurrentHashMap<String, ImmutableSequence<T>> map = new ConcurrentHashMap<>();
	private final Class<T> recordClass;

	@FunctionalInterface
	public interface KeyCalculator<TT extends PkoObject> { String[] calcKey(TT rec); }

	private final KeyCalculator<T> keyCalculator;

	public MultiIndex(Class<T> recordClass, KeyCalculator<T> keyCalculator) {
		this.recordClass=recordClass;
		this.keyCalculator=keyCalculator;
	}
	protected String[] calcKeys(T record) { return keyCalculator.calcKey(record); }

	@Override public Class<T> getRecordClass() { return recordClass;}
	public ConcurrentHashMap.KeySetView<String, ImmutableSequence<T>> keySet() { return map.keySet();}
	public TreeSet<String> sortedKeys() { return new TreeSet<String>(map.keySet());}
	public ImmutableSequence<T> records(String key) { return map.get(key);}


	@Override public boolean allow(PkoTable<T>.Change change) { return true; }
	@Override public void rollback(PkoTable<T>.Change change) {}


	private static String[] emptyList=new String[0];
	@Override public void commit(PkoTable<T>.Change change) {
		logger.debug("committing {}", change);
		String[] oldList = emptyList;
		String[] newList = emptyList;
		if (change.oldRecord != null) {
			for (String key : calcKeys(change.oldRecord))
				removeRecord(key, change.oldRecord);
		}
		if (change.newRecord != null) {
			for (String key : calcKeys(change.newRecord))
				addRecord(key, change.newRecord);
		}
	}

	private boolean contains(String[] list, String oldkey) {
		for (String s: list)
			if (oldkey.equals(s))
				return true;
		return false;
	}

	private void removeRecord(String key, T oldRecord) {
		logger.info("removing unique key {} ", key);
		ImmutableSequence<T> oldList = map.get(key);
		ImmutableSequence<T> newList = oldList.removeItem(oldRecord);
		if (newList.size() != oldList.size())
			map.put(key, newList);
	}

	private void addRecord(String key, T newRecord) {
		logger.info("adding unique key {} ", key);
		ImmutableSequence<T> oldList = map.get(key);
		if (oldList==null)
			map.put(key, ImmutableSequence.of(getRecordClass(), newRecord));
		else if (! oldList.contains(newRecord)) {
			ImmutableSequence<T> newList = oldList.growTail(newRecord);
			map.put(key, newList);
		}
	}
}


