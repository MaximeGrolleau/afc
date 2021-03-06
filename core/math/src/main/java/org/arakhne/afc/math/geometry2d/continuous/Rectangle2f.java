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
package org.arakhne.afc.math.geometry2d.continuous;

import java.util.NoSuchElementException;

import org.arakhne.afc.math.MathConstants;
import org.arakhne.afc.math.geometry.GeometryUtil;
import org.arakhne.afc.math.geometry.IntersectionUtil;
import org.arakhne.afc.math.geometry.PathWindingRule;
import org.arakhne.afc.math.geometry2d.Point2D;
import org.arakhne.afc.math.geometry2d.Vector2D;



/** 2D rectangle with floating-point points.
 * 
 * @author $Author: galland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public class Rectangle2f extends AbstractRectangularShape2f<Rectangle2f> {

	private static final long serialVersionUID = 8716296371653330467L;

	/**
	 */
	public Rectangle2f() {
		//
	}

	/**
	 * @param min is the min corner of the rectangle.
	 * @param max is the max corner of the rectangle.
	 */
	public Rectangle2f(Point2f min, Point2f max) {
		super(min, max);
	}

	/**
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public Rectangle2f(float x, float y, float width, float height) {
		super(x, y, width, height);
	}
	
	/**
	 * @param r
	 */
	public Rectangle2f(Rectangle2f r) {
		super(r);
	}

	@Override
	public void set(Shape2f s) {
		s.toBoundingBox(this);
	}

	/** {@inheritDoc}
	 */
	@Override
	public Rectangle2f toBoundingBox() {
		return this;
	}

	/** {@inheritDoc}
	 */
	@Override
	public float distanceSquared(Point2D p) {
		return GeometryUtil.distanceSquaredPointRectangle(p.getX(), p.getY(), this.getMinX(), this.getMinY(), this.getMaxX(),this.getMaxY());
	}

	/** {@inheritDoc}
	 */
	@Override
	public float distanceL1(Point2D p) {
		float dx;
		if (p.getX()<getMinX()) {
			dx = getMinX() - p.getX();
		}
		else if (p.getX()>getMaxX()) {
			dx = p.getX() - getMaxX();
		}
		else {
			dx = 0f;
		}
		float dy;
		if (p.getY()<getMinY()) {
			dy = getMinY() - p.getY();
		}
		else if (p.getY()>getMaxY()) {
			dy = p.getY() - getMaxY();
		}
		else {
			dy = 0f;
		}
		return dx + dy;
	}

	/** {@inheritDoc}
	 */
	@Override
	public float distanceLinf(Point2D p) {
		float dx;
		if (p.getX()<getMinX()) {
			dx = getMinX() - p.getX();
		}
		else if (p.getX()>getMaxX()) {
			dx = p.getX() - getMaxX();
		}
		else {
			dx = 0f;
		}
		float dy;
		if (p.getY()<getMinY()) {
			dy = getMinY() - p.getY();
		}
		else if (p.getY()>getMaxY()) {
			dy = p.getY() - getMaxY();
		}
		else {
			dy = 0f;
		}
		return Math.max(dx, dy);
	}

	/** {@inheritDoc}
	 */
	@Override
	public boolean contains(float x, float y) {
		return GeometryUtil.isInsidePointRectangle(x, y, getMinX(), getMinY(), getMaxX(), getMaxY());
	}

	@Override
	public boolean contains(Rectangle2f r) {
		return GeometryUtil.isInsideRectangleRectangle(
				getMinX(), getMinY(), getMaxX(), getMaxY(),
				r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY());
	}

	/** {@inheritDoc}
	 */
	@Override
	public Point2D getClosestPointTo(Point2D p) {
		
		Point2f closest = new Point2f();
		GeometryUtil.closestPointPointRectangle(p.getX(), p.getY(), getMinX(), getMinY(), getMaxX(), getMaxY(), closest);
		return closest;
	}

	/** Add the given coordinate in the rectangle.
	 * <p>
	 * The corners of the rectangles are moved to
	 * enclosed the given coordinate.
	 * 
	 * @param p
	 */
	public void add(Point2D p) {
		add(p.getX(), p.getY());
	}

	/** Add the given coordinate in the rectangle.
	 * <p>
	 * The corners of the rectangles are moved to
	 * enclosed the given coordinate.
	 * 
	 * @param x
	 * @param y
	 */
	public void add(float x, float y) {
		if (x<getMinX()) {
			setMinX(x);
		}
		else if (x>getMaxX()) {
			setMaxX(x);
		}
		if (y<getMinY()) {
			setMinY(y);
		}
		else if (y>getMaxY()) {
			setMaxY(y);
		}
	}

	/** Compute and replies the union of this rectangle and the given rectangle.
	 * This function does not change this rectangle.
	 * <p>
	 * It is equivalent to (where <code>ur</code> is the union):
	 * <pre><code>
	 * Rectangle2f ur = new Rectangle2f();
	 * Rectangle2f.union(ur, this, r);
	 * </code></pre>
	 * 
	 * @param r
	 * @return the union of this rectangle and the given rectangle.
	 * @see #union(Rectangle2f, Rectangle2f, Rectangle2f)
	 * @see #setUnion(Rectangle2f)
	 */
	public Rectangle2f createUnion(Rectangle2f r) {
		Rectangle2f rr = this.clone();
		rr.union(r);
		return rr;
	}

	/** Compute and replies the intersection of this rectangle and the given rectangle.
	 * This function does not change this rectangle.
	 * <p>
	 * It is equivalent to (where <code>ir</code> is the intersection):
	 * <pre><code>
	 * Rectangle2f ir = new Rectangle2f();
	 * Rectangle2f.intersection(ir, this, r);
	 * </code></pre>
	 * 
	 * @param r
	 * @return the union of this rectangle and the given rectangle.
	 * @see #intersection(Rectangle2f, Rectangle2f, Rectangle2f)
	 * @see #createIntersection(Rectangle2f)
	 */
	public Rectangle2f createIntersection(Rectangle2f r) {
		Rectangle2f rr = this.clone();
		rr.intersection(r);
		return rr;
	}
	

	@Override
	public PathIterator2f getPathIterator(Transform2D transform) {
		if (transform==null) {
			return new CopyPathIterator(
					getMinX(), getMinY(),
					getMaxX(), getMaxY());
		}
		return new TransformPathIterator(
				getMinX(), getMinY(),
				getMaxX(), getMaxY(),
				transform);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof Rectangle2f) {
			Rectangle2f rr2d = (Rectangle2f) obj;
			return ((getMinX() == rr2d.getMinX()) &&
					(getMinY() == rr2d.getMinY()) &&
					(getWidth() == rr2d.getWidth()) &&
					(getHeight() == rr2d.getHeight()));
		}
		return false;
	}

	@Override
	public int hashCode() {
		long bits = 1L;
		bits = 31L * bits + floatToIntBits(getMinX());
		bits = 31L * bits + floatToIntBits(getMinY());
		bits = 31L * bits + floatToIntBits(getMaxX());
		bits = 31L * bits + floatToIntBits(getMaxY());
		return (int) (bits ^ (bits >> 32));
	}

	@Override
	public boolean intersects(Rectangle2f s) {
		return IntersectionUtil.intersectsRectangleRectangle(
				getMinX(), getMinY(),
				getMaxX(), getMaxY(),
				s.getMinX(), s.getMinY(),
				s.getMaxX(), s.getMaxY());
	}

	@Override
	public boolean intersects(Ellipse2f s) {
		return IntersectionUtil.intersectsEllipseRectangle(
				s.getMinX(), s.getMinY(),
				s.getMaxX(), s.getMaxY(),
				getMinX(), getMinY(),
				getMaxX(), getMaxY());
	}

	@Override
	public boolean intersects(Circle2f s) {
		return IntersectionUtil.intersectsCircleRectangle(
				s.getX(), s.getY(),
				s.getRadius(),
				getMinX(), getMinY(),
				getMaxX(), getMaxY());
	}

	@Override
	public boolean intersects(Segment2f s) {
		return IntersectionUtil.intersectsRectangleSegment(
				getMinX(), getMinY(),
				getMaxX(), getMaxY(),
				s.getX1(), s.getY1(),
				s.getX2(), s.getY2());
	}
	
	@Override
	public boolean intersects(OrientedRectangle2f s) {
		return IntersectionUtil.intersectsAlignedRectangleOrientedRectangle(
				this.minx, this.miny, this.maxy, this.maxy,
				s.getCx(), s.getCy(), s.getRx(), s.getRy(), s.getSx(), s.getSy(), s.getExtentR(), s.getExtentS());
	}

	@Override
	public boolean intersects(Path2f s) {
		return intersects(s.getPathIterator(MathConstants.SPLINE_APPROXIMATION_RATIO));
	}

	@Override
	public boolean intersects(PathIterator2f s) {
		// Copied from AWT API
		if (isEmpty()) return false;
		int mask = (s.getWindingRule() == PathWindingRule.NON_ZERO ? -1 : 2);
		int crossings = Path2f.computeCrossingsFromRect(
				s,
				getMinX(), getMinY(), getMaxX(), getMaxY(),
				false, true);
		return (crossings == MathConstants.SHAPE_INTERSECTS ||
				(crossings & mask) != 0);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("["); //$NON-NLS-1$
		b.append(getMinX());
		b.append(";"); //$NON-NLS-1$
		b.append(getMinY());
		b.append(";"); //$NON-NLS-1$
		b.append(getMaxX());
		b.append(";"); //$NON-NLS-1$
		b.append(getMaxY());
		b.append("]"); //$NON-NLS-1$
		return b.toString();
	}

	/** Move this rectangle to avoid collision 
	 * with the reference rectangle.
	 * 
	 * @param reference is the rectangle to avoid collision with.
	 * @return the displacement vector.
	 */
	public Vector2D avoidCollisionWith(Rectangle2f reference) {
		float dx1 = reference.getMaxX() - getMinX();
		float dx2 = getMaxX() - reference.getMinX();
		float dy1 = reference.getMaxY() - getMinY();
		float dy2 = getMaxY() - reference.getMinY();

		float absdx1 = Math.abs(dx1);
		float absdx2 = Math.abs(dx2);
		float absdy1 = Math.abs(dy1);
		float absdy2 = Math.abs(dy2);

		float dx = 0;
		float dy = 0;

		if (dx1>=0 && absdx1<=absdx2 && absdx1<=absdy1 && absdx1<=absdy2) {
			// Move according to dx1
			dx = dx1; 
		}
		else if (dx2>=0 && absdx2<=absdx1 && absdx2<=absdy1 && absdx2<=absdy2) {
			// Move according to dx2
			dx = - dx2;
		}
		else if (dy1>=0 && absdy1<=absdx1 && absdy1<=absdx2 && absdy1<=absdy2) {
			// Move according to dy1
			dy = dy1; 
		}
		else {
			// Move according to dy2
			dy = - dy2;
		}

		set(
				getMinX()+dx,
				getMinY()+dy,
				getWidth(),
				getHeight());

		return new Vector2f(dx, dy);
	}

	/** Move this rectangle to avoid collision 
	 * with the reference rectangle.
	 * 
	 * @param reference is the rectangle to avoid collision with.
	 * @param displacementDirection is the direction of the allowed displacement.
	 * @return the displacement vector.
	 */
	public Vector2D avoidCollisionWith(Rectangle2f reference, Vector2D displacementDirection) {
		if (displacementDirection==null || displacementDirection.lengthSquared()==0f)
			return avoidCollisionWith(reference);

		float dx1 = reference.getMaxX() - getMinX();
		float dx2 = reference.getMinX() - getMaxX();
		float dy1 = reference.getMaxY() - getMinY();
		float dy2 = reference.getMinY() - getMaxY();

		float absdx1 = Math.abs(dx1);
		float absdx2 = Math.abs(dx2);
		float absdy1 = Math.abs(dy1);
		float absdy2 = Math.abs(dy2);

		float dx, dy;

		if (displacementDirection.getX()<0) {
			dx = -Math.min(absdx1, absdx2);
		}
		else {
			dx = Math.min(absdx1, absdx2);
		}

		if (displacementDirection.getY()<0) {
			dy = -Math.min(absdy1, absdy2);
		}
		else {
			dy = Math.min(absdy1, absdy2);
		}

		set(
				getMinX()+dx,
				getMinY()+dy,
				getWidth(),
				getHeight());

		displacementDirection.set(dx, dy);
		return displacementDirection;
	}
	
	/** Compute the union of r1 and r2. Set this with the result.
	 * 
	 * @param dest is the union.
	 * @param r1
	 * @param r2
	 */
	public void union(Rectangle2f r1, Rectangle2f r2) {
		this.setFromCorners(
				Math.min(r1.getMinX(), r2.getMinX()),
				Math.min(r1.getMinY(), r2.getMinY()),
				Math.max(r1.getMaxX(), r2.getMaxX()),
				Math.max(r1.getMaxY(), r2.getMaxY()));
	}
	
	/** Compute the union of this and r1. Set this with the result.
	 * 
	 * @param r1
	 */
	public void union(Rectangle2f r1) {
		this.setFromCorners(
				Math.min(this.getMinX(), r1.getMinX()),
				Math.min(this.getMinY(), r1.getMinY()),
				Math.max(this.getMaxX(), r1.getMaxX()),
				Math.max(this.getMaxY(), r1.getMaxY()));
	}

	/** Compute the intersection of r1 and this. Set this with the result.
	 * 
	 * @param r1
	 * 
	 */
	public void intersection(Rectangle2f r1) {
		float x1 = Math.max(r1.getMinX(), this.getMinX());
		float y1 = Math.max(r1.getMinY(), this.getMinY());
		float x2 = Math.min(r1.getMaxX(), this.getMaxX());
		float y2 = Math.min(r1.getMaxY(), this.getMaxY());
		if (x1<=x2 && y1<=y2) {
			this.setFromCorners(x1, y1, x2, y2);
		}
		else {
			this.set(0, 0, 0, 0);
		}
	}
	
	/** Compute the intersection of r1 and r2. Set this with the result.
	 * 
	 * @param r1
	 * @param r2
	 */
	public void intersection(Rectangle2f r1, Rectangle2f r2) {
		float x1 = Math.max(r1.getMinX(), r2.getMinX());
		float y1 = Math.max(r1.getMinY(), r2.getMinY());
		float x2 = Math.min(r1.getMaxX(), r2.getMaxX());
		float y2 = Math.min(r1.getMaxY(), r2.getMaxY());
		if (x1<=x2 && y1<=y2) {
			this.setFromCorners(x1, y1, x2, y2);
		}
		else {
			this.set(0, 0, 0, 0);
		}
	}
	
	
	

	/** Iterator on the path elements of the rectangle.
	 * 
	 * @author $Author: galland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	private static class CopyPathIterator implements PathIterator2f {

		private final float x1;
		private final float y1;
		private final float x2;
		private final float y2;
		private int index = 0;

		/**
		 * @param x1
		 * @param y1
		 * @param x2
		 * @param y2
		 */
		public CopyPathIterator(float x1, float y1, float x2, float y2) {
			this.x1 = Math.min(x1, x2);
			this.y1 = Math.min(y1, y2);
			this.x2 = Math.max(x1, x2);
			this.y2 = Math.max(y1, y2);
			if (Math.abs(this.x1-this.x2)<=0f || Math.abs(this.y1-this.y2)<=0f) {
				this.index = 6;
			}
		}

		@Override
		public boolean hasNext() {
			return this.index<=5;
		}

		@Override
		public PathElement2f next() {
			int idx = this.index;
			++this.index;
			switch(idx) {
			case 0:
				return new PathElement2f.MovePathElement2f(
						this.x1, this.y1);
			case 1:
				return new PathElement2f.LinePathElement2f(
						this.x1, this.y1,
						this.x2, this.y1);
			case 2:
				return new PathElement2f.LinePathElement2f(
						this.x2, this.y1,
						this.x2, this.y2);
			case 3:
				return new PathElement2f.LinePathElement2f(
						this.x2, this.y2,
						this.x1, this.y2);
			case 4:
				return new PathElement2f.LinePathElement2f(
						this.x1, this.y2,
						this.x1, this.y1);
			case 5:
				return new PathElement2f.ClosePathElement2f(
						this.x1, this.y1,
						this.x1, this.y1);
			default:
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public PathWindingRule getWindingRule() {
			return PathWindingRule.NON_ZERO;
		}
		
		@Override
		public boolean isPolyline() {
			return true;
		}
	}

	/** Iterator on the path elements of the rectangle.
	 * 
	 * @author $Author: galland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	private static class TransformPathIterator implements PathIterator2f {

		private final Transform2D transform;
		private final float x1;
		private final float y1;
		private final float x2;
		private final float y2;
		private int index = 0;

		private final Point2D p1 = new Point2f();
		private final Point2D p2 = new Point2f();

		/**
		 * @param x1
		 * @param y1
		 * @param x2
		 * @param y2
		 * @param transform
		 */
		public TransformPathIterator(float x1, float y1, float x2, float y2, Transform2D transform) {
			this.transform = transform;
			this.x1 = Math.min(x1, x2);
			this.y1 = Math.min(y1, y2);
			this.x2 = Math.max(x1, x2);
			this.y2 = Math.max(y1, y2);
			if (Math.abs(this.x1-this.x2)<=0f || Math.abs(this.y1-this.y2)<=0f) {
				this.index = 6;
			}
		}

		@Override
		public boolean hasNext() {
			return this.index<=5;
		}

		@Override
		public PathElement2f next() {
			int idx = this.index;
			++this.index;
			switch(idx) {
			case 0:
				this.p2.set(this.x1, this.y1);
				if (this.transform!=null) {
					this.transform.transform(this.p2);
				}
				return new PathElement2f.MovePathElement2f(
						this.p2.getX(), this.p2.getY());
			case 1:
				this.p1.set(this.p2);
				this.p2.set(this.x2, this.y1);
				if (this.transform!=null) {
					this.transform.transform(this.p2);
				}
				return new PathElement2f.LinePathElement2f(
						this.p1.getX(), this.p1.getY(),
						this.p2.getX(), this.p2.getY());
			case 2:
				this.p1.set(this.p2);
				this.p2.set(this.x2, this.y2);
				if (this.transform!=null) {
					this.transform.transform(this.p2);
				}
				return new PathElement2f.LinePathElement2f(
						this.p1.getX(), this.p1.getY(),
						this.p2.getX(), this.p2.getY());
			case 3:
				this.p1.set(this.p2);
				this.p2.set(this.x1, this.y2);
				if (this.transform!=null) {
					this.transform.transform(this.p2);
				}
				return new PathElement2f.LinePathElement2f(
						this.p1.getX(), this.p1.getY(),
						this.p2.getX(), this.p2.getY());
			case 4:
				this.p1.set(this.p2);
				this.p2.set(this.x1, this.y1);
				if (this.transform!=null) {
					this.transform.transform(this.p2);
				}
				return new PathElement2f.LinePathElement2f(
						this.p1.getX(), this.p1.getY(),
						this.p2.getX(), this.p2.getY());
			case 5:
				return new PathElement2f.ClosePathElement2f(
						this.p2.getX(), this.p2.getY(),
						this.p2.getX(), this.p2.getY());
			default:
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public PathWindingRule getWindingRule() {
			return PathWindingRule.NON_ZERO;
		}
		
		@Override
		public boolean isPolyline() {
			return true;
		}

	}
}