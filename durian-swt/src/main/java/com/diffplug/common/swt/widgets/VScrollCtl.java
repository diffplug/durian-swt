/*
 * Copyright (C) 2018-2020 DiffPlug, LLC - All Rights Reserved
 * Unauthorized copying of this file via any medium is strictly prohibited.
 * Proprietary and confidential.
 * Please send any inquiries to Ned Twigg <ned.twigg@diffplug.com>
 */
package com.diffplug.common.swt.widgets;

import com.diffplug.common.swt.VScrollBubble;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.diffplug.common.swt.ControlWrapper;

/**
 * SWT provides {@link ScrolledComposite} which provides a wealth of
 * scrolling behavior ([google](https://www.google.com/search?q=SWT+ScrolledComposite)).
 * However, the most common case is that you only want to scroll vertically.  This class
 * makes that easy.  Just create your content with {@link #getParentForContent()} as the
 * parent, and then set it to be the content with {@link #setContent(Control)}.  Easy!
 */
public class VScrollCtl extends ControlWrapper.AroundControl<ScrolledComposite> {
	public VScrollCtl(Composite parent, int style) {
		super(new ScrolledComposite(parent, SWT.VERTICAL | style));
		VScrollBubble.applyTo(parent.getDisplay());
	}

	public ScrolledComposite getParentForContent() {
		return wrapped;
	}

	public void setContent(ControlWrapper wrapper) {
		setContent(wrapper.getRootControl());
	}

	public void setContent(Control content) {
		wrapped.setContent(content);
		wrapped.setExpandHorizontal(true);
		wrapped.setExpandVertical(true);
		wrapped.addListener(SWT.Resize, e -> {
			int scrollWidth = wrapped.getVerticalBar().getSize().x;
			Point size = wrapped.getSize();
			Point contentSize = content.computeSize(size.x - scrollWidth, SWT.DEFAULT);
			content.setSize(contentSize);
			wrapped.setMinSize(contentSize);
		});
	}
}
