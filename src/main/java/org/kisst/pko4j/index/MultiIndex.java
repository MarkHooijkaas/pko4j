package org.kisst.pko4j.index;

import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.kisst.item4j.ImmutableSequence;
import org.kisst.pko4j.PkoObject;
import org.kisst.pko4j.PkoTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public  class MultiIndex<T extends PkoObject> implements PkoTable.ChangeHandler<T> {
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

	@Override public void commit(PkoTable<T>.Change change) {
		logger.debug("committing {}", change);
		String[] newList = null;
		if (change.newRecord != null) {
			newList = calcKeys(change.newRecord);
			for (String newkey : newList)
				addRecord(newkey, change.newRecord);
		}
		if (change.oldRecord != null){
			for (String oldkey : calcKeys(change.oldRecord)) {
				if (newList==null || ! contains(newList,oldkey))
				removeRecord(oldkey, change.oldRecord);
			}
		}
	}

	private boolean contains(String[] newList, String oldkey) {
		for (String s: newList)
			if (oldkey.equals(s))
				return true;
		return false;
	}

	private void removeRecord(String oldkey, T oldRecord) {
		logger.info("removing unique key {} ", oldkey);
		ImmutableSequence<T> oldList = map.get(oldkey);
		ImmutableSequence<T> newList = oldList.removeItem(oldRecord);
		if (newList.size() != oldList.size())
			map.put(oldkey, newList);
	}

	private void addRecord(String newkey, T newRecord) {
		logger.info("adding unique key {} ", newkey);
		ImmutableSequence<T> oldList = map.get(newkey);
		if (oldList==null)
			map.put(newkey, ImmutableSequence.of(getRecordClass(), newRecord));
		else if (! oldList.contains(newRecord)) {
			ImmutableSequence<T> newList = oldList.growTail(newRecord);
			map.put(newkey, newList);
		}
	}
}


