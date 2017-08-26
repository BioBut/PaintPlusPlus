package pl.karol202.paintplus.tool.gradient;

import android.graphics.*;
import android.graphics.Region.Op;
import pl.karol202.paintplus.R;
import pl.karol202.paintplus.image.Image;
import pl.karol202.paintplus.image.layer.Layer;
import pl.karol202.paintplus.tool.CoordinateSpace;
import pl.karol202.paintplus.tool.OnToolChangeListener;
import pl.karol202.paintplus.tool.StandardTool;
import pl.karol202.paintplus.tool.ToolProperties;
import pl.karol202.paintplus.tool.selection.Selection;

public class ToolGradient extends StandardTool implements OnToolChangeListener
{
	interface OnGradientEditListener
	{
		void onGradientSet();
	}
	
	private static final float POINT_OUTER_RADIUS = 10;
	private static final float POINT_INNER_RADIUS = 4;
	
	private static final float MAX_DISTANCE = 70;
	
	private Gradient gradient;
	private GradientShape shape;
	
	private OnGradientEditListener listener;
	private Selection selection;
	private GradientShapes shapes;
	private Paint maskPaint;
	private Paint pointOuterPaint;
	private Paint pointInnerPaint;
	
	private Layer layer;
	private Canvas canvas;
	private Path selectionPath;
	
	private PointF firstPoint;
	private PointF secondPoint;
	private boolean pointsCreated;
	private RectF pointDrawRect;
	
	private int draggingIndex;
	private PointF draggingStart;
	private PointF previousPositionOfDraggedPoint;
	
	public ToolGradient(Image image)
	{
		super(image);
		gradient = Gradient.createSimpleGradient(Color.WHITE, Color.BLACK);
		
		selection = image.getSelection();
		shapes = new GradientShapes(this);
		
		maskPaint = new Paint();
		maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
		maskPaint.setColor(Color.argb(160, 208, 208, 208));
		
		pointOuterPaint = new Paint();
		pointOuterPaint.setAntiAlias(true);
		pointOuterPaint.setStyle(Paint.Style.FILL);
		pointOuterPaint.setColor(Color.DKGRAY);
		
		pointInnerPaint = new Paint();
		pointInnerPaint.setAntiAlias(true);
		pointInnerPaint.setStyle(Paint.Style.FILL);
		pointInnerPaint.setColor(Color.WHITE);
		
		layer = image.getSelectedLayer();
		updateSelectionPath();
		
		pointDrawRect = new RectF();
		
		draggingIndex = -1;
		
		shape = shapes.getShape(0);
	}
	
	@Override
	public int getName()
	{
		return R.string.tool_gradient;
	}
	
	@Override
	public int getIcon()
	{
		return R.drawable.ic_tool_gradient_black_24dp;
	}
	
	@Override
	public Class<? extends ToolProperties> getPropertiesFragmentClass()
	{
		return GradientProperties.class;
	}
	
	@Override
	public CoordinateSpace getCoordinateSpace()
	{
		return CoordinateSpace.LAYER_SPACE;
	}
	
	@Override
	public boolean isUsingSnapping()
	{
		return false;
	}
	
	@Override
	public boolean onTouchStart(float x, float y)
	{
		layer = image.getSelectedLayer();
		if(layer == null) return false;
		canvas = image.getSelectedCanvas();
		
		updateSelectionPath();
		updateClipping(canvas);
		
		if(!pointsCreated) startGradientEditing(x, y);
		else startPointsDragging(x, y);
		
		return true;
	}
	
	private void startGradientEditing(float x, float y)
	{
		firstPoint = new PointF(x, y);
		secondPoint = new PointF(x, y);
		
		draggingIndex = 1;
		draggingStart = new PointF(x, y);
		previousPositionOfDraggedPoint = new PointF(x, y);
	}
	
	private void startPointsDragging(float x, float y)
	{
		float distanceToFirst = (float) Math.hypot(firstPoint.x - x, firstPoint.y - y);
		float distanceToSecond = (float) Math.hypot(secondPoint.x - x, secondPoint.y - y);
		
		if(distanceToFirst <= distanceToSecond && distanceToFirst <= MAX_DISTANCE)
		{
			draggingIndex = 0;
			draggingStart = new PointF(x, y);
			previousPositionOfDraggedPoint = new PointF(firstPoint.x, firstPoint.y);
		}
		else if(distanceToSecond < distanceToFirst && distanceToSecond <= MAX_DISTANCE)
		{
			draggingIndex = 1;
			draggingStart = new PointF(x, y);
			previousPositionOfDraggedPoint = new PointF(secondPoint.x, secondPoint.y);
		}
	}
	
