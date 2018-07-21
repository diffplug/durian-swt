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
package com.diffplug.common.swt.widgets;

import java.util.function.DoubleConsumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;

/**
 * A table which can be scrolled past its limits (positive and negative).
 * Support per-pixel scrolling on all platforms.
 */
abstract class AbstractSmoothTable extends Composite {
	protected final Table table;
	protected final int rowHeight;
	private final int scrollBarWidth;
	protected int offset;

	protected double topRow;
	protected int topPixel;

	private int width;
	protected int height;
	private int tableHeight;
	private int itemCount;

	static final DoubleConsumer DO_NOTHING = value -> {};
	private DoubleConsumer topIndexListener = DO_NOTHING;
	private DoubleConsumer numVisibleListener = DO_NOTHING;

	protected final boolean extraRow;
	protected final boolean hasVScroll;

	/** Stuff for things. */
	AbstractSmoothTable(Composite parent, int style) {
		// if there's a BORDER, apply it to the Composite instead
		super(parent, (style & SWT.V_SCROLL) | (style & SWT.BORDER));
		if ((style & SWT.H_SCROLL) == SWT.H_SCROLL) {
			throw new IllegalArgumentException("Must not set H_SCROLL");
		}
		// the table can't have the BORDER
		table = new Table(this, style & (~SWT.BORDER));
		this.setBackground(table.getBackground());
		rowHeight = table.getItemHeight();
		itemCount = table.getItemCount();

		hasVScroll = (SWT.V_SCROLL & style) == SWT.V_SCROLL;
		if (hasVScroll) {
			int scrollBarWidth = getVerticalBar().getSize().x;
			if (scrollBarWidth == 0) {
				this.scrollBarWidth = MAC_OVERLAY_SCROLLBAR_WIDTH;
			} else {
				this.scrollBarWidth = scrollBarWidth;
			}
			// 	setup the vertical scroll bar
			getVerticalBar().setVisible(true);
			table.addListener(SWT.MouseVerticalWheel, e -> {
				setTopPixelSaturated(topPixel - e.count * rowHeight);
				e.doit = false;
			});
			getVerticalBar().addListener(SWT.Selection, e -> {
				setTopPixelSaturated(getVerticalBar().getSelection() + minTopPixel);
			});
		} else {
			this.scrollBarWidth = 0;
		}
		// if we're Unscrollable, then we'll need an extra row
		this.extraRow = (this instanceof Unscrollable);
		// track our size
		this.addListener(SWT.Resize, e -> {
			Rectangle clientArea = this.getClientArea();
			width = clientArea.width + scrollBarWidth;
			if (height != clientArea.height) {
				height = clientArea.height;
				tableHeight = height + (extraRow ? rowHeight : 0);
				numVisibleListener.accept(height / ((double) rowHeight));
			}
			readjustScrollBar();
		});
		// set the table's size when we get laidout
		super.setLayout(new Layout() {
			@Override
			protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
				return table.computeSize(wHint, hHint);
			}

			@Override
			protected void layout(Composite composite, boolean flushCache) {
				table.setBounds(0, offset, width, tableHeight);
				if (layout != null) {
					layout.layout(composite, flushCache);
				}
			}
		});
	}

	private static final int MAC_OVERLAY_SCROLLBAR_WIDTH = 17;

	/** Returns the height of rows. */
	public int getRowHeight() {
		return rowHeight;
	}

	private LayoutableLayout layout;

	@Deprecated
	@Override
	public void setLayout(Layout layout) {
		throw new UnsupportedOperationException("You can only use a LayoutableLayout!");
	}

	public void setLayout(LayoutableLayout layout) {
		this.layout = layout;
	}

	/** Sets the vertical offset of the table. */
	protected void setOffset(int offset) {
		// save the double so that getOffset() is exact
		if (this.offset != offset) {
			this.offset = offset;
			table.setLocation(0, offset);
		}
	}

	/** Sets the item count. */
	public void setItemCount(int items) {
		table.setItemCount(items);
		itemCount = items;
		readjustScrollBar();
	}

	private int minTopPixel, maxTopPixel;

	private void readjustScrollBar() {
		int increment = height;
		int pageIncrement = height;

		int thumb = height;
		int selection = round(topRow * rowHeight);
		int minimum = 0;
		int maximum = itemCount * rowHeight;

		minTopPixel = 0;
		maxTopPixel = maximum - height;
		if (selection < 0) {
			// we've scrolled into the negatives
			minTopPixel = selection;
			if (maximum < height) {
				maximum = height - selection;
			} else {
				maximum -= selection;
			}
			selection = 0;
		} else if (maximum < height) {
			// we've scrolled lower than is now possible
			maximum = height + selection;
			maxTopPixel = selection;
		}
		maxTopPixel = Math.max(0, maxTopPixel);
		if (hasVScroll) {
			getVerticalBar().setValues(selection, minimum, maximum, thumb, increment, pageIncrement);
		}
	}

	/** Returns the maximum top row which wouldn't cause us to scroll past the end. */
	public double getMaxTopRow() {
		double maxTopPixel = Math.max(0, itemCount * rowHeight - height);
		return maxTopPixel / rowHeight;
	}

	private void setTopPixelSaturated(int topPixel) {
		double topPixelSat = Math.min(maxTopPixel, Math.max(minTopPixel, topPixel));
		setTopRow(topPixelSat / rowHeight);
	}

	/** Returns the top row. */
	public double getTopRow() {
		return topRow;
	}

	/** Sets the top row. */
	public void setTopRow(double topRow) {
		this.topRow = topRow;
		this.topPixel = round(topRow * rowHeight);
		readjustScrollBar();
		setTopRowImpl();
		topIndexListener.accept(topRow);
	}

	/** Marks the given row as requiring redraw. */
	public void redrawRow(int row) {
		if (row < itemCount) {
			boolean redrawChildren = true;
			Rectangle itemBounds = table.getItem(row).getBounds();
			table.redraw(0, itemBounds.y, width, rowHeight, redrawChildren);
		}
	}

	/** Sets the listener which will be called when the number of visible rows is changed (can only be set once). */
	public void setListenerNumVisible(DoubleConsumer numVisibleListener) {
		if (this.numVisibleListener != DO_NOTHING) {
			throw new IllegalArgumentException("Can't call twice!");
		}
		this.numVisibleListener = numVisibleListener;
	}

	/** Sets the listener which will be called when the top row is changed (can only be set once). */
	public void setListenerTopIndex(DoubleConsumer topIndexListener) {
		if (this.topIndexListener != DO_NOTHING) {
			throw new IllegalArgumentException("Can't call twice!");
		}
		this.topIndexListener = topIndexListener;
	}

	/** Sets the fractional top index. */
	protected abstract void setTopRowImpl();

	static int round(double value) {
		return (int) Math.round(value);
	}

	/** Returns the underlying table. */
	public Table getTable() {
		return table;
	}

	/** An implementation of {@link AbstractSmoothTable} which works for platforms which don't support per-pixel scrolling. */
	static class Unscrollable extends AbstractSmoothTable {
		Unscrollable(Composite parent, int style) {
			super(parent, style);
		}

		@Override
		protected void setTopRowImpl() {
			int offset, topIndex;
			if (topPixel < 0) {
				offset = -topPixel;
				topIndex = 0;
			} else {
				topIndex = Math.floorDiv(topPixel, rowHeight);
				int maxTopIndex = table.getItemCount() - (height / rowHeight) - 1;
				if (topIndex > maxTopIndex) {
					topIndex = Math.max(0, maxTopIndex);
					offset = (topIndex * rowHeight) - topPixel;
				} else {
					offset = -Math.floorMod(topPixel, rowHeight);
				}
			}

			if (this.offset != offset) {
				setRedraw(false);
				table.setTopIndex(topIndex);
				setOffset(offset);
				setRedraw(true);
			} else {
				table.setTopIndex(topIndex);
			}
		}
	}

	/** An implementation of {@link AbstractSmoothTable} which works for platforms which do support per-pixel scrolling. */
	static abstract class Scrollable extends AbstractSmoothTable {
		Scrollable(Composite parent, int style) {
			super(parent, style);
		}

		@Override
		protected void setTopRowImpl() {
			int offset, tableTopPixel;
			if (topPixel < 0) {
				offset = -topPixel;
				tableTopPixel = 0;
			} else {
				int topIndex = Math.floorDiv(topPixel, rowHeight);
				int maxTopIndex = table.getItemCount() - (height / rowHeight) - 1;
				if (topIndex > maxTopIndex) {
					topIndex = Math.max(0, maxTopIndex);
					tableTopPixel = topIndex * rowHeight;
					offset = tableTopPixel - topPixel;
				} else {
					tableTopPixel = topPixel;
					offset = 0;
				}
			}
			if (this.offset != offset) {
				setRedraw(false);
				setTopPixelWithinTable(tableTopPixel);
				setOffset(offset);
				setRedraw(true);
			} else {
				setTopPixelWithinTable(tableTopPixel);
			}
		}

		protected abstract void setTopPixelWithinTable(int topPixel);
	}
}
