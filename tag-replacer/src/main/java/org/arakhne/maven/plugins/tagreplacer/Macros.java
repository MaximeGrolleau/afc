/* 
 * $Id$
 * 
 * Copyright (C) 2010-12 Stephane GALLAND
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
package org.arakhne.maven.plugins.tagreplacer;

/**
 * List of the macros strings.
 *
 * @author $Author: galland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public interface Macros {

	/** &dollar;ArtifactId&dollar;
	 */
	public static final String MACRO_ARTIFACTID = "ArtifactId"; //$NON-NLS-1$

	/** &dollar;Author: id&dollar;
	 */
	public static final String MACRO_AUTHOR = "Author"; //$NON-NLS-1$
    
	/** &dollar;Date&dollar;
	 */
	public static final String MACRO_DATE = "Date"; //$NON-NLS-1$

	/** &dollar;Filename&dollar;
	 */
	public static final String MACRO_FILENAME = "Filename"; //$NON-NLS-1$

	/** &dollar;FullVersion&dollar;
	 */
	public static final String MACRO_FULLVERSION = "FullVersion"; //$NON-NLS-1$

	/** &dollar;GroupId&dollar;
	 */
	public static final String MACRO_GROUPID = "GroupId"; //$NON-NLS-1$

	/** &dollar;Id&dollar;
	 */
	public static final String MACRO_ID = "Id"; //$NON-NLS-1$

	/** &dollar;Name&dollar;
	 */
	public static final String MACRO_NAME = "Name"; //$NON-NLS-1$

	/** &dollar;Organization&dollar;
	 */
	public static final String MACRO_ORGANIZATION = "Organization"; //$NON-NLS-1$

	/** &dollar;Revision&dollar;
	 */
	public static final String MACRO_REVISION = "Revision"; //$NON-NLS-1$

	/** &dollar;Version&dollar;
	 */
	public static final String MACRO_VERSION = "Version"; //$NON-NLS-1$

	/** &dollar;Website&dollar;
	 */
	public static final String MACRO_WEBSITE = "Website"; //$NON-NLS-1$

	/** &dollar;Year&dollar;
	 * 
	 * @since 2.3
	 */
	public static final String MACRO_YEAR = "Year"; //$NON-NLS-1$

	/** &dollar;InceptionYear&dollar;
	 * 
	 * @since 2.3
	 */
	public static final String MACRO_INCEPTIONYEAR = "InceptionYear"; //$NON-NLS-1$

	/** &dollar;Prop: name&dollar;
	 * 
	 * @since 2.3
	 */
	public static final String MACRO_PROP = "Prop"; //$NON-NLS-1$

}