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
package com.diffplug.common.swt.widgets;


import com.diffplug.common.base.Preconditions;
import com.diffplug.common.rx.RxBox;
import com.diffplug.common.swt.ControlWrapper;
import com.diffplug.common.swt.Layouts;
import com.diffplug.common.swt.SwtExec;
import com.diffplug.common.swt.SwtMisc;
import java.text.DecimalFormat;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;

public class ScaleCtl extends ControlWrapper.AroundControl<Composite> {
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String nameLbl, unitsLbl;
		private Mapping mapping;
		private int digitsAfterDecimatPoint;
		private boolean isCompact;

		public Builder name(String nameLbl) {
			this.nameLbl = nameLbl;
			return this;
		}

		public Builder units(String unitsLbl) {
			this.unitsLbl = unitsLbl;
			return this;
		}

		public Builder rangeArithmetic(double min, double max) {
			this.mapping = new ScaleArithmetic(min, max);
			return this;
		}

		public Builder rangeGeometric(double min, double max) {
			this.mapping = new ScaleGeometric(min, max);
			return this;
		}

		public Builder digitsAfterDecimatPoint(int digits) {
			this.digitsAfterDecimatPoint = digits;
			return this;
		}

		public Builder isCompact(boolean isCompact) {
			this.isCompact = isCompact;
			return this;
		}

		public ScaleCtl build(Composite parent, RxBox<Double> box) {
			Preconditions.checkState(mapping != null, "Mapping must exist");
			return new ScaleCtl(parent, this, box);
		}
	}

	static class DecimalPointFormatter {
		private DecimalFormat format;

		public DecimalPointFormatter(int digitsAfterDecimal) {
			if (digitsAfterDecimal == 0) {
				format = new DecimalFormat("0");
			} else {
				StringBuilder builder = new StringBuilder();
				builder.append("0.");
				for (int i = 0; i < digitsAfterDecimal; ++i) {
					builder.append('0');
				}
				format = new DecimalFormat(builder.toString());
			}
		}

		public String formatDouble(double val) {
			return format.format(val);
		}
	}

	private final Scale scale;
	private final Text text;

	private static final int SCALE_MAX = 1000;

	private final DecimalPointFormatter format;
	private final Mapping mapping;

	public ScaleCtl(Composite parent, Builder builder, RxBox<Double> box) {
		super(new Composite(parent, SWT.NONE));
		this.mapping = builder.mapping;

		format = new DecimalPointFormatter(builder.digitsAfterDecimatPoint);
		int numColumns = builder.isCompact ? 6 : 3;
		if (builder.nameLbl == null) {
			--numColumns;
		}
		if (builder.unitsLbl == null) {
			--numColumns;
		}
		Layouts.setGrid(wrapped).numColumns(numColumns).margin(0);

		scale = new Scale(wrapped, SWT.HORIZONTAL);
		scale.setMinimum(0);
		scale.setMaximum(SCALE_MAX);
		Layouts.setGridData(scale).grabHorizontal().horizontalSpan(3);
		scale.addListener(SWT.Selection, event -> {
			double value = mapping.scaleToValue(scale.getSelection());
			box.set(value);
		});

		if (builder.nameLbl != null) {
			Label nameLbl = new Label(wrapped, SWT.NONE);
			nameLbl.setText(builder.nameLbl);
			Layouts.setGridData(nameLbl).grabHorizontal();
		}

		text = new Text(wrapped, SWT.BORDER | SWT.SINGLE);
		Layouts.setGridData(text).grabHorizontal().widthHint(SwtMisc.systemFontWidth() * 4);

		SwtExec.async().guardOn(text).execute(text::selectAll);
		Listener selectAllListener = SwtMisc.asListener(SwtExec.async().guardOn(text).wrap(text::selectAll));
		text.addListener(SWT.DefaultSelection, event -> {
			try {
				double value = Double.parseDouble(text.getText());
				value = Math.min(value, mapping.maxValue);
				value = Math.max(value, mapping.minValue);
				box.set(value);
			} catch (Exception e) {}
			selectAllListener.handleEvent(event);
		});
		text.addListener(SWT.FocusIn, selectAllListener);

		if (builder.unitsLbl != null) {
			Label unitsLbl = new Label(wrapped, SWT.NONE);
			unitsLbl.setText(builder.unitsLbl);
		}

		SwtExec.immediate().guardOn(this).subscribe(box, value -> {
			text.setText(format.formatDouble(value));
			scale.setSelection(mapping.valueToScale(value));
		});
	}

	public void setEnabled(boolean enabled) {
		scale.setEnabled(enabled);
		text.setEnabled(enabled);
	}

	private static abstract class Mapping {
		protected final double minValue, maxValue;

		Mapping(double minValue, double maxValue) {
			this.minValue = minValue;
			this.maxValue = maxValue;
		}

		abstract int valueToScale(double value);

		abstract double scaleToValue(int scale);
	}

	private static class ScaleArithmetic extends Mapping {
		ScaleArithmetic(double minValue, double maxValue) {
			super(minValue, maxValue);
		}

		@Override
		public int valueToScale(double value) {
			return (int) Math.round(SCALE_MAX * (value - minValue) / (maxValue - minValue));
		}

		@Override
		public double scaleToValue(int scale) {
			return minValue + (maxValue - minValue) * scale / SCALE_MAX;
		}
	}

	private static class ScaleGeometric extends Mapping {
		final double geometricBase;

		ScaleGeometric(double minValue, double maxValue) {
			super(minValue, maxValue);
			Preconditions.checkArgument(!isZero(minValue), "minValue can't be 0");
			Preconditions.checkArgument(!isZero(maxValue), "maxValue can't be 0");
			Preconditions.checkArgument(
					(minValue > 0 && maxValue > 0) || (minValue < 0 && maxValue < 0),
					"minValue and maxValue must be both positive or both negative");
			geometricBase = Math.pow(maxValue / minValue, 1.0 / SCALE_MAX);
		}

		@Override
		public int valueToScale(double value) {
			if (isZero(value)) {
				return Integer.MAX_VALUE;
			} else {
				return (int) Math.round(Math.log(value / minValue) / Math.log(geometricBase));
			}
		}

		@Override
		public double scaleToValue(int scale) {
			return minValue * Math.pow(geometricBase, scale);
		}
	}

	static boolean isZero(double value) {
		return value == 0.0;
	}
}
