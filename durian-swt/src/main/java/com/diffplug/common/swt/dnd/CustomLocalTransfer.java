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


import com.diffplug.common.base.Box;
import com.diffplug.common.base.Preconditions;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;

/** Custom implementation of a local transfer class. The "data" object will be this Transfer itself. */
public abstract class CustomLocalTransfer<T> extends TypedTransfer<T> implements Box<T> {
	// create a custom type name
	private final String TYPE_NAME = getClass().getCanonicalName() + System.currentTimeMillis();
	private final int TYPE_ID = registerType(TYPE_NAME);

	protected CustomLocalTransfer() {}

	@Override
	protected int[] getTypeIds() {
		return new int[]{TYPE_ID};
	}

	@Override
	protected String[] getTypeNames() {
		return new String[]{TYPE_NAME};
	}

	@SuppressWarnings("unchecked")
	@Override
	public void javaToNative(Object object, TransferData transferData) {
		if (object != null) {
			set((T) object);
		}
		byte[] check = TYPE_NAME.getBytes(StandardCharsets.UTF_8);
		super.javaToNative(check, transferData);
	}

	@Override
	public Object nativeToJava(TransferData transferData) {
		if (obj == null) {
			return null;
		} else {
			// check result
			Object result = super.nativeToJava(transferData);
			Preconditions.checkArgument(result instanceof byte[], "%s should have been byte[]", result.getClass());
			String resultStr = new String((byte[]) result, StandardCharsets.UTF_8);
			Preconditions.checkArgument(TYPE_NAME.equals(resultStr), "%s should have been %s", resultStr, TYPE_NAME);

			// return this transfer object itself
			return get();
		}
	}

	@Override
	protected final boolean canSetValue(@javax.annotation.Nullable T value) {
		if (value != null && canSetValueNonnull(value)) {
			set(value);
			return true;
		} else {
			obj = null;
			return false;
		}
	}

	/** Returns the underlying value. */
	@Override
	public T getValue(DropTargetEvent e) {
		return get();
	}

	/** Sets the underlying value. */
	@Override
	public void setValue(DragSourceEvent e, T value) {
		set(value);
	}

	private T obj;

	@Override
	public final void set(T value) {
		this.obj = Objects.requireNonNull(value);
	}

	@Override
	public final T get() {
		return Objects.requireNonNull(obj);
	}
}
