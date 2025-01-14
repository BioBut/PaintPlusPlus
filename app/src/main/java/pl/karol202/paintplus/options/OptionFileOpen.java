package pl.karol202.paintplus.options;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;
import pl.karol202.paintplus.AsyncManager;
import pl.karol202.paintplus.R;
import pl.karol202.paintplus.activity.ActivityPaint;
import pl.karol202.paintplus.activity.ActivityResultListener;
import pl.karol202.paintplus.file.ActivityFileOpen;
import pl.karol202.paintplus.file.ImageLoaderDialog;
import pl.karol202.paintplus.image.Image;
import pl.karol202.paintplus.recent.OnFileEditListener;

import static android.app.Activity.RESULT_OK;

public class OptionFileOpen extends Option implements ActivityResultListener, ImageLoaderDialog.OnImageLoadListener
{
	private static final int REQUEST_OPEN_FILE = 1;
	
	private ActivityPaint activity;
	private OnFileEditListener listener;
	private AsyncManager asyncManager;
	private String filePath;
	
	public OptionFileOpen(ActivityPaint activity, Image image, AsyncManager asyncManager, OnFileEditListener listener)
	{
		super(activity, image);
		this.activity = activity;
		this.asyncManager = asyncManager;
		this.listener = listener;
	}
	
	@Override
	public void execute()
	{
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
		dialogBuilder.setTitle(R.string.dialog_are_you_sure);
		dialogBuilder.setMessage(R.string.dialog_unsaved_changes);
		dialogBuilder.setPositiveButton(R.string.dialog_open_file_positive, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				startFileOpenActivity();
			}
		});
		dialogBuilder.setNegativeButton(R.string.cancel, null);
		dialogBuilder.show();
	}
	
	public void executeWithoutAsking()
	{
		startFileOpenActivity();
	}
	
	private void startFileOpenActivity()
	{
		activity.registerActivityResultListener(REQUEST_OPEN_FILE, this);
		
		Intent intent = new Intent(context, ActivityFileOpen.class);
		activity.startActivityForResult(intent, REQUEST_OPEN_FILE);
	}
	
	public void openFile(String filePath)
	{
		this.filePath = filePath;
		new ImageLoaderDialog(context, asyncManager, this).loadBitmapAndAskForScalingIfTooBig(filePath);
	}
	
	@Override
	public void onActivityResult(int resultCode, Intent data)
	{
		activity.unregisterActivityResultListener(REQUEST_OPEN_FILE);
		if(resultCode != RESULT_OK) return;
		String filePath = data.getStringExtra("filePath");
		openFile(filePath);
	}
	
	@Override
	public void onImageLoaded(Bitmap bitmap)
	{
		if(bitmap == null) Toast.makeText(context, R.string.message_cannot_open_file, Toast.LENGTH_SHORT).show();
		else
		{
			image.openImage(bitmap);
			image.setLastPath(filePath);
			image.centerView();
			
			if(listener != null) listener.onFileEdited(filePath, bitmap);
		}
	}
}