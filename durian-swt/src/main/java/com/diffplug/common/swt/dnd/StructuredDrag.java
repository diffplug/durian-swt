/*
 * Copyright 2020 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.common.swt.dnd;


import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.Unhandled;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.common.rx.RxBox;
import com.diffplug.common.rx.RxGetter;
import com.diffplug.common.swt.SwtMisc;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;

/**
 * Typed mechanism for implementing drag listeners.
 * 
 * https://eclipse.org/articles/Article-SWT-DND/DND-in-SWT.html
 */
public class StructuredDrag {
	public enum DropResult {
		NO_DROP, COPIED, MOVED;
	}

	@FunctionalInterface
	public interface TypedDragHandler<T> {
		/** DragSourceEvent will be null if it's starting from a 'Ctrl+C' copy. */
		T dragStartData(@Nullable DragSourceEvent e);

		default void dropped(DragSourceEvent e, T value, DropResult result) {}

		default <R> TypedDragHandler<R> map(Function<T, R> mapper) {
			return new MappedTypedDragHandler<R, T>(this, mapper);
		}
	}

	private static class MappedTypedDragHandler<R, T> implements TypedDragHandler<R> {
		TypedDragHandler<T> delegate;
		Function<T, R> mapper;

		MappedTypedDragHandler(TypedDragHandler<T> delegate, Function<T, R> mapper) {
			this.delegate = Objects.requireNonNull(delegate);
			this.mapper = Objects.requireNonNull(mapper);
		}

		T lastOriginal;
		R lastMapped;
		int count = 0;

		@Override
		public R dragStartData(DragSourceEvent e) {
			T original = delegate.dragStartData(e);
			if (original == null) {
				lastOriginal = null;
				lastMapped = null;
				count = 0;
				e.doit = false;
			} else if (original.equals(lastOriginal)) {
				++count;
			} else {
				lastOriginal = original;
				lastMapped = mapper.apply(lastOriginal);
				count = 1;
			}
			return lastMapped;
		}

		@Override
		public void dropped(DragSourceEvent e, R value, DropResult moved) {
			Preconditions.checkArgument(value.equals(lastMapped), "dropped=%s lastMapped=%s", value, lastMapped);
			delegate.dropped(e, lastOriginal, moved);
			if (--count == 0) {
				lastOriginal = null;
				lastMapped = null;
			}
		}
	}

	@FunctionalInterface
	public interface TriConsumer<A, B, C> {
		void accept(A a, B b, C c);
	}

	private static class Handler {
		Predicate<Object> canSetValue;
		TriConsumer<Transfer, DragSourceEvent, Object> valueSetter;
		TypedDragHandler<Object> handler;

		@SuppressWarnings("unchecked")
		public <T> Handler(Predicate<T> canSetValue, TriConsumer<? extends Transfer, DragSourceEvent, T> valueSetter, TypedDragHandler<T> handler) {
			this.canSetValue = (Predicate<Object>) canSetValue;
			this.valueSetter = (TriConsumer<Transfer, DragSourceEvent, Object>) valueSetter;
			this.handler = (TypedDragHandler<Object>) handler;
		}
	}

	public class TypeMapper<T> {
		final TypedDragHandler<T> onEvent;

		private TypeMapper(TypedDragHandler<T> onEvent) {
			this.onEvent = onEvent;
		}

		private <R> TypeMapper<R> mapFromIfNotDuplicate(Transfer transfer, TypedDragHandler<R> dropHandler, Function<TypedDragHandler<R>, TypeMapper<R>> add) {
			if (hasHandlerFor(transfer)) {
				return new TypeMapper<R>(dropHandler);
			} else {
				return add.apply(dropHandler);
			}
		}

		public <R> TypeMapper<R> mapTo(TypedTransfer<R> transfer, Function<T, R> mapper) {
			return mapFromIfNotDuplicate(transfer, onEvent.map(mapper),
					mapped -> StructuredDrag.this.add(transfer, mapped));
		}

		public TypeMapper<String> mapToText(Function<T, String> mapper) {
			return mapFromIfNotDuplicate(TextTransfer.getInstance(), onEvent.map(mapper),
					mapped -> StructuredDrag.this.addText(mapped));
		}

		public TypeMapper<ImmutableList<File>> mapToFile(Function<T, ImmutableList<File>> mapper) {
			return mapFromIfNotDuplicate(FileTransfer.getInstance(), onEvent.map(mapper),
					mapped -> StructuredDrag.this.addFile(mapped));
		}
	}

	private ImmutableMap.Builder<Transfer, Handler> builder = ImmutableMap.builder();
	private Map<Transfer, Exception> addedAt = new HashMap<>();
	private Listener impl;
	private final RxBox<Boolean> dragInProgress = RxBox.of(false);

	public RxGetter<Boolean> dragInProgress() {
		return dragInProgress;
	}

	/** Returns true if it contains a handler for this transfer type. */
	public boolean hasHandlerFor(Transfer transfer) {
		if (builder != null) {
			return addedAt.containsKey(transfer);
		} else {
			return impl.handlers.containsKey(transfer);
		}
	}

	/** Adds a drag for the given transfer. */
	public <TValue, TTransfer extends Transfer> TypeMapper<TValue> add(TTransfer transfer, Predicate<TValue> canSetValue, TriConsumer<TTransfer, DragSourceEvent, TValue> valueSetter, TypedDragHandler<TValue> onEvent) {
		if (builder == null) {
			throw new IllegalStateException("Has already been applied.");
		}
		// check for duplicate entries
		Exception previous = addedAt.put(transfer, new IllegalArgumentException());
		if (previous != null) {
			throw new IllegalArgumentException("Duplicate for " + transfer, previous);
		}
		builder.put(transfer, new Handler(canSetValue, valueSetter, onEvent));
		return new TypeMapper<>(onEvent);
	}

