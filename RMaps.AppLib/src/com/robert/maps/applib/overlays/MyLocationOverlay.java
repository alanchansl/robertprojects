package com.robert.maps.applib.overlays;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.TypeConverter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.robert.maps.applib.R;
import com.robert.maps.applib.utils.Ut;
import com.robert.maps.applib.view.TileView;
import com.robert.maps.applib.view.TileView.OpenStreetMapViewProjection;
import com.robert.maps.applib.view.TileViewOverlay;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class MyLocationOverlay extends TileViewOverlay {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected final Paint mPaint = new Paint();

	protected Bitmap PERSON_ICON2 = null;
	private Bitmap mArrow = null;

	private Context mCtx;
	protected GeoPoint mLocation;
	private float mAccuracy;
	private int mPrefAccuracy;
	private float mBearing;
	private float mSpeed;
	private int METER_IN_PIXEL = 156412;
	private Paint mPaintAccurasyFill;
	private Paint mPaintAccurasyBorder;
	private Paint mPaintLineToGPS;
	private boolean mNeedCrosshair;
	private boolean mNeedCircleDistance;
	private final Paint mPaintCross = new Paint();
	private final static int mCrossSize = 7;
	private Location mLoc;

	private boolean mLineToGPS;
	private int mUnits;
	private TextView mLabelVw;

	private int mZoomLevel;

    private static final int SCALE[][] = {{25000000,15000000,8000000,4000000,2000000,1000000,500000,250000,100000,50000,25000,15000,8000,4000,2000,1000,500,250,100,50}
    ,{15000,8000,4000,2000,1000,500,250,100,50,25,15,8,4,2,1,3000,1500,500,250,100}};
    private static int EQUATOR_M = 40075676;
    private static int EQUATOR_ML = 24902;
    private static int EQUATOR_FT = 131481877;

    private double mTouchScale;
	private int mWidth;
	private int mWidth2;
	private String mDist;

	// ===========================================================
	// Constructors
	// ===========================================================

	public MyLocationOverlay(final Context ctx){
		mCtx = ctx.getApplicationContext();

		mPaintAccurasyFill = new Paint();
		mPaintAccurasyFill.setAntiAlias(true);
		mPaintAccurasyFill.setStrokeWidth(2);
		mPaintAccurasyFill.setStyle(Paint.Style.FILL);
		mPaintAccurasyFill.setColor(0x4490B8D8);

		mPaintAccurasyBorder = new Paint(mPaintAccurasyFill);
		mPaintAccurasyBorder.setStyle(Paint.Style.STROKE);
		mPaintAccurasyBorder.setColor(0xFF90B8D8);
		
		mPaintLineToGPS = new Paint(mPaintAccurasyFill);
		mPaintLineToGPS.setColor(ctx.getResources().getColor(R.color.line_to_gps));
		
		mPaintCross.setAntiAlias(true);
		mPaintCross.setStyle(Paint.Style.STROKE);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		mPrefAccuracy = Integer.parseInt(pref.getString("pref_accuracy", "1").replace("\"", ""));
		mNeedCrosshair = pref.getBoolean("pref_crosshair", true);
		mNeedCircleDistance = pref.getBoolean("pref_circle_distance", true);
		mLineToGPS = pref.getBoolean("pref_line_gps", false);
		mUnits = Integer.parseInt(pref.getString("pref_units", "0"));
		
		if(mLineToGPS) {
			mLabelVw = (TextView) LayoutInflater.from(ctx).inflate(R.layout.label_map, null);
			mLabelVw.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	private boolean getPersonIcon(){
		if(PERSON_ICON2 == null)
			try {
				this.PERSON_ICON2 = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.person);
			} catch (Exception e) {
				PERSON_ICON2 = null;
			} catch (OutOfMemoryError e) {
				PERSON_ICON2 = null;
			}

		return PERSON_ICON2 == null ? false : true;
	}

	private boolean getArrowIcon(){
		if(mArrow == null)
			try {
				this.mArrow = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.arrow);
			} catch (Exception e) {
				mArrow = null;
			} catch (OutOfMemoryError e) {
				mArrow = null;
			}

		return mArrow == null ? false : true;
	}
	
	public GeoPoint getLastGeoPoint() {
		return mLocation;
	}
	
	public Location getLastLocation() {
		return mLoc;
	}

	public void setLocation(final Location loc){
		this.mLocation = TypeConverter.locationToGeoPoint(loc);
		this.mAccuracy = loc.getAccuracy();
		this.mBearing = loc.getBearing();
		this.mSpeed = loc.getSpeed();
		mLoc = loc;
	}

	public void setLocation(final GeoPoint geopoint){
		this.mLocation = geopoint;
		this.mAccuracy = 0;
		this.mBearing = 0;
		this.mSpeed = 0;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onDrawFinished(Canvas c, TileView osmv) {
		return;
	}

	@Override
	public void onDraw(final Canvas c, final TileView osmv) {
		if(this.mLocation != null){
			final OpenStreetMapViewProjection pj = osmv.getProjection();
			final Point screenCoords = new Point();
			pj.toPixels(this.mLocation, screenCoords);
			

	        if(mZoomLevel != osmv.getZoomLevel() || mTouchScale != osmv.mTouchScale){
	        	mZoomLevel = osmv.getZoomLevel();
	        	mTouchScale = osmv.mTouchScale;

				final int dist = SCALE[mUnits][Math.max(0, Math.min(19, mZoomLevel + 1 + (int)(mTouchScale > 1 ? Math.round(mTouchScale)-1 : -Math.round(1/mTouchScale)+1)))];
	    		if(mUnits == 0){
		    		mWidth = (int) ((double)dist*mTouchScale*256*(1<<(mZoomLevel + 1))/EQUATOR_M);

		    		if(dist > 999)
		    			mDist = ""+(dist/1000)+" km";
		    		else
		    			mDist = ""+dist+" m";
	    		} else if(mZoomLevel < 15){
		    		mWidth = (int) ((double)dist*mTouchScale*256*2*(1<<(mZoomLevel + 1))/EQUATOR_ML);
		    		mDist = ""+dist+" ml";
	    		} else {
		    		mWidth = (int) ((double)dist*mTouchScale*256*2*(1<<(mZoomLevel + 1))/EQUATOR_FT);
		    		mDist = ""+dist+" ft";
	    		}

	    		mWidth2 = (int) mWidth / 2;
	        }
	        
	        if(mNeedCircleDistance) {
		        c.drawCircle(screenCoords.x, screenCoords.y, mWidth, this.mPaintCross);
		        c.drawCircle(screenCoords.x, screenCoords.y, mWidth * 2, this.mPaintCross);
		        c.drawCircle(screenCoords.x, screenCoords.y, mWidth * 3, this.mPaintCross);
		        c.drawCircle(screenCoords.x, screenCoords.y, mWidth * 4, this.mPaintCross);
	        }
			
			if (mPrefAccuracy != 0
					&& mSpeed <= 0.278
					&& ((mAccuracy > 0 && mPrefAccuracy == 1) || (mPrefAccuracy > 1 && mAccuracy >= mPrefAccuracy))) {
				int PixelRadius = (int) (osmv.mTouchScale * mAccuracy / ((float)METER_IN_PIXEL / (1 << osmv.getZoomLevel())));
				c.drawCircle(screenCoords.x, screenCoords.y, PixelRadius, mPaintAccurasyFill);
				c.drawCircle(screenCoords.x, screenCoords.y, PixelRadius, mPaintAccurasyBorder);
			}
			
			if(mLineToGPS) {
				c.drawLine(screenCoords.x, screenCoords.y, osmv.getWidth() / 2, osmv.getHeight() / 2, mPaintLineToGPS);
				final GeoPoint geo = pj.fromPixels(osmv.getWidth() / 2, osmv.getHeight() / 2);
				final float dist = this.mLocation.distanceTo(geo);
				final String lbl = Ut.formatDistance(mCtx, dist, mUnits); 
				
				mLabelVw.setText(lbl);
				mLabelVw.measure(0, 0);
				mLabelVw.layout(0, 0, mLabelVw.getMeasuredWidth(), mLabelVw.getMeasuredHeight());
				c.save();
				c.translate(osmv.getWidth() / 2 - (screenCoords.x < osmv.getWidth() / 2 ? 0 : mLabelVw.getMeasuredWidth()), osmv.getHeight() / 2);
				mLabelVw.draw(c);
				c.restore();
			}

			c.save();
			if (mSpeed <= 0.278) {
				c.rotate(osmv.getBearing(), screenCoords.x, screenCoords.y);
				if(getPersonIcon()){
					c.drawBitmap(PERSON_ICON2, screenCoords.x - (int)(PERSON_ICON2.getWidth()/2), screenCoords.y - (int)(PERSON_ICON2.getHeight() / 2), mPaint);
				};
			} else {
				if(getArrowIcon()){
					c.rotate(mBearing, screenCoords.x, screenCoords.y);
					c.drawBitmap(mArrow, screenCoords.x - (int)(mArrow.getWidth()/2), screenCoords.y - (int)(mArrow.getHeight() / 2), mPaint);
				}
			}
			c.restore();

		}
		
		final int x = osmv.getWidth() / 2;
		final int y = osmv.getHeight() / 2;

		if(mNeedCrosshair){
			c.drawLine(x - mCrossSize, y, x + mCrossSize, y, this.mPaintCross);
			c.drawLine(x, y - mCrossSize, x, y + mCrossSize, this.mPaintCross);
		}
		
	}

}
