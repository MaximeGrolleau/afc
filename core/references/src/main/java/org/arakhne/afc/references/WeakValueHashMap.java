/* 
 * $Id$
 * 
 * Copyright (C) 2005-2007 Stephane GALLAND.
 * Copyright (C) 2011 Stephane GALLAND.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * This program is free software; you can redistribute it and/or modify
 */

package org.arakhne.afc.references;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A <tt>Map</tt> implementation with {@link WeakReference weak values}. An entry in a
 * <tt>WeakValueHashMap</tt> will automatically be removed when its value is no
 * longer in ordinary use or null.
 * <p>
 * This class was inspirated from {@link WeakHashMap} and uses a {@link HashMap}
 * as its internal data structure.
 * <p>
 * This class has a special flag which permits to control the
 * way how the released references are expurged: {@link #isDeeplyExpurge()},
 * {@link #setDeeplyExpurge(boolean)}. If this flag is <code>true</code>,
 * all the released references will be immediately removed from the map even
 * if they are not enqueued by the virtual machine (see {@link #expurge()}.
 * If this flag is <code>false</code>,
 * only the enqueued references will be removed from the map
 * (see {@link #expurgeQueuedReferences()}.
 * <p>
 * If this map does not use a "deep expurge" of the released references,
 * it could contains <code>null</code> values that corresponds to
 * values that are released by the garbage collector. If a "deep expurge"
 * is used, all the values released by the garbage collector will be
 * removed from the map.
 * <p>
 * "Deep expurge" consumes much more time that "No deep expurge". This is the
 * reason why this feature is not activated by default.
 * <p>
 * The "deep expurge" feature was added to fix the uncoherent behavior
 * of the garbage collector which seems to not always enqueued the 
 * released values (sometimes the queue is empty even if a value was released).
 *
 * @param <K> is the type of the keys.
 * @param <V> is the type of the values.
 * @author $Author: galland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 * @since 5.8
 */
public class WeakValueHashMap<K,V> extends AbstractWeakValueMap<K,V> {
	
    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public WeakValueHashMap(int initialCapacity, float loadFactor) {
        super(new HashMap<K,ReferencableValue<K,V>>(initialCapacity, loadFactor));
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public WeakValueHashMap(int initialCapacity) {
    	super(new HashMap<K,ReferencableValue<K,V>>(initialCapacity));
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public WeakValueHashMap() {
    	super(new HashMap<K,ReferencableValue<K,V>>());
    }

    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     */
    public WeakValueHashMap(Map<? extends K, ? extends V> m) {
        super(new HashMap<K,ReferencableValue<K,V>>());
        putAll(m);
    }	
        
}
