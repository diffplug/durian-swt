/*
 * Copyright 2016 DiffPlug
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

import javax.annotation.Nullable;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetEvent;

/** A strongly-typed custom transfer type. */
public abstract class TypedTransfer<T> extends ByteArrayTransfer {
	public abstract T getValue(DropTargetEvent e);

	public abstract void setValue(DragSourceEvent e, T value);

	protected boolean canSetValue(@Nullable T value) {
		return value == null ? false : canSetValueNonnull(value);
	}

	protected abstract boolean canSetValueNonnull(T value);

	/**
	 * Called after every call to {@link StructuredDrag#add(TypedTransfer, com.diffplug.common.swt.dnd.StructuredDrag.TypedDragHandler)}
	 * and {@link StructuredDrag.TypeMapper#mapTo(TypedTransfer, com.diffplug.common.base.Converter)}.
	 */
	protected void mapDrag(StructuredDrag.TypeMapper<T> typeMapper) {}

	/**
	 * Called after every call to {@link StructuredDrop#add(TypedTransfer, com.diffplug.common.swt.dnd.StructuredDrop.TypedDropHandler)}
	 * and {@link StructuredDrop.TypeMapper#mapFrom(TypedTransfer, java.util.function.Function)}
	 */
	protected void mapDrop(StructuredDrop.TypeMapper<T> typeMapper) {}
}
