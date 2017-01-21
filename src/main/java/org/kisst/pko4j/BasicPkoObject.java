package org.kisst.pko4j;

import java.time.Instant;

import org.bson.types.ObjectId;
import org.kisst.item4j.HasName;
import org.kisst.item4j.ImmutableSequence;
import org.kisst.item4j.Item;
import org.kisst.item4j.SchemaBase;
import org.kisst.item4j.struct.MultiStruct;
import org.kisst.item4j.struct.SingleItemStruct;
import org.kisst.item4j.struct.Struct;
import org.kisst.item4j.struct.StructHelper;
import org.kisst.pko4j.StructStorage.HistoryItem;
import org.kisst.util.ReflectionUtil;

public abstract class BasicPkoObject<MT extends PkoModel, OT extends PkoObject> implements PkoObject, Struct, PkoModel.MyObject {
	public final MT model;
	public final PkoTable<OT> table;
	public final String _id;
	public final Instant creationDate;
	public final Instant modificationDate;
	public BasicPkoObject(MT model, PkoTable<OT> table, Struct data) {
		//super(table.schema);
		this.model=model;
		this.table=table;
		this._id=createUniqueKey(data);
		this.creationDate=new ObjectId(_id).getDate().toInstant();
		this.modificationDate=Item.asInstant(data.getDirectFieldValue("modificationDate", creationDate));
	}
	public String getKey() { return _id;} 
	@Override public String toString() { return StructHelper.toShortString(this); }
	//@Override public Iterable<String> fieldNames() { return schema.fieldNames(); }
	@Override public Object getDirectFieldValue(String name) { return ReflectionUtil.getFieldValueOrUnknownField(this, name); }
	//@Override public Instant getCreationDate() { return this.creationDate; }
	//@Override public Instant getModificationDate() { return this.modificationDate;}

	abstract public PkoRef<OT> getRef(); 

	protected String createUniqueKey(Struct data) {
		String key= Item.asString(data.getDirectFieldValue("_id",null)); 
		if (key==null)
			return uniqueKey();
		return key;
	}
	protected String uniqueKey() { return new ObjectId().toHexString();}

	public OT changeField(HasName field, Object value) { return changeField(field.getName(), value); }
	@SuppressWarnings("unchecked")
	public OT changeField(String fieldName, Object value) {
		Struct struc= new MultiStruct( 
			new SingleItemStruct(fieldName, value),
			this
		);
		return (OT) model.construct(table.getElementClass(), struc);
	}
	@SuppressWarnings("unchecked")
	public OT changeFields(Struct newFields) { 
		return (OT) model.construct(table.getElementClass(), new MultiStruct(
			newFields,
			this
		));
	}
	
	public <ST> OT addSequenceItem(SchemaBase.SequenceField<ST> field, ST value) {
		ImmutableSequence<ST> oldSequence = field.getSequence(model, this);
		ImmutableSequence<ST> newSequence = oldSequence.growTail(value);
		OT result= changeField(field, newSequence);
		return result;
	}
	@SuppressWarnings("unchecked")
	public  <ST> OT removeSequenceItem(SchemaBase.SequenceField<ST> field, ST value) {
		ImmutableSequence<ST> oldSequence = field.getSequence(model, this);
		int index=0;
		for (ST it: oldSequence) {
			if (it.equals(value)) { // TODO: will equals work?
				ImmutableSequence<ST> newSequence = oldSequence.remove(index);
				return changeField(field, newSequence);
			}
			index++;
		}
		return (OT) this;
	}

	public String readBlob(String path) { return table.storage.readBlob(_id, path); }
	public void writeBlob(String path, String blob) { table.storage.writeBlob(_id, path, blob); }
	public void appendBlob(String path, String blob) { table.storage.appendBlob(_id, path, blob); }
	public ImmutableSequence<HistoryItem> getHistory(String path) { return table.storage.getHistory(_id, path); }
	public ImmutableSequence<HistoryItem> getHistory() { return table.storage.getHistory(_id); }
}
