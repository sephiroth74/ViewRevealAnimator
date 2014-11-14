package it.sephiroth.android.library.viewrevealanimator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;

/**
 * Created by alessandro crugnola on 14/11/14.
 */
public class ViewRevealAnimator extends FrameLayout {
    public interface OnViewChangedListener {
        void onViewChanged(int previousIndex, int currentIndex);
    }

    public interface onViewAnimationListener {
        void onViewAnimationStarted(int previousIndex, int currentIndex);

        void onViewAnimationCompleted(int previousIndex, int currentIndex);
    }

    private static final String TAG = "ViewRevealAnimator";
    private static final boolean DBG = false;
    int mWhichChild = 0;
    int mPreviousChild = -1;
    boolean mFirstTime = true;
    boolean mAnimateFirstTime = true;
    boolean mAnimationsEnabled = true;
    boolean mUseFallbackAnimations = false;
    int mAnimationDuration;
    // fallback animators for api < 21
    Animation mInAnimation;
    Animation mOutAnimation;
    // default animator for api >= 21
    Object mAnimator;
    Object mInterpolator;
    OnViewChangedListener mViewChangedListener;
    onViewAnimationListener mViewAnimationListener;

    public ViewRevealAnimator(Context context) {
        super(context);
        initViewAnimator(context, null);
    }

    public ViewRevealAnimator(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewRelealAnimator);

        int resourceIn = a.getResourceId(R.styleable.ViewRelealAnimator_android_inAnimation, 0);
        int resourceOut = a.getResourceId(R.styleable.ViewRelealAnimator_android_outAnimation, 0);

        setAnimationFallback(context, resourceIn, resourceOut);

        boolean flag = a.getBoolean(R.styleable.ViewRelealAnimator_android_animateFirstView, true);
        setAnimateFirstView(flag);

        flag = a.getBoolean(R.styleable.ViewRelealAnimator_vra_animationsEnabled, true);
        setAnimaionsEnabled(flag);

        flag = a.getBoolean(R.styleable.ViewRelealAnimator_vra_alwaysUseFallbackAnimations, false);
        setAlwaysUseFallbackAnimations(flag);

        int animationDuration = a.getInteger(R.styleable.ViewRelealAnimator_android_animationDuration, 300);
        setAnimationDuration(animationDuration);

        if (Build.VERSION.SDK_INT >= 21) {
            int resID =
                a.getResourceId(R.styleable.ViewRelealAnimator_android_interpolator, android.R.interpolator.accelerate_decelerate);
            mInterpolator = AnimationUtils.loadInterpolator(context, resID);
            Log.v(TAG, "interpolator: " + mInterpolator);
        }

