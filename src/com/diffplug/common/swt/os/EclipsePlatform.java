/**
 * Copyright 2015 DiffPlug
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
package com.diffplug.common.swt.os;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/** Parsing and whatsuch for the eclipse platform, including the Wuff gradle plugin. */
public class EclipsePlatform {
	public final String ws;
	public final String os;
	public final String arch;

	private EclipsePlatform(String ws, String os, String arch) {
		this.ws = ws;
		this.os = os;
		this.arch = arch;
	}

	@Override
	public boolean equals(Object otherRaw) {
		if (otherRaw instanceof EclipsePlatform) {
			EclipsePlatform other = (EclipsePlatform) otherRaw;
			return ws.equals(other.ws) && os.equals(other.os) && arch.equals(other.arch);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(ws, os, arch);
	}

	/** Returns "os.ws.arch" */
	@Override
	public String toString() {
		return ws + "." + os + "." + arch;
	}

	/** Returns a string appropriate as an Eclipse-PlatformFilter. */
	public String platformFilter() {
		return "(& " + "(osgi.ws=" + ws + ") " + "(osgi.os=" + os + ") " + "(osgi.arch=" + arch + ")" + " )";
	}

	/** Returns a map containing the platform properties. */
	public Map<String, String> platformProperties() {
		return ImmutableMap.of("osgi.ws", ws, "osgi.os", os, "osgi.arch", arch);
	}

	/** Returns the code that wuff uses for this EclipsePlatform. */
	public String getWuffString() {
		String wuffString = WUFF_MAP.get(this);
		Preconditions.checkNotNull(wuffString);
		return wuffString;
	}

	// @formatter:off
	private static final ImmutableMap<EclipsePlatform, String> WUFF_MAP = ImmutableMap.of(
			parseWsOsArch("cocoa.macosx.x86_64"),	"macosx-x86_64",
			parseWsOsArch("gtk.linux.x86"),			"linux-x86_32",
			parseWsOsArch("gtk.linux.x86_64"),		"linux-x86_64",
			parseWsOsArch("win32.win32.x86"),		"windows-x86_32",
			parseWsOsArch("win32.win32.x86_64"),	"windows-x86_64");
	// @formatter:on

	/** Parses ws.os.arch strings (as the SWT bundles are specified). */
	public static EclipsePlatform parseWsOsArch(String unparsed) {
		String[] pieces = unparsed.split("\\.");
		Preconditions.checkArgument(pieces.length == 3);
		String ws = pieces[0];
		String os = pieces[1];
		String arch = pieces[2];
		return new EclipsePlatform(ws, os, arch);
	}

	/** Parses ws.os.arch strings (as the SWT bundles are specified). */
	public static EclipsePlatform parseOsWsArch(String unparsed) {
		String[] pieces = unparsed.split("\\.");
		Preconditions.checkArgument(pieces.length == 3);
		String os = pieces[0];
		String ws = pieces[1];
		String arch = pieces[2];
		return new EclipsePlatform(ws, os, arch);
	}

	/** Returns the EclipsePlatform for the native platform. */
	public static EclipsePlatform getNative() {
		return fromOS(OS.getNative());
	}

	/** Returns the EclipsePlatform for the running platform. */
	public static EclipsePlatform getRunning() {
		return fromOS(OS.getRunning());
	}

	/** Converts an OS to an EclipsePlatform. */
	public static EclipsePlatform fromOS(OS raw) {
		String ws = raw.winMacLinux("win32", "cocoa", "gtk");
		String os = raw.winMacLinux("win32", "macosx", "linux");
		String arch = raw.getArch().x86x64("x86", "x86_64");
		return new EclipsePlatform(ws, os, arch);
	}

	/** Returns all of the platforms. */
	public static List<EclipsePlatform> getAll() {
		return Lists.transform(Arrays.asList(OS.values()), EclipsePlatform::fromOS);
	}
}
