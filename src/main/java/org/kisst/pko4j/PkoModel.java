package org.kisst.pko4j;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import org.kisst.item4j.Item;
import org.kisst.item4j.struct.Struct;
import org.kisst.pko4j.PkoTable.ChangeHandler;
import org.kisst.util.ReflectionUtil;

public abstract class PkoModel implements Item.Factory {
	public interface MyObject {}

	
	private final StorageOption<?>[] options;

	public PkoModel() { this(new StorageOption[0]); }
	public PkoModel(StorageOption<?>[] options) {
		this.options=options;
	}

	public void initModel() {
		for (PkoTable<?> table: ReflectionUtil.getAllDeclaredFieldValuesOfType(this, PkoTable.class))
			table.loadFromStorage();
	}

	@SuppressWarnings("unchecked")
	public <RT extends PkoObject> StructStorage<RT> getStorage(Class<RT> cls) {
		for (StorageOption<?> opt: options) {
			if (opt instanceof StructStorage && opt.getRecordClass()==cls)
				return (StructStorage<RT>) opt;
		}
		throw new RuntimeException("Unknown Storage for type "+cls.getSimpleName());
	}
	@SuppressWarnings("unchecked")
	public<T extends PkoObject> ChangeHandler<T>[] getIndices(Class<?> cls) {
		ArrayList<ChangeHandler<T>> result=new ArrayList<ChangeHandler<T>>();
		for (Object opt: ReflectionUtil.getAllDeclaredFieldValuesOfType(this, Index.class)) {
			if (opt instanceof Index && ((Index<?>) opt).getRecordClass()==cls) {
				result.add((ChangeHandler<T>) opt);
			}
		}
		ChangeHandler<T>[] arr=new ChangeHandler[result.size()];
		for (int i=0; i<result.size(); i++)
			arr[i]=result.get(i);
		return arr;
	}

	@Override public Object construct(Class<?> cls, Object data) {
		if (MyObject.class.isAssignableFrom(cls)) {
			//System.out.println("Trying to construct "+cls.getName());
			Constructor<?> cons=ReflectionUtil.getConstructor(cls, new Class<?>[]{ this.getClass(), Struct.class} );
			if (cons!=null)
				return ReflectionUtil.createObject(cons, new Object[] {this, data});
		}
		if (PkoRef.class.isAssignableFrom(cls)) {
			Object result=ReflectionUtil.invoke(cls, null, "of", new Object[]{ this, data} );
			return result;
		}
		return basicFactory.construct(cls, data);
	}

	public interface Index<T extends PkoObject> {
		public Class<T> getRecordClass(); 
	}
}