	/** Adds a drop for the given filetype. */
	public <TValue> TypeMapper<TValue> add(TypedTransfer<TValue> transfer, TypedDragHandler<TValue> onEvent) {
		TypeMapper<TValue> mapper = add(transfer, transfer::canSetValue, TypedTransfer::setValue, onEvent);
		transfer.mapDrag(mapper);
		return mapper;
	}

	/** Adds the ability to drop text. */
	public TypeMapper<String> addText(TypedDragHandler<String> onEvent) {
		return add(TextTransfer.getInstance(), str -> !str.isEmpty(), (transfer, e, str) -> {
			e.data = str;
		}, onEvent);
	}

	/** Adds the ability to drop files. */
	public TypeMapper<ImmutableList<File>> addFile(TypedDragHandler<ImmutableList<File>> onEvent) {
		return add(FileTransfer.getInstance(), files -> files != null && !files.isEmpty(), (transfer, e, files) -> {
			if (!files.isEmpty()) {
				e.data = convertFilesToNative(files);
			}
		}, onEvent);
	}

	private static String[] convertFilesToNative(ImmutableList<File> files) {
		return files.stream().map(File::getAbsolutePath).toArray(String[]::new);
	}

	public Listener getListener() {
		if (impl == null) {
			impl = new Listener(dragInProgress, builder.build());
			builder = null;
			addedAt = null;
		}
		return impl;
	}

	public void applyTo(Control control, DndOp op) {
		applyTo(control, op.flag());
	}

	public void applyTo(Control control, DndOp opA, DndOp opB) {
		applyTo(control, opA.flag() | opB.flag());
	}

	private void applyTo(Control control, int styles) {
		DragSource dragSource = new DragSource(control, styles);
		dragSource.setTransfer(getListener().transferArray());
		dragSource.addDragListener(getListener());
	}

	public static class Listener implements DragSourceListener {
		final RxBox<Boolean> dragInProgress;
		final ImmutableMap<Transfer, Handler> handlers;
		final Transfer[] transfers;
		final Map<Transfer, Object> data = new IdentityHashMap<>();

		public Listener(RxBox<Boolean> dragInProgress, ImmutableMap<Transfer, Handler> map) {
			this.dragInProgress = dragInProgress;
			this.handlers = map;
			transfers = map.keySet().toArray(new Transfer[map.size()]);
		}

		@SuppressWarnings("unchecked")
		public void copyToClipboard() {
			Clipboard clipboard = new Clipboard(SwtMisc.assertUI());
			try {
				populateData(null);
				Object[] dataPer = new Object[data.size()];
				Transfer[] transferPer = new Transfer[data.size()];
				int i = 0;
				for (Map.Entry<Transfer, Object> entry : data.entrySet()) {
					transferPer[i] = entry.getKey();
					if (transferPer[i] instanceof FileTransfer) {
						dataPer[i] = convertFilesToNative((ImmutableList<File>) entry.getValue());
					} else {
						dataPer[i] = entry.getValue();
					}
					++i;
				}
				clipboard.setContents(dataPer, transferPer);
			} finally {
				clipboard.dispose();
			}
		}

		public Transfer[] transferArray() {
			return Arrays.copyOf(transfers, transfers.length);
		}

		private void populateData(@Nullable DragSourceEvent event) {
			data.clear();
			handlers.forEach((transfer, handler) -> {
				// get the drag data
				Object value = handler.handler.dragStartData(event);
				if (value != null && handler.canSetValue.test(value)) {
					data.put(transfer, value);
				}
			});
		}

		@Override
		public void dragStart(DragSourceEvent event) {
			dragInProgress.set(true);
			populateData(event);
			event.doit = data.size() > 0;
			if (event.doit) {
				Transfer[] validTransfers = data.keySet().stream().toArray(Transfer[]::new);
				((DragSource) event.getSource()).setTransfer(validTransfers);
			}
		}

		@Nullable
		private Transfer findTransfer(DragSourceEvent event) {
			// set event.data based on event.dataType
			for (Entry<Transfer, Object> entry : data.entrySet()) {
				Transfer transfer = entry.getKey();
				if (transfer.isSupportedType(event.dataType)) {
					return transfer;
				}
			}
			return null;
		}

		@Override
		public void dragSetData(DragSourceEvent event) {
			Transfer transfer = findTransfer(event);
			event.doit = transfer != null;
			if (event.doit) {
				Handler handler = handlers.get(transfer);
				Object value = data.get(transfer);
				handler.valueSetter.accept(transfer, event, value);
			}
		}

		@Override
		public void dragFinished(DragSourceEvent event) {
			Transfer selectedTransfer = findTransfer(event);
			data.forEach((transfer, value) -> {
				DropResult result;
				if (selectedTransfer == transfer) {
					// @formatter:off
					switch (event.detail) {
					case DND.DROP_MOVE: result = DropResult.MOVED;	break;
					case DND.DROP_COPY: result = DropResult.COPIED;	break;
					default: throw Unhandled.integerException(event.detail);
					}
					// @formatter:on
				} else {
					result = DropResult.NO_DROP;
				}
				handlers.get(transfer).handler.dropped(event, value, result);
			});
			data.clear();
			dragInProgress.set(false);
		}
	}
}