	@Override
	public boolean onTouchMove(float x, float y)
	{
		continuePointsDragging(x, y);
		return true;
	}
	
	private void continuePointsDragging(float x, float y)
	{
		if(draggingIndex == -1) return;
		float newX = x - draggingStart.x + previousPositionOfDraggedPoint.x;
		float newY = y - draggingStart.y + previousPositionOfDraggedPoint.y;
		if(draggingIndex == 0) firstPoint.set(newX, newY);
		else if(draggingIndex == 1) secondPoint.set(newX, newY);
	}
	
	@Override
	public boolean onTouchStop(float x, float y)
	{
		continuePointsDragging(x, y);
		
		pointsCreated = true;
		draggingIndex = -1;
		listener.onGradientSet();
		return true;
	}
	
	@Override
	public boolean isImageLimited()
	{
		return false;
	}
	
	@Override
	public boolean doesScreenDraw(boolean layerVisible)
	{
		return layerVisible;
	}
	
	@Override
	public boolean isDrawingOnTop()
	{
		return false;
	}
	
	@Override
	public void onScreenDraw(Canvas canvas)
	{
		layer = image.getSelectedLayer();
		
		canvas.scale(image.getZoom(), image.getZoom());
		canvas.translate(-image.getViewX() + layer.getX(), -image.getViewY() + layer.getY());
		
		canvas.save();
		updateClipping(canvas);
		shape.onScreenDraw(canvas);
		canvas.restore();
		
		if(firstPoint != null) drawPoint(canvas, firstPoint);
		if(secondPoint != null) drawPoint(canvas, secondPoint);
	}
	
	private void drawPoint(Canvas canvas, PointF point)
	{
		float outerSize = POINT_OUTER_RADIUS / image.getZoom();
		float innerSize = POINT_INNER_RADIUS / image.getZoom();
		
		pointDrawRect.left = point.x - outerSize;
		pointDrawRect.top = point.y - outerSize;
		pointDrawRect.right = point.x + outerSize;
		pointDrawRect.bottom = point.y + outerSize;
		canvas.drawOval(pointDrawRect, pointOuterPaint);
		
		pointDrawRect.left = point.x - innerSize;
		pointDrawRect.top = point.y - innerSize;
		pointDrawRect.right = point.x + innerSize;
		pointDrawRect.bottom = point.y + innerSize;
		canvas.drawOval(pointDrawRect, pointInnerPaint);
	}
	
	private void updateSelectionPath()
	{
		selectionPath = new Path(selection.getPath());
		selectionPath.offset(-layer.getX(), -layer.getY());
	}
	
	private void updateClipping(Canvas canvas)
	{
		canvas.clipRect(0, 0, layer.getWidth(), layer.getHeight(), Op.REPLACE);
		if(!selection.isEmpty()) canvas.clipPath(selectionPath, Op.INTERSECT);
	}
	
	void apply()
	{
		shape.applyGradient(canvas);
		cancel();
	}
	
	void cancel()
	{
		firstPoint = null;
		secondPoint = null;
		pointsCreated = false;
		image.updateImage();
	}
	
	@Override
	public void onToolSelected() { }
	
	@Override
	public void onOtherToolSelected()
	{
		cancel();
	}
	
	void setOnGradientEditListener(OnGradientEditListener listener)
	{
		this.listener = listener;
	}
	
	boolean isInEditMode()
	{
		return pointsCreated;
	}
	
	boolean canDrawGradient()
	{
		return firstPoint != null && secondPoint != null;
	}
	
	Gradient getGradient()
	{
		return gradient;
	}
	
	void setGradient(Gradient gradient)
	{
		this.gradient = gradient;
	}
	
	GradientShapes getShapes()
	{
		return shapes;
	}
	
	GradientShape getShape()
	{
		return shape;
	}
	
	void setShape(GradientShape shape)
	{
		this.shape = shape;
		image.updateImage();
	}
	
	PointF getFirstPoint()
	{
		return firstPoint;
	}
	
	PointF getSecondPoint()
	{
		return secondPoint;
	}
	
	int getLayerWidth()
	{
		return layer.getWidth();
	}
	
	int getLayerHeight()
	{
		return layer.getHeight();
	}
}