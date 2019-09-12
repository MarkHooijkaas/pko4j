package org.kisst.pko4j;

import java.time.Instant;
import java.util.Iterator;

import org.kisst.item4j.ImmutableSequence;
import org.kisst.item4j.seq.TypedSequence;
import org.kisst.pko4j.index.UniqueIndex;
import org.kisst.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PkoTable<T extends PkoObject> implements TypedSequence<T> {
	public static final Logger logger = LoggerFactory.getLogger(PkoTable.class);

	public final PkoModel model;
	public final Class<T> recordClass;
	private final String name;
	final StructStorage<T> storage;
	private final UniqueIndex<T> objects;
	private final ChangeHandler<T>[] indices;

	private boolean alwaysCheckId=true;
	@SuppressWarnings("unchecked")
	public PkoTable(PkoModel model, Class<T> recordClass) { 
		this.model=model;
		this.recordClass=recordClass;
		this.storage=model.getStorage(recordClass);
		this.name=recordClass.getSimpleName();
		this.objects=new UniqueIndex<T>(recordClass, false, rec->rec.getKey());
		this.indices=(ChangeHandler<T>[]) ArrayUtil.join(objects,model.getIndices(recordClass));
	}
	// This can not be done in the constructor, because then the KeyObjects will have a null table

	public void loadFromStorage() { 
		//System.out.println("Loading all "+name+" records to cache");
		TypedSequence<T> seq = storage.findAll(model);
		for (T rec:seq) {
			try {
				if (rec.getKey()==null) 
					System.out.println("rec=null"+rec);
				else
					executeChange(new Change(null,rec));
			}
			catch (RuntimeException e) { e.printStackTrace(); /*ignore*/ } // TODO: return dummy activity
		}
	}
	public String getName() { return name; }

	public synchronized void create(T newValue) {
		if (executeChange(new Change(null,newValue)))
			storage.create(newValue);
	}

	public T read(String key) {
		T result=readOrNull(key);
		if (result==null)
			throw new RuntimeException("Could not find "+name+" for key "+key);
		return result;
	}
	public T readOrNull(String key) {  
		T result;
		result=objects.get(key);
		//System.out.println("struct "+rec.getClass()+"="+rec);
		//System.out.println("object "+obj.getClass()+"="+obj);
		return result;
	}
	public synchronized void update(T oldValue, T newValue) {
		newValue=(T) newValue.changeField("modificationDate", Instant.now());
		checkSameId(oldValue, newValue);
		if (executeChange(new Change(oldValue,newValue)))
			storage.update(oldValue, newValue); 
	}

	public synchronized void delete(T oldValue) {
		if (executeChange(new Change(oldValue,null)))
			storage.delete(oldValue);
	}
	

	private void checkSameId(T oldValue, T newValue) {
		if (! alwaysCheckId)
			return;
		String newId = newValue.getKey();
		if (newId!=null) {
			String oldId = oldValue.getKey();
			if (!newId.equals(oldId))
				throw new IllegalArgumentException("Trying to update object with id "+oldId+" with object with id "+newId+": "+oldValue+"->"+newValue);
		}
	}
	public TypedSequence<T> findAll() {  
		return ImmutableSequence.smartCopy(model, recordClass,objects.getAll());
	}


	@Override public int size() { return findAll().size();}
	@Override public Object getObject(int index) { return findAll().get(index); }
	@Override public Iterator<T> iterator() { return findAll().iterator(); }
	@Override public Class<T> getElementClass() { return recordClass; }

	public void saveAll() { storage.saveAll(this); }

	public class Change {
		//public final Instant time;

		public final T oldRecord;
		public final T newRecord;
		public Change(T oldRecord, T newRecord) { this.oldRecord=oldRecord; this.newRecord=newRecord; }
		@Override public String toString() {
			String oldId=oldRecord==null? "null" : oldRecord.getKey();
			String newId=newRecord==null? "null" : newRecord.getKey();
			return "Change("+oldId+","+newId+")"; 
		} 
	}
	public interface ChangeHandler<TT extends PkoObject> {
		public boolean allow(PkoTable<TT>.Change change); 
		public void commit(PkoTable<TT>.Change change); 
		public void rollback(PkoTable<TT>.Change change);
		public Class<TT> getRecordClass();
	}

	private final static Logger changeLogger = LoggerFactory.getLogger(PkoTable.Change.class);
	
	private boolean executeChange(Change change) {
		changeLogger.info("applying {}", change);
		if (allow(change))
			return commmit(change);
		return false;
	}

	private boolean allow(Change change) {
		changeLogger.debug("asking to allow {}",change);
		for (ChangeHandler<T> res : indices) {
			try { 
				changeLogger.debug("asking {}",res);
				if (! res.allow(change)) {
					changeLogger.warn("Resource {} refused {}",res,change);
					return false;
				}
			} 
			catch(RuntimeException e) { 
				logger.error("Error preparing {}", change, e);
				return false;
			}
		}
		return true;
	}

	
	private boolean commmit(Change change) {
		changeLogger.debug("commiting {}",change);
		for (int index=0; index<indices.length; index++) {
			ChangeHandler<T> res = indices[index];
			changeLogger.debug("commiting in {}",res);
			try { res.commit(change); } 
			catch(RuntimeException e) { 
				changeLogger.error("Error commiting {}", change, e);
				rollback(change,index-1);
				return false;
			}
		}
		return true;
	}

	private void rollback(Change change, int highestIndex) {
		for (int index=highestIndex; index>=0; index--) {
			try { indices[index].rollback(change); } 
			catch(RuntimeException e) { changeLogger.error("Error rolling back {}", change, e);}
		}
	}
}
