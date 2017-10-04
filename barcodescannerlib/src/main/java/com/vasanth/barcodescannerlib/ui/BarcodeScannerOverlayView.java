package com.vasanth.barcodescannerlib.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.vasanth.barcodescannerlib.R;


/**
 * Barcode Scanner Overlay View.
 * <p>
 * 1. This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation
 *
 * @author Vasanth
 */
public class BarcodeScannerOverlayView extends View {

    private static final String TAG = "BS Overlay View";
    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
    private static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080
    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 80L;

    private Context context;
    private Rect viewFinderRect;
    private int viewFinderMiddleY;
    private Point screenResolution;
    private final Paint paint;
    private final int maskColor;
    private final int laserColor;
    private int scannerAlpha = 0;

    /**
     * Constructor.
     * <p>
     * 1. This constructor is used when the class is built from an XML resource.
     *
     * @param context Context.
     * @param attrs   Attribute Set.
     */
    public BarcodeScannerOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.barcodeScanner_overlayMask);
        laserColor = resources.getColor(R.color.barcodeScanner_overlayLaser);
    }

    /**
     * On Draw.
     * <p>
     * 1. Used to draw the overlay view.
     *
     * @param canvas Canvas.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // Get view finder frame.
        Rect frame = getFramingRect();

        // Get canvas height & width.
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        // Draw a red "laser scanner" line through the middle to show decoding is active
        paint.setColor(laserColor);
        paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
        viewFinderMiddleY = frame.height() / 2 + frame.top;
        canvas.drawRect(frame.left + 2, viewFinderMiddleY - 1, frame.right - 1, viewFinderMiddleY + 2, paint);

        // Request another update at the animation interval, but only repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(ANIMATION_DELAY,
                frame.left,
                frame.top,
                frame.right,
                frame.bottom);
    }

    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * barcode. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     *
     * @return The rectangle to draw on screen in window coordinates.
     */
    private synchronized Rect getFramingRect() {
        if (viewFinderRect == null) {
            Point screenResolution = getScreenResolution();
            int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
            int height = findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            viewFinderRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
        }
        return viewFinderRect;
    }

    /**
     * Used to get screen resolution of the device.
     *
     * @return Screen Resolution in Point (Width & Height).
     */
    public Point getScreenResolution() {
        if (screenResolution == null) {
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point theScreenResolution = new Point();
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR2) {
                display.getSize(theScreenResolution);
            } else {
                theScreenResolution.x = display.getWidth();
                theScreenResolution.y = display.getHeight();
            }
            screenResolution = theScreenResolution;
        }
        return screenResolution;
    }

    /**
     * Used to get desired resolution (width or height) for view finder camera preview.
     *
     * @param resolution Screen Resolution (Width or Height).
     * @param hardMin    Minimum Resolution (Width or Height).
     * @param hardMax    Maximum Resolution (Width or Height).
     * @return Desired Resolution.
     */
    private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 5 * resolution / 8; // Target 5/8 of each dimension
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }

    /**
     * Get View Finder Rectangle.
     *
     * @return View Finder Rectangle.
     */
    public Rect getViewFinderRect() {
        return viewFinderRect;
    }

    /**
     * Get View Finder Middle Y.
     *
     * @return View Finder Middle Y.
     */
    public int getViewFinderMiddleY() {
        return viewFinderMiddleY;
    }
}
