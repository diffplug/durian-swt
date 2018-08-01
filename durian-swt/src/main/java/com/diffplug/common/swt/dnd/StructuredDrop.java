/*
 * Copyright 2018 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.common.swt.dnd;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Control;

import com.diffplug.common.base.Predicates;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.collect.ImmutableMap;

/**
 * Typed mechanism for implementing drop listeners.
 * 
 * https://eclipse.org/articles/Article-SWT-DND/DND-in-SWT.html
 */
public class StructuredDrop {
	public enum DropMethod {
		dragEnter, dragLeave, dragOperationChanged, dragOver, drop, dropAccept
	}

	@FunctionalInterface
	public interface TypedDropHandler<T> {
		void onEvent(DropMethod method, DropTargetEvent e, T value);

		default <R> TypedDropHandler<R> map(Function<? super R, ? extends T> mapper) {
			return new MappedTypedDropHandler<R, T>(this, mapper);
		}
	}

	private static class MappedTypedDropHandler<R, T> implements TypedDropHandler<R> {
		TypedDropHandler<T> delegate;
		Function<? super R, ? extends T> mapper;

		MappedTypedDropHandler(TypedDropHandler<T> delegate, Function<? super R, ? extends T> mapper) {
			this.delegate = Objects.requireNonNull(delegate);
			this.mapper = Objects.requireNonNull(mapper);
		}

		@Override
		public void onEvent(DropMethod method, DropTargetEvent e, R value) {
			T mapped = mapper.apply(value);
			delegate.onEvent(method, e, mapped);
		}
	}

	private static class Handler {
		BiFunction<Object, Object, Object> valueGetter;
		TypedDropHandler<Object> handler;

		@SuppressWarnings("unchecked")
		public Handler(BiFunction<?, ?, ?> valueGetter, TypedDropHandler<?> handler) {
			this.valueGetter = (BiFunction<Object, Object, Object>) valueGetter;
			this.handler = (TypedDropHandler<Object>) handler;
		}
	}

	public class TypeMapper<T> {
		final TypedDropHandler<T> onEvent;

		private TypeMapper(TypedDropHandler<T> onEvent) {
			this.onEvent = onEvent;
		}

		private <R> TypeMapper<R> mapFromIfNotDuplicate(Transfer transfer, TypedDropHandler<R> dropHandler, Function<TypedDropHandler<R>, TypeMapper<R>> add) {
			if (hasHandlerFor(transfer)) {
				return new TypeMapper<R>(dropHandler);
			} else {
				return add.apply(dropHandler);
			}
		}

		public <R> TypeMapper<R> mapFrom(TypedTransfer<R> transfer, Function<? super R, ? extends T> mapper) {
			return mapFromIfNotDuplicate(transfer, onEvent.map(mapper),
					mapped -> StructuredDrop.this.add(transfer, mapped));
		}

		public TypeMapper<String> mapFromText(Function<String, ? extends T> mapper) {
			return mapFromIfNotDuplicate(TextTransfer.getInstance(), onEvent.map(mapper),
					mapped -> StructuredDrop.this.addText(mapped));
		}

		public TypeMapper<ImmutableList<File>> mapFromFile(Function<ImmutableList<File>, ? extends T> mapper) {
			return mapFromIfNotDuplicate(FileTransfer.getInstance(), onEvent.map(mapper),
					mapped -> StructuredDrop.this.addFile(mapped));
		}
	}

	private ImmutableMap.Builder<Transfer, Handler> builder = ImmutableMap.builder();
	private Map<Transfer, Exception> addedAt = new HashMap<>();
	private Listener impl;

	/** Returns true if it contains a handler for this transfer type. */
	public boolean hasHandlerFor(Transfer transfer) {
		if (builder != null) {
			return addedAt.containsKey(transfer);
		} else {
			return impl.handlers.containsKey(transfer);
		}
	}

	/** Adds a drop for the given transfer. */
	public <TValue, TTransfer extends Transfer> TypeMapper<TValue> add(TTransfer transfer, BiFunction<TTransfer, DropTargetEvent, TValue> valueGetter, TypedDropHandler<TValue> onEvent) {
		if (builder == null) {
			throw new IllegalStateException("Can't add new transfers after calling 'applyTo' or 'getListener'");
		}
		// check for duplicate entries 
		Exception previous = addedAt.put(transfer, new IllegalArgumentException());
		if (previous != null) {
			throw new IllegalArgumentException("Duplicate for " + transfer, previous);
		}
		// do the actual work 
		builder.put(transfer, new Handler(valueGetter, onEvent));
		return new TypeMapper<>(onEvent);
	}

	/** Adds a drop for the given filetype. */
	public <TValue> TypeMapper<TValue> add(TypedTransfer<TValue> transfer, TypedDropHandler<TValue> onEvent) {
		TypeMapper<TValue> typeMapper = add(transfer, TypedTransfer::getValue, onEvent);
		transfer.mapDrop(typeMapper);
		return typeMapper;
	}

