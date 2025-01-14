package pl.karol202.paintplus.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import pl.karol202.paintplus.AppDataFragment;
import pl.karol202.paintplus.AsyncManager;
import pl.karol202.paintplus.PaintView;
import pl.karol202.paintplus.R;
import pl.karol202.paintplus.activity.PermissionRequest.PermissionGrantListener;
import pl.karol202.paintplus.image.Image;
import pl.karol202.paintplus.options.OptionFileOpen;
import pl.karol202.paintplus.recent.OnFileEditListener;
import pl.karol202.paintplus.recent.RecentImageCreator;
import pl.karol202.paintplus.settings.ActivitySettings;
import pl.karol202.paintplus.tool.Tool;
import pl.karol202.paintplus.tool.Tools;
import pl.karol202.paintplus.util.GraphicsHelper;

import java.util.HashMap;

public class ActivityPaint extends AppCompatActivity implements PermissionRequest.PermissionGrantingActivity
{
	public static final String PATH_KEY = "path";
	public static final String OPEN_KEY = "open";
	
	private ActivityPaintActions actions;
	private ActivityPaintDrawers drawers;
	private ActivityPaintLayers layers;
	
	private View decorView;
	private FragmentManager fragments;
	private HashMap<Integer, ActivityResultListener> resultListeners;
	private HashMap<Integer, PermissionGrantListener> permissionListeners;
	private AsyncManager asyncManager;
	private AppDataFragment dataFragment;
	private ActionBar actionBar;
	private String initPath;
	private boolean openFile;
	private RecentImageCreator recentImageCreator;

	private ViewGroup mainContainer;
	private Toolbar toolbar;
	private PaintView paintView;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		readArguments(getIntent().getExtras());
		GraphicsHelper.init(this);
		
		actions = new ActivityPaintActions(this);
		drawers = new ActivityPaintDrawers(this);
		layers = new ActivityPaintLayers(this);
		
