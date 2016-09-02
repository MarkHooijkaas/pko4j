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
	protected String[] calcUniqueKey(T record) { return keyCalculator.calcKey(record); }

	@Override public Class<T> getRecordClass() { return recordClass;}
	public ConcurrentHashMap.KeySetView<String, ImmutableSequence<T>> keySet() { return map.keySet();}
	public TreeSet<String> sortedKeys() { return new TreeSet<String>(map.keySet());}
	public ImmutableSequence<T> records(String key) { return map.get(key);}


	@Override public boolean allow(PkoTable<T>.Change change) { return true; }
	@Override public void rollback(PkoTable<T>.Change change) {}

	@Override public void commit(PkoTable<T>.Change change) {
		logger.debug("committing {}", change);
		// TODO: should we check the prepare again??
		if (change.oldRecord != null && change.newRecord!=null)
			change(change.oldRecord, change.newRecord);
		else if (change.oldRecord != null)
			remove(change.oldRecord);
		else if (change.newRecord != null)
			add(change.newRecord);
	}

	protected void remove(T oldRecord) {
		for (String oldkey : calcUniqueKey(oldRecord)) {
			removeRecord(oldRecord, oldkey);
		}
	}


	protected void add(T newRecord) {
		for (String newkey : calcUniqueKey(newRecord))
			addRecord(newRecord, newkey);
	}


	protected void change(T oldRecord, T newRecord) {
		String[] oldKeys=calcUniqueKey(oldRecord);
		String[] newKeys=calcUniqueKey(newRecord);
		for (String oldKey :  oldKeys) {
			if (!contains(newKeys,oldKey))
				removeRecord(oldRecord, oldKey);
		}
		for (String newKey :  newKeys) {
			if (!contains(oldKeys,newKey))
				addRecord(newRecord, newKey);
		}
	}

	private void removeRecord(T oldRecord, String oldkey) {
		logger.info("removing unique key {} ", oldkey);
		ImmutableSequence<T> oldList = map.get(oldkey);
		ImmutableSequence<T> newList = oldList.removeItem(oldRecord);
		if (newList.size() != oldList.size())
			map.put(oldkey, newList);
	}

	private void addRecord(T newRecord, String newkey) {
		logger.info("adding unique key {} ", newkey);
		ImmutableSequence<T> oldList = map.get(newkey);
		if (oldList==null)
			map.put(newkey, ImmutableSequence.of(getRecordClass(), newRecord));
		else if (! oldList.contains(newRecord)) {
			ImmutableSequence<T> newList = oldList.growTail(newRecord);
			map.put(newkey, newList);
		}
	}

	public static <T> boolean contains(final T[] array, final T v) {
		if (v == null) {
			for (final T e : array)
				if (e == null)
					return true;
		} else {
			for (final T e : array)
				if (e == v || v.equals(e))
					return true;
		}

		return false;
	}
}


