package com.rafaelkhan.android.draw;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class Draw extends Activity {

	private PaintView pv;
	protected View layout;
	protected int progress;
	protected Dialog dialog;
	protected float stroke = 6;
	private String fileName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		this.fileName = null;
		super.onCreate(savedInstanceState);
		this.pv = new PaintView(this);
		setContentView(this.pv);
		this.pv.togglePencil(true);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	/*
	 * MENU METHODS
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.paintview_menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.pencil:
			this.pv.togglePencil(true);
			return true;

		case R.id.eraser:
			// erasing enabled
			this.pv.togglePencil(false);
			return true;

		case R.id.stroke:
			this.strokeDialog();
			return true;
			//
		case R.id.colour:
			this.colourPicker();
			return true;

		case R.id.clear:
			this.pv.clear();
			return true;

		case R.id.save:
			this.save();
			return true;

		case R.id.save_as:
			this.saveAs();
			return true;

		case R.id.load:
			this.loadImage();
			return true;

		case R.id.new_file:
			this.newFile();
			return true;
		}
		return true;
	}

	/*
	 * END MENU METHODS
	 */

	/*
	 * Thanks to these guys http://code.google.com/p/android-color-picker/
	 * 
	 * THIS METHOD BRINGS UP A DIALOG THAT ALLOWS THE USER TO SELECT A COLOR
	 */
	public void colourPicker() {
		AmbilWarnaDialog dialog = new AmbilWarnaDialog(Draw.this,
				this.pv.getColor(), new OnAmbilWarnaListener() {
					@Override
					public void onOk(AmbilWarnaDialog dialog, int color) {
						Draw.this.pv.setColor(color);
					}

					@Override
					public void onCancel(AmbilWarnaDialog dialog) {
						// cancel was selected by the user
					}
				});
		dialog.show();
	}

	/*
	 * STROKE SETTING METHODS
	 */
	public void strokeDialog() {
		this.dialog = new Dialog(this);
		this.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		this.layout = inflater.inflate(R.layout.stroke_dialog,
				(ViewGroup) findViewById(R.id.dialog_root_element));

		SeekBar dialogSeekBar = (SeekBar) layout
				.findViewById(R.id.dialog_seekbar);

		dialogSeekBar.setThumbOffset(convertDipToPixels(9.5f));
		dialogSeekBar.setProgress((int) this.stroke * 2);

		this.setTextView(this.layout, String.valueOf(Math.round(this.stroke)));

		dialogSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// herp
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// derp
			}

			@Override
			public void onProgressChanged(SeekBar seekBark, int progress,
					boolean fromUser) {
				Draw.this.progress = progress / 2;
				Draw.this
						.setTextView(Draw.this.layout, "" + Draw.this.progress);

				Button b = (Button) Draw.this.layout
						.findViewById(R.id.dialog_button);
				b.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Draw.this.stroke = Draw.this.progress;
						Draw.this.pv.paint.setStrokeWidth(Draw.this.stroke);
						Draw.this.dialog.dismiss();
					}
				});
			}
		});

		dialog.setContentView(layout);
		dialog.show();
	}

	protected void setTextView(View layout, String s) {
		TextView text = (TextView) layout.findViewById(R.id.stroke_text);
		text.setText(s);
	}

	private int convertDipToPixels(float dip) {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float density = metrics.density;
		return (int) (dip * density);
	}

	/*
	 * END STROKE RELATED METHODS
	 */

	/*
	 * SAVE FILE RELATED METHODS
	 */

	public void save() { // called on save menu
		Log.e("saving1", "a");
		if (this.fileName == null) {
			Log.e("saving2", "a");
			this.saveDialog();
		} else {
			Log.e("saving3", "a");
			this.saveToFile(this.fileName);
		}
	}

	public void saveAs() {
		this.saveDialog();
	}

	public void saveDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Save file...");
		alert.setMessage("File name to save as");
		final EditText input = new EditText(this);
		input.setText("");
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String fname = input.getText().toString();
				String path = Environment.getExternalStorageDirectory()
						.toString();
				File f = new File(path, "draw_images/" + fname + ".jpg");
				if (f.exists()) {
					Draw.this.fileExistsConfirmationDialog(fname);
				} else {
					Draw.this.saveToFile(fname);
				}
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});

		alert.show();
	}

	public void fileExistsConfirmationDialog(final String fname) {
		AlertDialog.Builder alert = new AlertDialog.Builder(Draw.this);
		alert.setTitle("Error");
		alert.setMessage("The file \"" + fname
				+ "\" already exists, do you wish to overwrite it?");
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Draw.this.saveToFile(fname);
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});

		alert.show();
	}

	public void saveToFile(String fname) {
		this.pv.setDrawingCacheEnabled(true);
		this.pv.invalidate();
		String path = Environment.getExternalStorageDirectory().toString();
		OutputStream fOut = null;
		File file = new File(path, "draw_images/" + fname + ".jpg");
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e) {
			Log.e("create noo", e.toString());
		}

		try {
			file.createNewFile();
		} catch (Exception e) {
			Log.e("draw_save", e.toString());
		}

		try {
			fOut = new FileOutputStream(file);
		} catch (Exception e) {
			Log.e("draw_save1", e.toString());
		}

		if (this.pv.getDrawingCache() == null) {
			Log.e("lal", "tis null");
		}

		this.pv.getDrawingCache()
				.compress(Bitmap.CompressFormat.JPEG, 85, fOut);

		try {
			fOut.flush();
			fOut.close();
		} catch (IOException e) {
			Log.e("draw_save1", e.toString());
		}

		@SuppressWarnings("unused")
		SingleMediaScanner sMs = new SingleMediaScanner(this, file);
		this.fileName = fname;
		this.setTitle();

		Toast.makeText(this, "Saved " + this.fileName, 0).show();
	}

	/*
	 * Invoke media scanner
	 */
	private class SingleMediaScanner implements MediaScannerConnectionClient {

		private MediaScannerConnection mMs;
		private File mFile;

		public SingleMediaScanner(Context context, File f) {
			mFile = f;
			mMs = new MediaScannerConnection(context, this);
			mMs.connect();
		}

		@Override
		public void onMediaScannerConnected() {
			mMs.scanFile(mFile.getAbsolutePath(), null);
		}

		@Override
		public void onScanCompleted(String path, Uri uri) {
			mMs.disconnect();
		}

	}

	/*
	 * END SAVE FILE METHODS
	 */

	/*
	 * LOAD IMAGE RELATED METHODS
	 */
	public void loadImage() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"),
				1);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (requestCode == 1) {
				// currImageURI is the global variable I'm using to hold the
				// content:// URI of the image
				Uri currImageURI = data.getData();
				Bitmap bitmap = BitmapFactory.decodeFile(this
						.getRealPathFromURI(currImageURI));

				Display display = getWindowManager().getDefaultDisplay();
				int width = display.getWidth();
				int height = display.getHeight();
				this.pv.bgImage = Bitmap.createScaledBitmap(bitmap, width,
						height, false);
				this.pv.clear();
				this.pv.invalidate();
			}
		}
	}

	public String getRealPathFromURI(Uri contentUri) {
		// can post image
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(contentUri, proj, // Which columns to
														// return
				null, // WHERE clause; which rows to return (all rows)
				null, // WHERE clause selection arguments (none)
				null); // Order-by clause (ascending by name)
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();

		return cursor.getString(column_index);
	}

	/*
	 * END LOAD IMAGE METHODS
	 */

	/*
	 * MISC. METHODS
	 */
	private void newFile() {
		this.fileName = null;
		Display display = getWindowManager().getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		this.pv.bgImage = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		this.pv.clear();
		this.pv.invalidate();
	}

	private void setTitle() {
		if (this.fileName == null) {
			if (this.pv.pencil) {
				setTitle("Draw. - Pencil");
			} else {
				setTitle("Draw. - Eraser");
			}
		} else {
			if (this.pv.pencil) {
				setTitle("Draw. - Pencil - " + this.fileName);
			} else {
				setTitle("Draw. - Eraser - " + this.fileName);
			}
		}
	}

	private class PaintView extends View {

		private Paint paint;
		private Bitmap bmp;
		private Paint bmpPaint;
		private Canvas canvas;
		@SuppressWarnings("unused")
		private Context context;
		private float mX, mY;
		private Path path;
		private static final float TOUCH_TOLERANCE = 0.8f;
		private int colour;
		private Bitmap bgImage; // image that gets loaded
		protected Boolean pencil;

		private PaintView(Context c) {
			super(c);

			setDrawingCacheEnabled(true); // to save images

			this.context = c;
			this.colour = Color.BLACK;

			this.path = new Path();
			this.bmpPaint = new Paint();
			this.paint = new Paint();
			this.paint.setAntiAlias(true);
			this.paint.setDither(true);
			this.paint.setColor(this.colour);
			this.paint.setStyle(Paint.Style.STROKE);
			this.paint.setStrokeJoin(Paint.Join.ROUND);
			this.paint.setStrokeCap(Paint.Cap.ROUND);
			this.paint.setStrokeWidth(6);
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			this.bgImage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			this.bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			this.canvas = new Canvas(this.bmp);
		}

		private void touchStart(float x, float y) {
			this.path.reset();
			this.path.moveTo(x, y);
			this.mX = x;
			this.mY = y;
		}

		private void touchUp() {
			this.path.lineTo(mX, mY);
			// commit the path to our offscreen
			this.canvas.drawPath(this.path, paint);
			// kill this so we don't double draw
			this.path.reset();
		}

		private void touchMove(float x, float y) {
			float dx = Math.abs(x - this.mX);
			float dy = Math.abs(y - this.mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
				// draws a quadratic curve
				this.path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
				mX = x;
				mY = y;
			}
		}

		@Override
		public boolean onTouchEvent(MotionEvent e) {
			float x = e.getX();
			float y = e.getY();

			switch (e.getAction()) {
			case MotionEvent.ACTION_DOWN:
				this.touchStart(x, y);
				this.touchMove(x + 0.8f, y + 0.8f);
				invalidate();
				break;
			case MotionEvent.ACTION_MOVE:
				this.touchMove(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_UP:
				this.touchUp();
				invalidate();
				break;
			}
			return true;
		}

		// Called on invalidate();
		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(Color.WHITE);
			canvas.drawBitmap(this.bgImage, 0, 0, this.bmpPaint);
			canvas.drawBitmap(this.bmp, 0, 0, this.bmpPaint);
			canvas.drawPath(this.path, this.paint);
		}

		/*
		 * Menu called methods
		 */
		protected void togglePencil(Boolean b) {
			if (b) { // set pencil
				paint.setXfermode(null);
				this.pencil = true;
			} else { // set eraser
				paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
				this.pencil = false;
			}
			Draw.this.setTitle();
		}

		public void setColor(int c) {
			this.paint.setColor(c);
			this.colour = c;
		}

		protected int getColor() {
			return this.colour;
		}

		protected void clear() {
			this.path = new Path(); // empty path
			this.canvas.drawColor(Color.WHITE);
			if (this.bgImage != null) {
				this.canvas.drawBitmap(this.bgImage, 0, 0, null);
			}
			this.invalidate();
		}
	}
}