		setContentView(R.layout.activity_paint);
		decorView = getWindow().getDecorView();
		decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
		{
			@Override
			public void onSystemUiVisibilityChange(int visibility)
			{
				initSystemUIVisibility();
			}
		});
		initSystemUIVisibility();
		
		fragments = getSupportFragmentManager();
		resultListeners = new HashMap<>();
		permissionListeners = new HashMap<>();
		asyncManager = new AsyncManager(this);
		recentImageCreator = new RecentImageCreator(this);
		
		mainContainer = findViewById(R.id.main_container);
		
		toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		actionBar = getSupportActionBar();
		if(actionBar == null) throw new RuntimeException("Cannot set action bar of activity.");
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);
		
		paintView = findViewById(R.id.paint_view);
		
		drawers.initDrawers();
		layers.initLayers();
		
		restoreInstanceState(savedInstanceState);
	}
	
	private void readArguments(Bundle bundle)
	{
		if(bundle == null) return;
		initPath = bundle.getString(PATH_KEY);
		openFile = initPath == null && bundle.getBoolean(OPEN_KEY, false);
	}
	
	private void initSystemUIVisibility()
	{
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) initSystemUIVisibilityKitkat();
		else initSystemUIVisibilityPreKitkat();
	}
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void initSystemUIVisibilityKitkat()
	{
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
									  | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
									  | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
									  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
									  | View.SYSTEM_UI_FLAG_FULLSCREEN
									  | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	}
	
	private void initSystemUIVisibilityPreKitkat()
	{
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
	}
	
	private String makeTitle(Bundle savedInstanceState)
	{
		if(savedInstanceState != null) return savedInstanceState.getString("title");
		else return null;
	}
	
	private void restoreInstanceState(Bundle state)
	{
		if(state != null) super.onRestoreInstanceState(state);
		dataFragment = (AppDataFragment) fragments.findFragmentByTag(AppDataFragment.TAG);
		if(dataFragment == null) createNewDataFragment();
	}
	
	private void createNewDataFragment()
	{
		dataFragment = new AppDataFragment();
		dataFragment.setAsyncManager(asyncManager);
		FragmentTransaction transaction = fragments.beginTransaction();
		transaction.add(dataFragment, AppDataFragment.TAG);
		transaction.commit();
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		
		setTitle(makeTitle(savedInstanceState));
		paintView.init(this);
		layers.postInitLayers();
		drawers.postInitDrawers();
		
		loadImageIfPathIsPresent();
		selectImageToOpenIfNeeded();
	}
	
	private void loadImageIfPathIsPresent()
	{
		if(initPath != null) new OptionFileOpen(this, getImage(), asyncManager, recentImageCreator).openFile(initPath);
	}
	
	private void selectImageToOpenIfNeeded()
	{
		if(openFile) new OptionFileOpen(this, getImage(), asyncManager, recentImageCreator).executeWithoutAsking();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		paintView.updatePreferences();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putString("title", toolbar.getTitle().toString());
		getIntent().putExtra("path", (String) null);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		GraphicsHelper.destroy();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		if(hasFocus) initSystemUIVisibility();
		layers.updateView();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		actions.inflateMenu(menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		actions.prepareMenu(menu);
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return actions.handleAction(item) || super.onOptionsItemSelected(item);
	}
	
	void showSettingsActivity()
	{
		Intent intent = new Intent(this, ActivitySettings.class);
		startActivity(intent);
	}
	
	public void registerActivityResultListener(int requestCode, ActivityResultListener listener)
	{
		if(resultListeners.containsKey(requestCode))
			throw new RuntimeException("requestCode is already used: " + requestCode);
		resultListeners.put(requestCode, listener);
	}
	
	public void unregisterActivityResultListener(int requestCode)
	{
		if(!resultListeners.containsKey(requestCode))
			throw new RuntimeException("requestCode isn't registered yet: " + requestCode);
		resultListeners.remove(requestCode);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if(!resultListeners.containsKey(requestCode)) return;
		resultListeners.get(requestCode).onActivityResult(resultCode, data);
	}
	
	@Override
	public void registerPermissionGrantListener(int requestCode, PermissionGrantListener listener)
	{
		if(permissionListeners.containsKey(requestCode))
			throw new RuntimeException("requestCode is already used: " + requestCode);
		permissionListeners.put(requestCode, listener);
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if(!permissionListeners.containsKey(requestCode)) return;
		if(grantResults[0] == PackageManager.PERMISSION_GRANTED) permissionListeners.get(requestCode).onPermissionGrant();
		permissionListeners.remove(requestCode);
	}
	
	boolean isAnyDrawerOpen()
	{
		return drawers.isAnyDrawerOpen();
	}
	
	void togglePropertiesDrawer()
	{
		drawers.togglePropertiesDrawer();
	}
	
	void toggleLayersSheet()
	{
		layers.toggleLayersSheet();
	}
	
	void closeLayersSheet()
	{
		layers.closeLayersSheet();
	}
	
	public void setScrollingBlocked(boolean blocked)
	{
		layers.setScrollingBlocked(blocked);
	}
	
	public void setTitle(String title)
	{
		if(getTool() == null) title = getString(R.string.activity_main);
		else if(title == null) title = getString(getTool().getName());
		actionBar.setTitle(title);
	}
	
	public void updateLayersPreview()
	{
		layers.updateData();
	}
	
	public DisplayMetrics getDisplayMetrics()
	{
		return getResources().getDisplayMetrics();
	}
	
	AsyncManager getAsyncManager()
	{
		return asyncManager;
	}
	
	public Image getImage()
	{
		if(dataFragment == null) return null;
		return dataFragment.getImage();
	}
	
	public Tools getTools()
	{
		if(dataFragment == null) return null;
		return dataFragment.getTools();
	}
	
	public Tool getTool()
	{
		if(dataFragment == null) return null;
		return dataFragment.getCurrentTool();
	}
	
	public void setTool(Tool tool)
	{
		if(dataFragment == null) return;
		dataFragment.setCurrentTool(tool);
		paintView.onImageChanged();
	}
	
	OnFileEditListener getFileEditListener()
	{
		return recentImageCreator;
	}
	
	public ViewGroup getMainContainer()
	{
		return mainContainer;
	}
	
	public Toolbar getToolbar()
	{
		return toolbar;
	}
	
	PaintView getPaintView()
	{
		return paintView;
	}
}