package org.kisst.pko4j;

public class PkoRef<T extends PkoObject>  {
	private final String key;
	public final PkoTable<T> table;

	protected PkoRef(PkoTable<T> table, String key) { this.table=table; this.key=key; assert table!=null; assert key!=null;	}
	
	public T get() { return table.read(key); }
	public T get0() { return table.readOrNull(key); }
	public String getKey() { return key; }
	public boolean refersTo(T rec) { return key.equals(rec.getKey()); }

	@Override public String toString() { return key; } //return "Ref("+table.getName()+":"+_id+")";}
	@Override public boolean equals(Object obj) {
		if (obj==null)
			return false;
		if (obj==this)
			return true;
		if (! (obj instanceof PkoRef))
			return false;
		PkoRef<?> ref=(PkoRef<?>) obj;
		if (this.table!=ref.table)
			return false;
		return this.key.equals(ref.key);
	}
	@Override public int hashCode() { return (key+table).hashCode(); }
}
