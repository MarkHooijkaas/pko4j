package org.kisst.pko4j;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import org.kisst.item4j.Item;
import org.kisst.item4j.Schema;
import org.kisst.item4j.struct.Struct;
import org.kisst.pko4j.PkoTable.ChangeHandler;
import org.kisst.util.ReflectionUtil;

public abstract class PkoModel implements Item.Factory {
	public interface MyObject {}

	
	private final StorageOption[] options;

	public PkoModel() { this(new StorageOption[0]); }
	public PkoModel(StorageOption[] options) {
		this.options=options;
	}

	public void initModel() {
		for (PkoTable<?,?> table: ReflectionUtil.getAllDeclaredFieldValuesOfType(this, PkoTable.class))
			table.initcache();
	}
	public void close() {
		for (PkoTable<?,?> table : ReflectionUtil.getAllDeclaredFieldValuesOfType(this, PkoTable.class))
			table.close();
	}

	public StructStorage getStorage(Class<?> cls) {
		for (StorageOption opt: options) {
			if (opt instanceof StructStorage && opt.getRecordClass()==cls)
				return (StructStorage) opt;
		}
		throw new RuntimeException("Unknown Storage for type "+cls.getSimpleName());
	}
	@SuppressWarnings("unchecked")
	public<MT extends PkoModel, T extends PkoObject<MT,T>> ChangeHandler<MT, T>[] getIndices(Class<?> cls) {
		ArrayList<ChangeHandler<MT,T>> result=new ArrayList<ChangeHandler<MT,T>>();
		for (Object opt: ReflectionUtil.getAllDeclaredFieldValuesOfType(this, Index.class)) {
			if (opt instanceof Index && ((Index<?>) opt).getRecordClass()==cls) {
				result.add((ChangeHandler<MT,T>) opt);
			}
		}
		ChangeHandler<MT,T>[] arr=new ChangeHandler[result.size()];
		for (int i=0; i<result.size(); i++)
			arr[i]=result.get(i);
		return arr;
	}

	@Override public <T> T construct(Class<?> cls, Struct data) {
		if (MyObject.class.isAssignableFrom(cls)) {
			//System.out.println("Trying to construct "+cls.getName());
			Constructor<?> cons=ReflectionUtil.getConstructor(cls, new Class<?>[]{ this.getClass(), Struct.class} );
			return cast(ReflectionUtil.createObject(cons, new Object[] {this, data}));
		}
		return basicFactory.construct(cls, data);
	}
	@Override public <T> T construct(Class<?> cls, String data) { 
		if (MyObject.class.isAssignableFrom(cls)) {
			//System.out.println("Trying to construct "+cls.getName());
			Constructor<?> cons=ReflectionUtil.getConstructor(cls, new Class<?>[]{ this.getClass(), String.class} );
			return cast(ReflectionUtil.createObject(cons, new Object[] {this, data}));
		}
		return basicFactory.construct(cls, data);
	}

	public interface Index<T extends PkoObject<?,?>> {
		public Class<T> getRecordClass(); 
	}
	public interface UniqueIndex<T extends PkoObject<?,?>> extends Index<T >{
		public Schema.Field<?>[] fields();
		public T get(String ... field); 
	}
	public interface OrderedIndex<T extends PkoObject<?,?>> extends Index<T >, Iterable<T>{
		public Schema.Field<?>[] fields();
		public Iterable<T> tailList(String fromKey); 
		public Iterable<T> headList(String toKey);  
		public Iterable<T> subList(String fromKey,String toKey); 
	}
}
