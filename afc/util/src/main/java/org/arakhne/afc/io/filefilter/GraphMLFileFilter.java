/* 
 * $Id$
 * 
 * Copyright (C) 2012-13 Stephane GALLAND.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
package org.arakhne.afc.io.filefilter ;

import org.arakhne.vmutil.locale.Locale;

/** File filter for the "GraphML" files.
 * 
 * @author $Author: galland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 * @since 16.0
 */
public class GraphMLFileFilter extends AbstractFileFilter {

	/** Default extension for the "GraphML" files.
	 */
	public static final String EXTENSION_GRAPHML = "graphml"; //$NON-NLS-1$

	/** Default extension for the "GraphML" files.
	 */
	public static final String EXTENSION_GRL = "grl"; //$NON-NLS-1$

	/**
	 */
	public GraphMLFileFilter() {
		this(true);
	}

	/**
	 * @param acceptDirectories is <code>true</code> to
	 * permit to this file filter to accept directories;
	 * <code>false</code> if the directories should not
	 * match.
	 */
	public GraphMLFileFilter(boolean acceptDirectories) {
		super(
				acceptDirectories,
				Locale.getString(GraphMLFileFilter.class, "FILE_FILTER_NAME"), //$NON-NLS-1$
				EXTENSION_GRAPHML, EXTENSION_GRL);
	}
	
}
