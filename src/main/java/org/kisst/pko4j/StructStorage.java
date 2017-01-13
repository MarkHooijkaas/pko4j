package org.kisst.pko4j;

import java.time.Instant;

import org.kisst.item4j.ImmutableSequence;
import org.kisst.item4j.seq.TypedSequence;

public interface StructStorage<T extends PkoObject> extends StorageOption<T> {
	public Class<T> getRecordClass();
	public void create(T value);
	public void update(T oldValue, T newValue);
	public void delete(T oldValue);

	public TypedSequence<T> findAll(PkoModel model);

	public String readBlob(String key, String path);
	public void writeBlob(String key, String path, String blob);
	public void appendBlob(String key, String path, String blob);
	public ImmutableSequence<HistoryItem> getHistory(String _id, String path);
	public ImmutableSequence<HistoryItem> getHistory(String _id);

	public void saveAll(Iterable<T> records);

	public static class HistoryItem {
		public final Instant time;
		public final String user;
		public final String email;
		public final String comment;
		public HistoryItem(Instant time, String user, String email, String comment) {
			this.time=time;
			this.user=user;
			this.email=email;
			this.comment=comment;
		}
	}
}
