/*
 * Copyright (C) 2018-2022 DiffPlug
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


import com.diffplug.common.swt.ControlWrapper;
import com.diffplug.common.swt.VScrollBubble;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

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
