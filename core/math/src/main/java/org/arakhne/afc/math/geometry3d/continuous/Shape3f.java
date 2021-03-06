/* 
 * $Id$
 * 
 * Copyright (C) 2010-2013 Stephane GALLAND.
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
package org.arakhne.afc.math.geometry3d.continuous;

import org.arakhne.afc.math.geometry3d.Point3D;
import org.arakhne.afc.math.geometry3d.Shape3D;


/** 2D shape with floating-point points.
 * 
 * @author $Author: galland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public interface Shape3f extends Shape3D {

	/** {@inheritDoc}
	 */
	@Override
	public Shape3f clone();

//	/** Replies the bounds of the shape.
//	 * 
//	 * @return the bounds of the shape.
//	 */
//	public Rectangle2f toBoundingBox();
	
//	/** Replies the bounds of the shape.
//	 * 
//	 * @param box is set with the bounds of the shape.
//	 */
//	public void toBoundingBox(Rectangle2f box);

	/** Replies the minimal distance from this shape to the given point.
	 * 
	 * @param p
	 * @return the minimal distance between this shape and the point.
	 */
	public float distance(Point3D p);

	/** Replies the squared value of the minimal distance from this shape to the given point.
	 * 
	 * @param p
	 * @return squared value of the minimal distance between this shape and the point.
	 */
	public float distanceSquared(Point3D p);

	/**
	 * Computes the L-1 (Manhattan) distance between this shape and
	 * point p1.  The L-1 distance is equal to abs(x1-x2) + abs(y1-y2) +abs(z1-z2).
	 * @param p the point
	 * @return the distance.
	 */
	public float distanceL1(Point3D p);

	/**
	 * Computes the L-infinite distance between this shape and
	 * point p1.  The L-infinite distance is equal to 
	 * MAX[abs(x1-x2), abs(y1-y2), abs(z1-z2)]. 
	 * @param p the point
	 * @return the distance.
	 */
	public float distanceLinf(Point3D p);

	/** Translate the shape.
	 * 
	 * @param dx
	 * @param dy
	 * @param dz
	 */
	public void translate(float dx, float dy, float dz); 
	
	/** Replies if the given point is inside this shape.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return <code>true</code> if the given point is inside this
	 * shape, otherwise <code>false</code>.
	 */
	public boolean contains(float x, float y, float z);
	
//	/** Replies if the given rectangle is inside this shape.
//	 * 
//	 * @param r
//	 * @return <code>true</code> if the given rectangle is inside this
//	 * shape, otherwise <code>false</code>.
//	 */
//	public boolean contains(Rectangle2f r);

//	/** Replies the elements of the paths.
//	 * 
//	 * @param transform is the transformation to apply to the path.
//	 * @return the elements of the path.
//	 */
//	public PathIterator2f getPathIterator(Transform2D transform);
//
//	/** Replies the elements of the paths.
//	 * 
//	 * @return the elements of the path.
//	 */
//	public PathIterator2f getPathIterator();

	/** Apply the transformation to the shape and reply the result.
	 * This function does not change the current shape.
	 * 
	 * @param transform is the transformation to apply to the shape.
	 * @return the result of the transformation.
	 */
	public Shape3f createTransformedShape(Transform3D transform);
	
//	/** Replies if this shape is intersecting the given rectangle.
//	 * 
//	 * @param s
//	 * @return <code>true</code> if this shape is intersecting the given shape;
//	 * <code>false</code> if there is no intersection.
//	 */
//	public boolean intersects(Rectangle2f s);
//
//	/** Replies if this shape is intersecting the given ellipse.
//	 * 
//	 * @param s
//	 * @return <code>true</code> if this shape is intersecting the given shape;
//	 * <code>false</code> if there is no intersection.
//	 */
//	public boolean intersects(Ellipse2f s);
//
//	/** Replies if this shape is intersecting the given circle.
//	 * 
//	 * @param s
//	 * @return <code>true</code> if this shape is intersecting the given shape;
//	 * <code>false</code> if there is no intersection.
//	 */
//	public boolean intersects(Circle2f s);
//
//	/** Replies if this shape is intersecting the given line.
//	 * 
//	 * @param s
//	 * @return <code>true</code> if this shape is intersecting the given shape;
//	 * <code>false</code> if there is no intersection.
//	 */
//	public boolean intersects(Segment2f s);
//
//	/** Replies if this shape is intersecting the given path.
//	 * 
//	 * @param s
//	 * @return <code>true</code> if this shape is intersecting the given path;
//	 * <code>false</code> if there is no intersection.
//	 */
//	public boolean intersects(Path2f s);
//
//	/** Replies if this shape is intersecting the given path.
//	 * 
//	 * @param s
//	 * @return <code>true</code> if this shape is intersecting the given path;
//	 * <code>false</code> if there is no intersection.
//	 */
//	public boolean intersects(PathIterator2f s);

}