	/** Adds the ability to drop text. */
	public TypeMapper<String> addText(TypedDropHandler<String> onEvent) {
		return add(TextTransfer.getInstance(), (transfer, e) -> (String) e.data, onEvent);
	}

	/** Adds the ability to drop files. */
	public TypeMapper<ImmutableList<File>> addFile(TypedDropHandler<ImmutableList<File>> onEvent) {
		return add(FileTransfer.getInstance(), (transfer, e) -> {
			if (e.data == null) {
				return null;
			}
			String[] paths = (String[]) e.data;
			List<File> files = new ArrayList<>(paths.length);
			for (int i = 0; i < paths.length; ++i) {
				files.add(new File(paths[i]));
			}
			return ImmutableList.copyOf(files);
		}, onEvent);
	}

	public Listener getListener() {
		if (impl == null) {
			impl = new Listener(builder.build());
			builder = null;
			addedAt = null;
		}
		return impl;
	}

	public void applyTo(Control control) {
		DropTarget dropTarget = new DropTarget(control, DndOp.dropAll());
		dropTarget.setTransfer(getListener().transferArray());
		dropTarget.addDropListener(getListener());
	}

	public void applyTo(Control... controls) {
		for (Control control : controls) {
			applyTo(control);
		}
	}

	/** Sets event.currentDataType to the most preferred possible. Returns the Transfer that was actually used. */
	public static Transfer preferDropTransfer(DropTargetEvent event, Transfer[] preferreds) {
		if (event.dataTypes == null) {
			return null;
		}
		for (Transfer transfer : preferreds) {
			// for each transfer
			for (TransferData data : event.dataTypes) {
				// find a supported TransferData
				if (transfer.isSupportedType(data)) {
					// set currentDataType and return the Transfer if we're successful
					event.currentDataType = data;
					return transfer;
				}
			}
		}

		// return nulls if we failed
		event.currentDataType = null;
		return null;
	}

	public static class Listener implements DropTargetListener {
		final ImmutableMap<Transfer, Handler> handlers;
		final Transfer[] transfers;

		public Listener(ImmutableMap<Transfer, Handler> map) {
			this.handlers = map;
			transfers = map.keySet().toArray(new Transfer[map.size()]);
		}

		public Transfer[] transferArray() {
			return Arrays.copyOf(transfers, transfers.length);
		}

		@Override
		public void dragEnter(DropTargetEvent event) {
			onEvent(DropMethod.dragEnter, event);
		}

		@Override
		public void dragOver(DropTargetEvent event) {
			onEvent(DropMethod.dragOver, event);
		}

		@Override
		public void dragOperationChanged(DropTargetEvent event) {
			onEvent(DropMethod.dragOperationChanged, event);
		}

		@Override
		public void dropAccept(DropTargetEvent event) {
			onEvent(DropMethod.dropAccept, event);
		}

		@Override
		public void dragLeave(DropTargetEvent event) {
			onEvent(DropMethod.dragLeave, event);
		}

		@Override
		public void drop(DropTargetEvent event) {
			onEvent(DropMethod.drop, event);
		}

		private void onEvent(DropMethod method, DropTargetEvent event) {
			Transfer preferred = StructuredDrop.preferDropTransfer(event, transfers);
			if (preferred != null) {
				Handler handler = handlers.get(preferred);
				Object value = handler.valueGetter.apply(preferred, event);
				handler.handler.onEvent(method, event, value);
			} else {
				event.detail = DND.DROP_NONE;
			}
		}
	}

	public abstract static class AbstractTypedDropHandler<T> implements TypedDropHandler<T> {
		protected final DndOp operation;

		public AbstractTypedDropHandler(DndOp operation) {
			this.operation = Objects.requireNonNull(operation);
		}

		@Override
		public void onEvent(DropMethod method, DropTargetEvent e, T value) {
			if (method == DropMethod.dragLeave) {
				return;
			} else {
				if (value == null) {
					// this means that we don't know what the data is yet
					operation.trySetDetail(e);
				} else if (accept(value)) {
					if (operation.trySetDetail(e) && method == DropMethod.drop) {
						drop(e, value, e.detail == DND.DROP_MOVE);
					}
				}
			}
		}

		protected abstract boolean accept(T value);

		protected abstract void drop(DropTargetEvent event, T value, boolean moved);
	}

	/** Creates a TypedDropHandler for the given type. */
	public static <T> TypedDropHandler<T> handler(DndOp op, Predicate<T> predicate, Consumer<T> onDrop) {
		return new AbstractTypedDropHandler<T>(op) {
			@Override
			protected boolean accept(T value) {
				return predicate.test(value);
			}

			@Override
			protected void drop(DropTargetEvent event, T value, boolean moved) {
				onDrop.accept(value);
			}
		};
	}

	/** Creates a TypedDropHandler for the given type. */
	public static <T> TypedDropHandler<T> handler(DndOp op, Consumer<T> onDrop) {
		return handler(op, Predicates.alwaysTrue(), onDrop);
	}
}
