package com.diffplug.common.swt;

import org.eclipse.swt.widgets.Layout;

/** Base class to Layouts{X}Layout. */
interface LayoutWrapper {
	LayoutWrapper margin(int margin);
	LayoutWrapper spacing(int spacing);
	Layout getRaw();
}
