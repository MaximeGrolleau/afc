/* 
 * $Id$
 * 
 * Copyright (C) 2005-09 Stephane GALLAND.
 * Copyright (C) 2012 Stephane GALLAND.
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
package org.arakhne.afc.math.discrete.object2d;

import java.io.Serializable;

import org.arakhne.afc.math.generic.PathElementType;

/** An element of the path.
 *
 * @author $Author: galland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public abstract class PathElement2i implements Serializable {
	
	private static final long serialVersionUID = 7757419973445894032L;

	/** Create an instance of path element.
	 * 
	 * @param type is the type of the new element.
	 * @param lastX is the coordinate of the last point.
	 * @param lastY is the coordinate of the last point.
	 * @param coords are the coordinates.
	 * @return the instance of path element.
	 */
	public static PathElement2i newInstance(PathElementType type, int lastX, int lastY, int[] coords) {
		switch(type) {
		case MOVE_TO:
			return new MovePathElement2i(coords[0], coords[1]);
		case LINE_TO:
			return new LinePathElement2i(lastX, lastY, coords[0], coords[1]);
		case QUAD_TO:
			return new QuadPathElement2i(lastX, lastY, coords[0], coords[1], coords[2], coords[3]);
		case CURVE_TO:
			return new CurvePathElement2i(lastX, lastY, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
		case CLOSE:
			return new ClosePathElement2i(lastX, lastY, coords[0], coords[1]);
		default:
		}
		throw new IllegalArgumentException();
	}
	
	/** Type of the path element.
	 */
	public final PathElementType type;
	
	/** Source point.
	 */
	public final int fromX;
	
	/** Source point.
	 */
	public final int fromY;

	/** Target point.
	 */
	public final int toX;
	
	/** Target point.
	 */
	public final int toY;

	/** First control point.
	 */
	public final int ctrlX1;
	
	/** First control point.
	 */
	public final int ctrlY1;

	/** Second control point.
	 */
	public final int ctrlX2;
	
	/** Second control point.
	 */
	public final int ctrlY2;

	/**
	 * @param type is the type of the element.
	 * @param fromx is the source point.
	 * @param fromy is the source point.
	 * @param ctrlx1 is the first control point.
	 * @param ctrly1 is the first control point.
	 * @param ctrlx2 is the first control point.
	 * @param ctrly2 is the first control point.
	 * @param tox is the target point.
	 * @param toy is the target point.
	 */
	public PathElement2i(PathElementType type, int fromx, int fromy, int ctrlx1, int ctrly1, int ctrlx2, int ctrly2, int tox, int toy) {
		assert(type!=null);
		this.type = type;
		this.fromX = fromx;
		this.fromY = fromy;
		this.ctrlX1 = ctrlx1;
		this.ctrlY1 = ctrly1;
		this.ctrlX2 = ctrlx2;
		this.ctrlY2 = ctrly2;
		this.toX = tox;
		this.toY = toy;
	}

	/** Replies if the element is empty, ie. the points are the same.
	 * 
	 * @return <code>true</code> if the points are
	 * the same; otherwise <code>false</code>.
	 */
	public abstract boolean isEmpty();
	
	/** Replies if the element is not empty and its drawable.
	 * Only the path elements that may produce pixels on the screen
	 * must reply <code>true</code> in this function.
	 * 
	 * @return <code>true</code> if the path element
	 * is drawable; otherwise <code>false</code>.
	 */
	public abstract boolean isDrawable();

	/** Copy the coords into the given array, except the source point.
	 * 
	 * @param array
	 */
	public abstract void toArray(int[] array);

	/** Copy the coords into the given array, except the source point.
	 * 
	 * @param array
	 */
	public abstract void toArray(float[] array);

	/** Copy the coords into an array, except the source point.
	 * 
	 * @return the array of the points, except the source point.
	 */
	public abstract int[] toArray();

	/** An element of the path that represents a <code>MOVE_TO</code>.
	 *
	 * @author $Author: galland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	public static class MovePathElement2i extends PathElement2i {
		
		private static final long serialVersionUID = -8591881826671557331L;

		/**
		 * @param x
		 * @param y
		 */
		public MovePathElement2i(int x, int y) {
			super(PathElementType.MOVE_TO,
					0, 0, 0, 0, 0, 0,
					x, y);
		}

		@Override
		public boolean isEmpty() {
			return (this.fromX==this.toX) && (this.fromY==this.toY);
		}

		@Override
		public boolean isDrawable() {
			return false;
		}
		
		@Override
		public void toArray(int[] array) {
			array[0] = this.toX;
			array[1] = this.toY;
		}
		
		@Override
		public void toArray(float[] array) {
			array[0] = this.toX;
			array[1] = this.toY;
		}

		@Override
		public int[] toArray() {
			return new int[] {this.toX, this.toY};
		}

		@Override
		public String toString() {
			return "MOVE("+ //$NON-NLS-1$
					this.toX+"x"+ //$NON-NLS-1$
					this.toY+")"; //$NON-NLS-1$
		}

	}
	
	/** An element of the path that represents a <code>LINE_TO</code>.
	 *
	 * @author $Author: galland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	public static class LinePathElement2i extends PathElement2i {
		
		private static final long serialVersionUID = 497492389885992535L;

		/**
		 * @param fromx
		 * @param fromy
		 * @param tox
		 * @param toy
		 */
		public LinePathElement2i(int fromx, int fromy, int tox, int toy) {
			super(PathElementType.LINE_TO,
					fromx, fromy,
					0, 0, 0, 0,
					tox, toy);
		}
		
		@Override
		public boolean isEmpty() {
			return (this.fromX==this.toX) && (this.fromY==this.toY);
		}

		@Override
		public boolean isDrawable() {
			return !isEmpty();
		}

		@Override
		public void toArray(int[] array) {
			array[0] = this.toX;
			array[1] = this.toY;
		}
		
		@Override
		public void toArray(float[] array) {
			array[0] = this.toX;
			array[1] = this.toY;
		}

		@Override
		public int[] toArray() {
			return new int[] {this.toX, this.toY};
		}

		@Override
		public String toString() {
			return "LINE("+ //$NON-NLS-1$
					this.toX+"x"+ //$NON-NLS-1$
					this.toY+")"; //$NON-NLS-1$
		}

	}
	
	/** An element of the path that represents a <code>QUAD_TO</code>.
	 *
	 * @author $Author: galland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	public static class QuadPathElement2i extends PathElement2i {
		
		private static final long serialVersionUID = 6341899683730854257L;

		/**
		 * @param fromx
		 * @param fromy
		 * @param ctrlx
		 * @param ctrly
		 * @param tox
		 * @param toy
		 */
		public QuadPathElement2i(int fromx, int fromy, int ctrlx, int ctrly, int tox, int toy) {
			super(PathElementType.QUAD_TO,
					fromx, fromy,
					ctrlx, ctrly,
					0, 0,
					tox, toy);
		}
		
		@Override
		public boolean isEmpty() {
			return (this.fromX==this.toX) && (this.fromY==this.toY) &&
					(this.ctrlX1==this.toX) && (this.ctrlY1==this.toY);
		}

		@Override
		public boolean isDrawable() {
			return !isEmpty();
		}

		@Override
		public void toArray(int[] array) {
			array[0] = this.ctrlX1;
			array[1] = this.ctrlY1;
			array[2] = this.toX;
			array[3] = this.toY;
		}
		
		@Override
		public void toArray(float[] array) {
			array[0] = this.ctrlX1;
			array[1] = this.ctrlY1;
			array[2] = this.toX;
			array[3] = this.toY;
		}

		@Override
		public int[] toArray() {
			return new int[] {this.ctrlX1, this.ctrlY1, this.toX, this.toY};
		}
		
		@Override
		public String toString() {
			return "QUAD("+ //$NON-NLS-1$
					this.ctrlX1+"x"+ //$NON-NLS-1$
					this.ctrlY1+"|"+ //$NON-NLS-1$
					this.toX+"x"+ //$NON-NLS-1$
					this.toY+")"; //$NON-NLS-1$
		}

	}

	/** An element of the path that represents a <code>CURVE_TO</code>.
	 *
	 * @author $Author: galland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	public static class CurvePathElement2i extends PathElement2i {
		
		private static final long serialVersionUID = 1043302430176113524L;

		/**
		 * @param fromx
		 * @param fromy
		 * @param ctrlx1
		 * @param ctrly1
		 * @param ctrlx2
		 * @param ctrly2
		 * @param tox
		 * @param toy
		 */
		public CurvePathElement2i(int fromx, int fromy, int ctrlx1, int ctrly1, int ctrlx2, int ctrly2, int tox, int toy) {
			super(PathElementType.CURVE_TO,
					fromx, fromy,
					ctrlx1, ctrly1,
					ctrlx2, ctrly2,
					tox, toy);
		}
		
		@Override
		public boolean isEmpty() {
			return (this.fromX==this.toX) && (this.fromY==this.toY) &&
					(this.ctrlX1==this.toX) && (this.ctrlY1==this.toY) &&
					(this.ctrlX2==this.toX) && (this.ctrlY2==this.toY);
		}

		@Override
		public boolean isDrawable() {
			return !isEmpty();
		}

		@Override
		public void toArray(int[] array) {
			array[0] = this.ctrlX1;
			array[1] = this.ctrlY1;
			array[2] = this.ctrlX2;
			array[3] = this.ctrlY2;
			array[4] = this.toX;
			array[5] = this.toY;
		}
		
		@Override
		public void toArray(float[] array) {
			array[0] = this.ctrlX1;
			array[1] = this.ctrlY1;
			array[2] = this.ctrlX2;
			array[3] = this.ctrlY2;
			array[4] = this.toX;
			array[5] = this.toY;
		}

		@Override
		public int[] toArray() {
			return new int[] {this.ctrlX1, this.ctrlY1, this.ctrlX2, this.ctrlY2, this.toX, this.toY};
		}

		@Override
		public String toString() {
			return "CURVE("+ //$NON-NLS-1$
					this.ctrlX1+"x"+ //$NON-NLS-1$
					this.ctrlY1+"|"+ //$NON-NLS-1$
					this.ctrlX2+"x"+ //$NON-NLS-1$
					this.ctrlY2+"|"+ //$NON-NLS-1$
					this.toX+"x"+ //$NON-NLS-1$
					this.toY+")"; //$NON-NLS-1$
		}

	}

	/** An element of the path that represents a <code>CLOSE</code>.
	 *
	 * @author $Author: galland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	public static class ClosePathElement2i extends PathElement2i {
		
		private static final long serialVersionUID = 2745123226508569279L;

		/**
		 * @param fromx
		 * @param fromy
		 * @param tox
		 * @param toy
		 */
		public ClosePathElement2i(int fromx, int fromy, int tox, int toy) {
			super(PathElementType.CLOSE,
					fromx, fromy,
					0, 0, 0, 0,
					tox, toy);
		}
		
		@Override
		public boolean isEmpty() {
			return (this.fromX==this.toX) && (this.fromY==this.toY);
		}
		
		@Override
		public boolean isDrawable() {
			return false;
		}

		@Override
		public void toArray(int[] array) {
			//
		}
		
		@Override
		public void toArray(float[] array) {
			//
		}

		@Override
		public int[] toArray() {
			return new int[0];
		}
		
		@Override
		public String toString() {
			return "CLOSE"; //$NON-NLS-1$
		}

	}

}