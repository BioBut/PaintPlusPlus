package pl.karol202.paintplus.options;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.widget.Toast;
import pl.karol202.paintplus.R;
import pl.karol202.paintplus.activity.ActivityPaint;
import pl.karol202.paintplus.file.ImageLoader;
import pl.karol202.paintplus.image.Image;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Activity.RESULT_OK;
import static android.os.Environment.DIRECTORY_PICTURES;
import static android.provider.MediaStore.EXTRA_OUTPUT;

public class OptionFileCapturePhoto extends Option implements ActivityPaint.ActivityResultListener
{
	private static final int REQUEST_CAPTURE_PHOTO = 0;
	
	private ActivityPaint activity;
	private File photoFile;
	
	public OptionFileCapturePhoto(ActivityPaint activity, Image image)
	{
		super(activity, image);
		this.activity = activity;
		this.activity.registerActivityResultListener(REQUEST_CAPTURE_PHOTO, this);
	}
	
	@Override
	public void execute()
	{
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if(intent.resolveActivity(activity.getPackageManager()) == null)
			throw new RuntimeException("Cannot resolve camera activity.");
		
		try
		{
			photoFile = createPhotoFile();
		}
		catch(IOException ex)
		{
			throw new RuntimeException("Cannot create temporary file for photo.", ex);
		}
		
		Uri photoUri = FileProvider.getUriForFile(context, "pl.karol202.paintplus", photoFile);
		intent.putExtra(EXTRA_OUTPUT, photoUri);
		activity.startActivityForResult(intent, REQUEST_CAPTURE_PHOTO);
	}
	
	private File createPhotoFile() throws IOException
	{
		String dateString = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss").format(new Date());
		String fileName = "CAPTURED_" + dateString;
		File directory = context.getExternalFilesDir(DIRECTORY_PICTURES);
		photoFile = File.createTempFile(fileName, ".jpeg", directory);
		return photoFile;
	}
	
	@Override
	public void onActivityResult(int resultCode, Intent data)
	{
		activity.unregisterActivityResultListener(REQUEST_CAPTURE_PHOTO);
		if(resultCode != RESULT_OK) return;
		
		Bitmap bitmap = ImageLoader.openBitmap(photoFile.getAbsolutePath());
		if(bitmap == null) Toast.makeText(context, R.string.message_cannot_open_file, Toast.LENGTH_SHORT).show();
		else
		{
			image.openImage(bitmap);
			image.centerView();
		}
		
		photoFile.delete();
		
	}
}