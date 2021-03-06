package org.kisst.pko4j;

import java.lang.reflect.Constructor;

import org.kisst.item4j.Item;
import org.kisst.item4j.ReflectSchema;
import org.kisst.item4j.struct.Struct;
import org.kisst.util.ReflectionUtil;

public class PkoSchema<T extends PkoObject> extends ReflectSchema<T> {
	public PkoSchema(Class<T> cls) { 
		super(cls); 
	}

	public final IdField _id = new IdField();
	public final InstantField creationDate = new InstantField("creationDate");
	public final InstantField modificationDate = new InstantField("modificationDate");
	public final IntField pkoVersion = new IntField("pkoVersion");


	@SuppressWarnings("unchecked")
	public T createObject(PkoModel model, Struct doc, int version) { 
		Constructor<?> cons=ReflectionUtil.getConstructor(getJavaClass(), new Class<?>[]{ model.getClass(), Struct.class, int.class} );
		if (cons!=null)
			return (T) ReflectionUtil.createObject(cons, new Object[]{model, doc, version} );
		cons=ReflectionUtil.getConstructor(getJavaClass(), new Class<?>[]{ model.getClass(), Struct.class} );
		return (T) ReflectionUtil.createObject(cons, new Object[]{model, doc} );
	}

	//public final IdField _id = new IdField();
	
	//public IdField getKeyField() { return _id;}

	public int getCurrentVersion() { return 0; }
	public static class IdField extends BasicField<String> {
		public IdField() { super(String.class, "_id"); }
		public IdField(String name) { super(String.class, name); }
		public String getString(Struct data) { return Item.asString(data.getDirectFieldValue(name)); };
	}
}
