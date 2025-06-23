package com.diffplug.common.swt

import com.diffplug.common.swt.jface.ImageDescriptors
import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Label
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(InteractiveTest::class)
class Issue_2228 {
    @Test
    fun recreate() {
        InteractiveTest.testCoat("Show the difference between `.svg` and `@x2.png` rendering") {
            Layouts.setGrid(it).numColumns(2)
            Label(it, SWT.NONE).apply {
                text = ".svg"
            }
            Label(it, SWT.NONE).apply {
                val imgDesc = ImageDescriptor.createFromFile(Issue_2228::class.java, "/issue_2228/strikethrough.svg");
                image = imgDesc.createImage()
            }
            Label(it, SWT.NONE).apply {
                text = ".png"
            }
            Label(it, SWT.NONE).apply {
                val imgDesc = ImageDescriptor.createFromFile(Issue_2228::class.java, "/issue_2228/strikethrough.png");
                image = imgDesc.createImage()
            }
        }
    }
}