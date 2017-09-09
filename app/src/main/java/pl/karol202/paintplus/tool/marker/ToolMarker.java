package pl.karol202.paintplus.tool.marker;

import android.graphics.Canvas;
import android.graphics.Rect;
import pl.karol202.paintplus.R;
import pl.karol202.paintplus.color.ColorsSet;
import pl.karol202.paintplus.image.Image;
import pl.karol202.paintplus.tool.StandardTool;
import pl.karol202.paintplus.tool.ToolCoordinateSpace;
import pl.karol202.paintplus.tool.ToolProperties;

public class ToolMarker extends StandardTool
{
	private float size;
	private float opacity;
	private boolean smoothEdge;
	
	private Canvas canvas;
	private ColorsSet colors;
	
	private MarkerAdapterQuadraticPath adapterQuadraticPath;
	
	private Rect dirtyRect;

	public ToolMarker(Image image)
	{
		super(image);
		this.size = 25;
		this.opacity = 1;
		this.smoothEdge = true;
		
		this.colors = image.getColorsSet();
		
		this.adapterQuadraticPath = new MarkerAdapterQuadraticPath(this);
	}
	
	@Override
	public int getName()
	{
		return R.string.tool_marker;
	}
	
	@Override
	public int getIcon()
	{
		return R.drawable.ic_tool_marker_black_24dp;
	}
	
	@Override
	public Class<? extends ToolProperties> getPropertiesFragmentClass()
	{
		return MarkerProperties.class;
	}
	
	@Override
	public ToolCoordinateSpace getCoordinateSpace()
	{
		return ToolCoordinateSpace.LAYER_SPACE;
	}
	
	@Override
	public boolean isUsingSnapping()
	{
		return true;
	}
	
	@Override
	public boolean onTouchStart(float x, float y)
	{
		canvas = image.getSelectedCanvas();
		if(canvas == null) return false;
		layer = image.getSelectedLayer();
		
		updateSelectionPath();
		resetClipping(canvas);
		doLayerAndSelectionClipping(canvas);
		
		getCurrentAdapter().onBeginDraw(x, y);
		
		expandDirtyRectByPoint((int) x, (int) y);
		return true;
	}
	
	@Override
	public boolean onTouchMove(float x, float y)
	{
		getCurrentAdapter().onDraw(x, y);
		
		expandDirtyRectByPoint((int) x, (int) y);
		return true;
	}
	
	@Override
	public boolean onTouchStop(float x, float y)
	{
		getCurrentAdapter().onEndDraw(x, y);
		
		dirtyRect = null;
		return true;
	}
	
	private void expandDirtyRectByPoint(int x, int y)
	{
		if(dirtyRect == null) dirtyRect = new Rect();
		dirtyRect.left = (int) Math.min(dirtyRect.left, x - size);
		dirtyRect.top = (int) Math.min(dirtyRect.top, y - size);
		dirtyRect.right = (int) Math.max(dirtyRect.right, x + size);
		dirtyRect.bottom = (int) Math.max(dirtyRect.bottom, y + size);
	}
	
	@Override
	public boolean providesDirtyRegion()
	{
		return true;
	}
	
	@Override
	public Rect getDirtyRegion()
	{
		return dirtyRect;
	}
	
	@Override
	public void resetDirtyRegion()
	{
		if(dirtyRect != null) dirtyRect.setEmpty();
	}
	
	@Override
	public boolean doesOnLayerDraw(boolean layerVisible)
	{
		return layerVisible;
	}
	
	@Override
	public boolean doesOnTopDraw()
	{
		return false;
	}
	
	@Override
	public ToolCoordinateSpace getOnLayerDrawingCoordinateSpace()
	{
		return ToolCoordinateSpace.LAYER_SPACE;
	}
	
	@Override
	public ToolCoordinateSpace getOnTopDrawingCoordinateSpace()
	{
		return null;
	}
	
	@Override
	public void onLayerDraw(Canvas canvas)
	{
		resetClipping(canvas);
		doLayerAndSelectionClipping(canvas);
		doImageClipping(canvas);
		getCurrentAdapter().onScreenDraw(canvas);
	}
	
	@Override
	public void onTopDraw(Canvas canvas) { }
	
	private MarkerAdapter getCurrentAdapter()
	{
		return adapterQuadraticPath;
	}
	
	float getSize()
	{
		return size;
	}

	void setSize(float size)
	{
		this.size = size;
	}
	
	float getOpacity()
	{
		return opacity;
	}
	
	void setOpacity(float opacity)
	{
		this.opacity = opacity;
	}
	
	boolean isSmoothEdge()
	{
		return smoothEdge;
	}
	
	void setSmoothEdge(boolean smoothEdge)
	{
		this.smoothEdge = smoothEdge;
	}
	
	Canvas getCanvas()
	{
		return canvas;
	}
	
	ColorsSet getColors()
	{
		return colors;
	}
}