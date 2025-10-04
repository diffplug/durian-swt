/*
 * Copyright (C) 2020-2025 DiffPlug
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
package com.diffplug.durian.swt.coroutines

import com.diffplug.common.swt.SwtExec
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.internal.MainDispatcherFactory
import org.eclipse.swt.widgets.Display

/**
 * Factory for creating SWT-based main coroutine dispatcher.
 * 
 * This factory integrates SWT's UI thread with Kotlin coroutines by providing
 * the SWT Display thread as the main dispatcher. When this library is on the 
 * classpath, coroutines launched on Dispatchers.Main will automatically execute
 * on the SWT UI thread.
 * 
 * The factory uses [SwtExec.immediate] which provides optimal performance:
 * - Executes immediately when already on the SWT thread
 * - Uses Display.asyncExec for cross-thread dispatch
 * 
 * @see SwtExec.immediate
 */
@OptIn(InternalCoroutinesApi::class)
class SwtMainDispatcherFactory : MainDispatcherFactory {
    
    /**
     * Load order priority for this factory.
     * Higher values have higher priority. Using a moderate value to allow
     * other UI toolkit factories to potentially override if needed.
     */
    override val loadPriority: Int = 100
    
    /**
     * Creates the main dispatcher if SWT Display is available.
     * 
     * @param allFactories All available main dispatcher factories (unused)
     * @return SWT-based main dispatcher
     * @throws IllegalStateException if SWT Display is not available
     */
    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher {
        try {
            // Try to get the default Display - this will fail if SWT is not properly initialized
            // or if we're in a headless environment
            Display.getDefault()
            
            // If we got here successfully, SWT is available
            // Return the dispatcher from SwtExec.immediate() which provides optimal thread handling
            return SwtExec.immediate().rxExecutor.dispatcher as MainCoroutineDispatcher
        } catch (e: Exception) {
            // SWT Display is not available (headless environment, not initialized, etc.)
            // Throw an exception as the interface no longer supports nullable returns
            throw IllegalStateException("SWT Display is not available", e)
        }
    }
    
    /**
     * Hint about the expected dispatcher type.
     * Returns null since we throw an exception when SWT is not available.
     */
    override fun hintOnError(): String? = null
}