        a.recycle();
        initViewAnimator(context, attrs);
    }

    private void initViewAnimator(Context context, AttributeSet attrs) {
        if (attrs == null) {
            setMeasureAllChildren(true);
            return;
        }

        final TypedArray a = context.obtainStyledAttributes(
            attrs,
            R.styleable.ViewRelealAnimator);
        final boolean measureAllChildren = a.getBoolean(
            R.styleable.ViewRelealAnimator_android_measureAllChildren, true);
        setMeasureAllChildren(measureAllChildren);
        a.recycle();

    }

    @Override
    public void setMeasureAllChildren(final boolean measureAll) {
        super.setMeasureAllChildren(measureAll);
    }

    public void setOnViewChangedListener(OnViewChangedListener listener) {
        mViewChangedListener = listener;
    }

    public void setOnViewAnimationListener(onViewAnimationListener listener) {
        mViewAnimationListener = listener;
    }

    public void setDisplayedChild(int whichChild, @Nullable Point origin) {
        if (DBG) {
            Log.i(TAG, "setDisplayedChild, previous: " + mPreviousChild + ", which: " + mWhichChild + ", next: " + whichChild);
        }

        if (isAnimating()) {
            Log.w(TAG, "View is animating. No animations allowed now.");
            return;
        }

        mPreviousChild = mWhichChild;
        mWhichChild = whichChild;

        if (whichChild >= getChildCount()) {
            mWhichChild = 0;
        } else if (whichChild < 0) {
            mWhichChild = getChildCount() - 1;
        }
        boolean hasFocus = getFocusedChild() != null;
        showOnly(mPreviousChild, mWhichChild, origin);
        if (hasFocus) {
            requestFocus(FOCUS_FORWARD);
        }
    }

    @SuppressWarnings ("unused")
    public int getDisplayedChild() {
        return mWhichChild;
    }

    public void showNext(@Nullable Point origin) {
        setDisplayedChild(mWhichChild + 1, origin);
    }

    public void showPrevious(@Nullable Point origin) {
        setDisplayedChild(mWhichChild - 1, origin);
    }

    void showOnly(int previousChild, int childIndex, boolean animate, Point point) {
        if (DBG) {
            Log.i(TAG, "showOnly: " + previousChild + " >> " + childIndex + ", animate: " + animate);
        }

        mFirstTime = false;

        if (!animate) {
            showOnlyNoAnimation(previousChild, childIndex);
            return;
        }

        if (!getUseFallbackAnimations()) {
            showOnly21(previousChild, childIndex, point);
        } else {
            showOnly19(previousChild, childIndex);
        }
    }

    @TargetApi (21)
    protected void showOnly21(final int previousChild, final int childIndex, final Point point) {
        if (DBG) {
            Log.i(TAG, "showOnly21: " + previousChild + " >> " + childIndex);
        }

        final boolean isReveal = childIndex > previousChild ? true : false;
        if (DBG) {
            Log.v(TAG, "is reveal: " + isReveal);
        }

        // previously invisible view
        final View previousView = getChildAt(previousChild);
        final View nextView = getChildAt(childIndex);
        final View targetView = isReveal ? nextView : previousView;
        nextView.setVisibility(View.VISIBLE);

        if (targetView.getWidth() == 0 || targetView.getHeight() == 0) {
            targetView.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        targetView.getViewTreeObserver().removeOnPreDrawListener(this);

                        if (targetView.getWidth() == 0 || targetView.getHeight() == 0) {
                            showOnlyNoAnimation(previousChild, childIndex);
                        } else {
                            showOnly21(previousChild, childIndex, point);
                        }
                        return true;
                    }
                });
            return;
        }

        int cx, cy;
        if (null == point) {
            cx = (targetView.getLeft() + targetView.getRight()) / 2;
            cy = (targetView.getTop() + targetView.getBottom()) / 2;
        } else {
            cx = targetView.getLeft() + point.x;
            cy = targetView.getTop() + point.y;
        }

        int finalRadius = Math.max(targetView.getWidth(), targetView.getHeight());

        Animator anim =
            ViewAnimationUtils.createCircularReveal(
                targetView,
                cx,
                cy,
                isReveal ? 0 : finalRadius,
                isReveal ? finalRadius : 0);

        mAnimator = anim;

        // make the view invisible when the animation is done
        anim.addListener(
            new AnimatorListenerAdapter() {
                boolean isCancelled;

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);

                    if (!isCancelled) {
                        if (null != previousView) {
                            previousView.setVisibility(View.GONE);
                        }
                        onAnimationCompleted(previousChild, childIndex);
                        onViewChanged(previousChild, childIndex);
                    }
                }

                @Override
                public void onAnimationCancel(final Animator animation) {
                    isCancelled = true;
                    super.onAnimationCancel(animation);
                }

                @Override
                public void onAnimationStart(final Animator animation) {
                    super.onAnimationStart(animation);
                    onAnimationStarted(previousChild, childIndex);
                }
            });

        anim.setDuration(mAnimationDuration);
        anim.setInterpolator((Interpolator) mInterpolator);
        anim.start();
    }

    protected void showOnly19(final int previousChild, final int childIndex) {
        if (DBG) {
            Log.i(TAG, "showOnly19: " + previousChild + " >> " + childIndex);
        }

        mInAnimation.setAnimationListener(
            new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(final Animation animation) {
                    onAnimationStarted(previousChild, childIndex);
                }

                @Override
                public void onAnimationEnd(final Animation animation) {
                    onAnimationCompleted(previousChild, childIndex);
                    onViewChanged(previousChild, childIndex);
                }

                @Override
                public void onAnimationRepeat(final Animation animation) {
                }
            });

        View nextChild = getChildAt(childIndex);
        nextChild.startAnimation(mInAnimation);
        nextChild.setVisibility(View.VISIBLE);

        View prevChild = getChildAt(previousChild);
        if (prevChild.getAnimation() == mOutAnimation) {
            prevChild.clearAnimation();
        } else if (mOutAnimation != null && prevChild.getVisibility() == View.VISIBLE) {
            prevChild.startAnimation(mOutAnimation);
        } else if (prevChild.getAnimation() == mInAnimation) {
            prevChild.clearAnimation();
        }
        prevChild.setVisibility(View.GONE);
    }

    protected void showOnlyNoAnimation(final int previousChild, final int childIndex) {
        if (DBG) {
            Log.i(TAG, "showOnlyNoAnimation: " + childIndex);
        }
        getChildAt(childIndex).setVisibility(View.VISIBLE);
        getChildAt(previousChild).setVisibility(View.GONE);

        onViewChanged(previousChild, childIndex);
    }

    @TargetApi (11)
    public boolean isAnimating() {
        if (Build.VERSION.SDK_INT >= 21 && !mUseFallbackAnimations) {
            return mAnimator != null && ((Animator) mAnimator).isRunning();
        } else {
            return
                (mInAnimation != null && (mInAnimation.hasStarted() && !mInAnimation.hasEnded()))
                    || (mOutAnimation != null && (mOutAnimation.hasStarted() && !mOutAnimation.hasEnded()));

        }
    }

    public void onAnimationStarted(int prevIndex, int curIndex) {
        if (null != mViewAnimationListener) {
            mViewAnimationListener.onViewAnimationStarted(prevIndex, curIndex);
        }
    }

    public void onAnimationCompleted(int prevIndex, int curIndex) {
        if (null != mViewAnimationListener) {
            mViewAnimationListener.onViewAnimationCompleted(prevIndex, curIndex);
        }
    }

    void onViewChanged(int prevIndex, int curIndex) {
        if (null != mViewChangedListener) {
            mViewChangedListener.onViewChanged(prevIndex, curIndex);
        }
    }

    void showOnly(int previousIndex, int childIndex, Point point) {
        final boolean animate = shouldAnimate();
        showOnly(previousIndex, childIndex, animate, point);
    }

    private boolean shouldAnimate() {
        return ((!mFirstTime || mAnimateFirstTime) && mAnimationsEnabled)
            && (getUseFallbackAnimations() ? null != mInAnimation : true);
    }

    private boolean getUseFallbackAnimations() {
        return Build.VERSION.SDK_INT < 21 || mUseFallbackAnimations;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (DBG) {
            Log.i(TAG, "addView, index: " + index + ", current children: " + getChildCount());
        }
        super.addView(child, index, params);
        if (getChildCount() == 1) {
            child.setVisibility(View.VISIBLE);
        } else {
            child.setVisibility(View.GONE);
        }
        if (index >= 0 && mWhichChild >= index) {
            setDisplayedChild(mWhichChild + 1, null);
        }
    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();
        mWhichChild = 0;
        mPreviousChild = -1;
        mFirstTime = true;
    }

    @Override
    public void removeView(View view) {
        final int index = indexOfChild(view);
        if (index >= 0) {
            removeViewAt(index);
        }
    }

    @Override
    public void removeViewAt(int index) {
        super.removeViewAt(index);
        final int childCount = getChildCount();
        if (childCount == 0) {
            mWhichChild = 0;
            mPreviousChild = -1;
            mFirstTime = true;
        } else if (mWhichChild >= childCount) {
            setDisplayedChild(childCount - 1, null);
        } else if (mWhichChild == index) {
            setDisplayedChild(mWhichChild, null);
        }
    }

    public void removeViewInLayout(View view) {
        removeView(view);
    }

    public void removeViews(int start, int count) {
        super.removeViews(start, count);
        if (getChildCount() == 0) {
            mWhichChild = 0;
            mPreviousChild = -1;
            mFirstTime = true;
        } else if (mWhichChild >= start && mWhichChild < start + count) {
            setDisplayedChild(mWhichChild, null);
        }
    }

    public void removeViewsInLayout(int start, int count) {
        removeViews(start, count);
    }

    public View getCurrentView() {
        return getChildAt(mWhichChild);
    }

    public View getPreviousView() {
        if (mPreviousChild > -1) {
            return getChildAt(mPreviousChild);
        }
        return null;
    }

    public Animation getInAnimation() {
        return mInAnimation;
    }

    public Animation getOutAnimation() {
        return mOutAnimation;
    }

    /**
     * These are the in and out animations used for devices with API &lt; 21
     *
     * @param context
     * @param inAnimationResID
     * @param outAnimationResID
     */
    public void setAnimationFallback(Context context, int inAnimationResID, int outAnimationResID) {
        if (DBG) {
            Log.i(TAG, "setAnimationFallback: " + inAnimationResID + ", " + outAnimationResID);
        }

        Animation inAnimation = null;
        Animation outAnimation = null;

        if (inAnimationResID > 0) {
            inAnimation = AnimationUtils.loadAnimation(context, inAnimationResID);
        }

        if (outAnimationResID > 0) {
            outAnimation = AnimationUtils.loadAnimation(context, outAnimationResID);
        }

        setAnimationFallback(
            inAnimation, outAnimation);
    }

    public void setAnimationFallback(Animation inAnimation, Animation outAnimation) {
        if (DBG) {
            Log.i(TAG, "setAnimationFallback: " + inAnimation + ", " + outAnimation);
        }
        this.mInAnimation = inAnimation;
        this.mOutAnimation = outAnimation;
    }

    public void setAlwaysUseFallbackAnimations(boolean value) {
        mUseFallbackAnimations = value;
    }

    public void setAnimationDuration(int value) {
        mAnimationDuration = value;
    }

    public int getAnimationDuration() {
        return mAnimationDuration;
    }

    @SuppressWarnings ("unused")
    public boolean getAnimateFirstView() {
        return mAnimateFirstTime;
    }

    public void setAnimateFirstView(boolean animate) {
        if (DBG) {
            Log.i(TAG, "setAnimateFirstView: " + animate);
        }
        mAnimateFirstTime = animate;
    }

    /**
     * Turn on/off the animations between views
     *
     * @param animate
     */
    public void setAnimaionsEnabled(boolean animate) {
        if (DBG) {
            Log.i(TAG, "setAnimationsEnabled: " + animate);
        }
        mAnimationsEnabled = animate;
    }

    public boolean getAnimationsEnabled() {
        return mAnimationsEnabled;
    }

    @Override
    public int getBaseline() {
        return (getCurrentView() != null) ? getCurrentView().getBaseline() : super.getBaseline();
    }

    @TargetApi (14)
    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        if (Build.VERSION.SDK_INT >= 14) {
            super.onInitializeAccessibilityEvent(event);
            event.setClassName(ViewAnimator.class.getName());
        }
    }

    @TargetApi (14)
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        if (Build.VERSION.SDK_INT >= 14) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName(ViewAnimator.class.getName());
        }
    }
}
