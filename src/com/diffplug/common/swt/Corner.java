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
package com.diffplug.common.swt;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/** Positions within a rectangle (the corners, the center of the lines, and the center). */
public enum Corner {
	// @formatter:off
	TOP_LEFT(0, 0), TOP_RIGHT(1, 0), BOTTOM_LEFT(0, 1), BOTTOM_RIGHT(1, 1), // true corners
	TOP(0.5f, 0), LEFT(0, 0.5f), BOTTOM(0.5f, 1), RIGHT(1, 0.5f), CENTER(0.5f, 0.5f); // edges and center
	// @formatter:on

	private final float x, y;

	private Corner(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/** Returns this corner's position within the given rectangle. */
	public Point getPosition(Rectangle rectangle) {
		return new Point(
				rectangle.x + (int) (x * rectangle.width),
				rectangle.y + (int) (y * rectangle.height));
	}

	/**
	 * If you move the topLeft of `rectangle` to the returned point,
	 * then this corner will be at `position`. 
	 */
	public Point topLeftRequiredFor(Rectangle rectangle, Point position) {
		Point current = getPosition(rectangle);
		return new Point(
				rectangle.x + position.x - current.x,
				rectangle.y + position.y - current.y);
	}
}
