package org.kisst.pko4j;

public class PkoRef<MT extends PkoModel, T extends PkoObject<MT, T>>  {
	public final String _id;
	public final PkoTable<MT, T> table;

	protected PkoRef(PkoTable<MT, T> table, String _id) { this.table=table; this._id=_id; }
	
	public T get() { return table.read(_id); }
	public T get0() { return table.readOrNull(_id); }

	@Override public String toString() { return _id; } //return "Ref("+table.getName()+":"+_id+")";}
	@Override public boolean equals(Object obj) {
		if (obj==null)
			return false;
		if (obj==this)
			return true;
		if (! (obj instanceof PkoRef))
			return false;
		PkoRef<?,?> ref=(PkoRef<?,?>) obj;
		if (this.table!=ref.table)
			return false;
		return this._id.equals(ref._id);
	}
	@Override public int hashCode() { return (_id+table).hashCode(); }
}
