package pl.karol202.paintplus.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import pl.karol202.paintplus.R;
import pl.karol202.paintplus.activity.ActivityPaint;

import java.util.ArrayList;
import java.util.HashMap;

public class LayersAdapter extends RecyclerView.Adapter<LayerViewHolder>
{
	private final String DUPLICATE_INDICATOR;
	
	private Context context;
	private Image image;
	private ArrayList<Layer> layers;
	
	private HashMap<Integer, LayerViewHolder> viewHolders;
	private LayerHandle layerHandle;
	
	public LayersAdapter(ActivityPaint activity)
	{
		this.context = activity;
		this.viewHolders = new HashMap<>();
		this.layerHandle = new LayerHandle(activity, this);
		
		DUPLICATE_INDICATOR = context.getString(R.string.duplicate);
	}
	
	@Override
	public LayerViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		View view = LayoutInflater.from(context).inflate(R.layout.item_layer, parent, false);
		return new LayerViewHolder(this, view);
	}
	
	@Override
	public void onBindViewHolder(LayerViewHolder holder, int position)
	{
		holder.bind(layers.get(position));
		viewHolders.put(position, holder);
	}
	
	@Override
	public int getItemCount()
	{
		return layers.size();
	}
	
	public Context getContext()
	{
		return context;
	}
	
	public Image getImage()
	{
		return image;
	}
	
	public void setImage(Image image)
	{
		this.image = image;
		this.layers = image.getLayers();
		notifyDataSetChanged();
	}
	
	public HashMap<Integer, LayerViewHolder> getViewHolders()
	{
		return viewHolders;
	}
	
	public LayerHandle getLayerHandle()
	{
		return layerHandle;
	}
	
	public void moveLayer(int layerId, int target)
	{
		Layer selected = image.getSelectedLayer();
		
		Layer layer = layers.remove(layerId);
		layers.add(target, layer);
		
		image.selectLayer(layers.indexOf(selected));
		image.updateImage();
	}
	
	public void duplicateLayer(Layer layer)
	{
		int layerIndex = layers.indexOf(layer);
		String newName = layer.getName() + DUPLICATE_INDICATOR;
		Layer newLayer = new Layer(layer.getX(), layer.getY(), layer.getWidth(), layer.getHeight(), newName, Color.BLACK);
		Bitmap newBitmap = Bitmap.createBitmap(layer.getBitmap());
		newLayer.setBitmap(newBitmap);
		image.addLayer(newLayer, layerIndex);
		image.selectLayer(layerIndex);
	}
}