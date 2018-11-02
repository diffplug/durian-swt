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

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.common.base.Converter;
import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.Throwing;
import com.diffplug.common.rx.Rx;
import com.diffplug.common.rx.RxBox;
import com.diffplug.common.swt.InteractiveTest;
import com.diffplug.common.swt.Layouts;
import com.diffplug.common.swt.SwtMisc;
import com.diffplug.common.swt.SwtRx;
import com.diffplug.common.swt.widgets.ScaleCtl;
import io.reactivex.Observable;

@Category(InteractiveTest.class)
public class StructuredDndMappingTest {
	interface Playground<T> {
		void putOn(Composite cmp, RxBox<T> box, StructuredDrag drag, StructuredDrop drop);
	}

	@Test
	public void dndPlayground() {
		InteractiveTest.testCoat("Drag numbers, text, and fractions around", 30, 15, rootCmp -> {
			Layouts.setFill(rootCmp);
			class Adder {
				<T> void addGroup(String name, T value, Playground<T> coat) {
					Group txtGrp = new Group(rootCmp, SWT.SHADOW_ETCHED_IN);
					txtGrp.setText(name);

					StructuredDrag drag = new StructuredDrag();
					StructuredDrop drop = new StructuredDrop();

					Layouts.setGrid(txtGrp);
					Composite control = new Composite(txtGrp, SWT.NONE);
					Layouts.setGridData(control).grabAll();

					RxBox<T> box = RxBox.of(value);
					coat.putOn(control, box, drag, drop);

					Text currentValue = new Text(txtGrp, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
					Layouts.setGridData(currentValue).grabHorizontal();
					Rx.subscribe(box, v -> currentValue.setText(v.toString()));

					Label dndCtl = new Label(txtGrp, SWT.NONE);
					dndCtl.setBackground(SwtMisc.getSystemColor(SWT.COLOR_WHITE));
					Layouts.setGridData(dndCtl).grabHorizontal();
					dndCtl.setText("Drag and drop");
					drag.applyTo(dndCtl, DndOp.COPY);
					drop.applyTo(dndCtl);

					Composite btnCmp = new Composite(txtGrp, SWT.NONE);
					Layouts.setGridData(btnCmp).grabHorizontal();
					Layouts.setGrid(btnCmp).margin(0).numColumns(2);
					BiConsumer<String, Runnable> addBtn = (lbl, toRun) -> {
						Button btn = new Button(btnCmp, SWT.PUSH | SWT.FLAT);
						Layouts.setGridData(btn).grabHorizontal();
						btn.setText(lbl);
						btn.addListener(SWT.Selection, e -> toRun.run());
					};
					addBtn.accept("Copy", () -> drag.getListener().copyToClipboard());
					addBtn.accept("Paste", () -> drop.getListener().pasteFromClipboard());
				}
			}
			Adder adder = new Adder();

			adder.addGroup("Text", "words", (cmp, box, drag, drop) -> {
				Layouts.setGrid(cmp);
				Text txt = new Text(cmp, SWT.SINGLE | SWT.BORDER);
				Layouts.setGridData(txt).grabHorizontal();

				Rx.sync(box, SwtRx.textImmediate(txt));

				drag.addText(e -> box.get());
				drop.addText(StructuredDrop.handler(DndOp.COPY, box::set));
			});
			adder.addGroup("Double", 50.0, (cmp, box, drag, drop) -> {
				Layouts.setFill(cmp);
				ScaleCtl.builder()
						.rangeArithmetic(0, 100)
						.build(cmp, box);

				drag.add(DoubleTransfer.INSTANCE, e -> box.get());
				drop.add(DoubleTransfer.INSTANCE, StructuredDrop.handler(DndOp.COPY, box::set));
			});
			adder.addGroup("Fraction", Fraction.create(1, 2), (cmp, box, drag, drop) -> {
				Layouts.setGrid(cmp);
				Text numeratorTxt = new Text(cmp, SWT.SINGLE | SWT.BORDER);
				Layouts.setGridData(numeratorTxt).grabHorizontal();
				Text denominatorTxt = new Text(cmp, SWT.SINGLE | SWT.BORDER);
				Layouts.setGridData(denominatorTxt).grabHorizontal();

				Converter<String, Integer> converter = Converter.from(
						Errors.suppress().wrapFunctionWithDefault(Integer::parseInt, 0),
						Object::toString);
				RxBox<Integer> num = SwtRx.textImmediate(numeratorTxt).map(converter);
				RxBox<Integer> den = SwtRx.textImmediate(denominatorTxt).map(converter);

				Rx.subscribe(box, d -> {
					num.set(d.numerator());
					den.set(d.denominator());
				});

				Observable<Fraction> rxFraction = Observable.combineLatest(num, den, Fraction::create);
				Rx.subscribe(rxFraction, box::set);

				drag.add(FractionTransfer.INSTANCE, e -> box.get());
				drop.add(FractionTransfer.INSTANCE, StructuredDrop.handler(DndOp.COPY, box::set));
			});
		});
	}

	public static class DoubleTransfer extends CustomLocalTransfer<Double> {
		private DoubleTransfer() {}

		static final DoubleTransfer INSTANCE = new DoubleTransfer();

		@Override
		protected boolean canSetValueNonnull(Double value) {
			return true;
		}

		@Override
		protected void mapDrag(StructuredDrag.TypeMapper<Double> typeMapper) {
			typeMapper.mapToText(safe(Object::toString));
		}

		@Override
		protected void mapDrop(StructuredDrop.TypeMapper<Double> typeMapper) {
			typeMapper.mapFromText(safe(Double::parseDouble));
			typeMapper.mapFrom(FractionTransfer.INSTANCE, safe(Fraction::toDouble));
		}
	}

	public static class Fraction {
		final int numerator, denominator;

		private Fraction(int numerator, int denominator) {
			this.numerator = numerator;
			this.denominator = denominator;
		}

		public int numerator() {
			return numerator;
		}

		public int denominator() {
			return denominator;
		}

		public double toDouble() {
			return ((double) numerator()) / denominator();
		}

		public static Fraction create(int numerator, int denominator) {
			return new Fraction(numerator, denominator);
		}

		public static Fraction parse(String txt) {
			String[] pieces = txt.split("/", -1);
			Preconditions.checkArgument(pieces.length == 2);
			int numerator = Integer.parseInt(pieces[0]);
			int denominator = Integer.parseInt(pieces[1]);
			return create(numerator, denominator);
		}

		@Override
		public int hashCode() {
			return 101 * denominator + numerator;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof Fraction) {
				Fraction other = (Fraction) obj;
				return other.numerator == numerator && other.denominator == denominator;
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return numerator() + "/" + denominator();
		}
	}

	@Test
	public void testFractionParse() {
		Converter<Fraction, String> converter = Converter.from(Fraction::toString, Fraction::parse);
		Consumer<Fraction> test = value -> {
			String encoded = converter.convert(value);
			Fraction decoded = converter.reverse().convert(encoded);
			String reEncoded = converter.convert(decoded);
			Assert.assertEquals(encoded, reEncoded);
		};
		test.accept(Fraction.create(0, 0));
		test.accept(Fraction.create(1, 0));
		test.accept(Fraction.create(0, 1));
		test.accept(Fraction.create(1, 1));
	}

	public static class FractionTransfer extends CustomLocalTransfer<Fraction> {
		private FractionTransfer() {}

		static final FractionTransfer INSTANCE = new FractionTransfer();

		@Override
		protected boolean canSetValueNonnull(Fraction value) {
			return true;
		}

		@Override
		protected void mapDrag(StructuredDrag.TypeMapper<Fraction> typeMapper) {
			typeMapper.mapToText(safe(Fraction::toString));
			typeMapper.mapTo(DoubleTransfer.INSTANCE, safe(Fraction::toDouble));
		}

		@Override
		protected void mapDrop(StructuredDrop.TypeMapper<Fraction> typeMapper) {
			typeMapper.mapFromText(safe(Fraction::parse));
		}
	}

	static <T, R> Function<T, R> safe(Throwing.Function<T, R> function) {
		return Errors.suppress().wrapFunctionWithDefault(function, null);
	}
}
