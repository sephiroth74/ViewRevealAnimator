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
    private static final String TAG = "ViewRevealAnimator";
    private static final boolean DBG = true;
    int mWhichChild = 0;
    boolean mFirstTime = true;
    boolean mAnimateFirstTime = true;
    int mAnimationDuration;
    Animation mInAnimation;
    Animation mOutAnimation;
    Object mAnimator;
    boolean mAnimatorAnimating;
    Interpolator mInterpolator;
    OnViewChangedListener mViewChangedListener;
    RevealAnimator mInstance;
    boolean mHideBeforeReveal;

    public int getViewRadius(final View view) {
        return Math.min(view.getWidth(), view.getHeight());
    }

    public interface OnViewChangedListener {
        void onViewChanged(int previousIndex, int currentIndex);
    }

    public ViewRevealAnimator(Context context) {
        this(context, null);
    }

    public ViewRevealAnimator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewRevealAnimator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);

        if (Build.VERSION.SDK_INT >= 21) {
            mInstance = new LollipopRevealAnimatorImpl(this);
        } else {
            mInstance = new ICSRevealAnimatorImpl(this);
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewRelealAnimator);

        int resourceIn = a.getResourceId(R.styleable.ViewRelealAnimator_android_inAnimation, 0);
        int resourceOut = a.getResourceId(R.styleable.ViewRelealAnimator_android_outAnimation, 0);
        boolean flag = a.getBoolean(R.styleable.ViewRelealAnimator_android_animateFirstView, true);
        int animationDuration = a.getInteger(R.styleable.ViewRelealAnimator_android_animationDuration, 400);
        boolean hideBeforeReveal = a.getBoolean(R.styleable.ViewRelealAnimator_vra_hideBeforeReveal, true);

        setInAnimation(context, resourceIn);
        setOutAnimation(context, resourceOut);
        setAnimateFirstView(flag);

        setAnimationDuration(animationDuration);
        setHideBeforeReveal(hideBeforeReveal);

        if (Build.VERSION.SDK_INT >= 21) {
            int resID =
                a.getResourceId(R.styleable.ViewRelealAnimator_android_interpolator, android.R.interpolator.accelerate_decelerate);
            Interpolator interpolator = AnimationUtils.loadInterpolator(context, resID);
            setInterpolator(interpolator);
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

    public void setOnViewChangedListener(OnViewChangedListener listener) {
        mViewChangedListener = listener;
    }

    /**
     * Sets which child view will be displayed.
     *
     * @param whichChild the index of the child view to display
     */
    public void setDisplayedChild(int whichChild) {
        setDisplayedChild(whichChild, null);
    }

    public void setDisplayedChild(int whichChild, @Nullable Point origin) {
        Log.i(TAG, "setDisplayedChild, current: " + mWhichChild + ", next: " + whichChild);

        if (whichChild == mWhichChild) {
            // same child
            return;
        }

        int mPreviousChild = mWhichChild;
        mWhichChild = whichChild;

        if (whichChild >= getChildCount()) {
            mWhichChild = 0;
        } else if (whichChild < 0) {
            mWhichChild = getChildCount() - 1;
        }
        boolean hasFocus = getFocusedChild() != null;
        showOnly(mPreviousChild, mWhichChild);
        if (hasFocus) {
            requestFocus(FOCUS_FORWARD);
        }
    }

    /**
     * Returns the index of the currently displayed child view.
     *
     * @return
     */
    public int getDisplayedChild() {
        return mWhichChild;
    }

    public void showNext() {
        setDisplayedChild(mWhichChild + 1);
    }

    public void showPrevious() {
        setDisplayedChild(mWhichChild - 1);
    }

    void showOnly(int previousIndex, int childIndex) {
        final boolean animate = shouldAnimate();
        showOnly(previousIndex, childIndex, animate);
    }

    void showOnly(int previousChild, int childIndex, boolean animate) {
        Log.i(TAG, "showOnly: " + previousChild + " >> " + childIndex + ", animate: " + animate);

        mFirstTime = false;

        if (!animate) {
            mInstance.showOnlyNoAnimation(previousChild, childIndex);
            onViewChanged(previousChild, childIndex);
        } else {
            mInstance.showOnly(previousChild, childIndex);
        }
    }

    @TargetApi (21)
    private void circularHide(final int previousIndex, final int nextIndex, final Point point) {
        final View previousView = getChildAt(previousIndex);
        final View nextView = getChildAt(nextIndex);

        Point newPoint = getViewCenter(previousView);
        int finalRadius = Math.max(previousView.getWidth(), previousView.getHeight());

        Animator animator = ViewAnimationUtils.createCircularReveal(previousView, newPoint.x, newPoint.y, finalRadius, 0);
        animator.addListener(
            new AnimatorListenerAdapter() {
                boolean isCancelled;

                @Override
                public void onAnimationStart(final Animator animation) {
                    super.onAnimationStart(animation);
                }

                @Override
                public void onAnimationCancel(final Animator animation) {
                    Log.v(TAG, "onAnimationCancel(out)");
                    isCancelled = true;
                    mAnimatorAnimating = false;
                    showOnlyNoAnimation(previousIndex, nextIndex);
                }

                @Override
                public void onAnimationEnd(final Animator animation) {
                    if (!isCancelled) {
                        Log.v(TAG, "onAnimationEnd(out)");
                        super.onAnimationEnd(animation);
                        circularReveal(previousIndex, nextIndex, point);
                    }
                }
            });

        mAnimator = animator;
        animator.setDuration(mAnimationDuration);
        animator.setInterpolator((Interpolator) mInterpolator);
        animator.start();

        mAnimatorAnimating = true;
    }

    @TargetApi (21)
    private void circularReveal(final int previousIndex, final int nextIndex, final Point point) {
        if (DBG) {
            Log.i(TAG, "circularReveal: " + previousIndex + " > " + nextIndex);
        }

        final View nextView = getChildAt(nextIndex);
        final View previousView = getChildAt(previousIndex);
        final View targetView = nextView;

        showOnlyNoAnimation(previousIndex, nextIndex);

        if (targetView.getWidth() == 0 || targetView.getHeight() == 0) {
            targetView.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        targetView.getViewTreeObserver().removeOnPreDrawListener(this);

                        if (targetView.getWidth() == 0 || targetView.getHeight() == 0) {
                            mAnimatorAnimating = false;
                            showOnlyNoAnimation(previousIndex, nextIndex);
                            onViewChanged(previousIndex, nextIndex);
                        } else {
                            circularReveal(previousIndex, nextIndex, point);
                        }
                        return true;
                    }
                });
            return;
        }

        Point newPoint = getViewCenter(targetView);
        int finalRadius = Math.max(targetView.getWidth(), targetView.getHeight());

        Animator animator = ViewAnimationUtils
            .createCircularReveal(targetView, newPoint.x, newPoint.y, 0, finalRadius);

        animator.addListener(
            new AnimatorListenerAdapter() {
                boolean isCancelled;

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mAnimatorAnimating = false;

                    if (!isCancelled) {
                        onViewChanged(previousIndex, nextIndex);
                    }
                }

                @Override
                public void onAnimationCancel(final Animator animation) {
                    isCancelled = true;
                    mAnimatorAnimating = false;
                    super.onAnimationCancel(animation);
                }
            });

        mAnimator = animator;
        animator.setDuration(mAnimationDuration);
        animator.setInterpolator((Interpolator) mInterpolator);
        animator.start();
    }

    protected Point getViewCenter(final View targetView) {
        Point newPoint = new Point();
        newPoint.x = (targetView.getLeft() + targetView.getRight()) / 2;
        newPoint.y = (targetView.getTop() + targetView.getBottom()) / 2;
        return newPoint;
    }

    protected void showOnlyNoAnimation(final int previousIndex, final int childIndex) {
        mInstance.showOnlyNoAnimation(previousIndex, childIndex);
    }

    public boolean isAnimating() {
        return mInstance.isAnimating();
    }

    void onViewChanged(int prevIndex, int curIndex) {
        if (null != mViewChangedListener) {
            mViewChangedListener.onViewChanged(prevIndex, curIndex);
        }
    }

    private boolean shouldAnimate() {
        return mInstance.shouldAnimate();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        Log.i(TAG, "addView, index: " + index + ", current children: " + getChildCount());
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
            mFirstTime = true;
        } else if (mWhichChild >= start && mWhichChild < start + count) {
            setDisplayedChild(mWhichChild, null);
        }
    }

    public void removeViewsInLayout(int start, int count) {
        removeViews(start, count);
    }

    /**
     * Returns the current visible child
     *
     * @return
     */
    public View getCurrentView() {
        return getChildAt(mWhichChild);
    }

    /**
     * Returns the current animation used to animate a View that enters the screen.
     *
     * @return An Animation or null if none is set.
     * @see #setInAnimation(android.view.animation.Animation)
     * @see #setInAnimation(android.content.Context, int)
     */
    public Animation getInAnimation() {
        return mInAnimation;
    }

    /**
     * Specifies the animation used to animate a View that enters the screen.
     *
     * @param inAnimation The animation started when a View enters the screen.
     * @see #getInAnimation()
     * @see #setInAnimation(android.content.Context, int)
     */
    public void setInAnimation(Animation inAnimation) {
        mInAnimation = inAnimation;
    }

    /**
     * Returns the current animation used to animate a View that exits the screen.
     *
     * @return An Animation or null if none is set.
     * @see #setOutAnimation(android.view.animation.Animation)
     * @see #setOutAnimation(android.content.Context, int)
     */
    public Animation getOutAnimation() {
        return mOutAnimation;
    }

    /**
     * Specifies the animation used to animate a View that exit the screen.
     *
     * @param outAnimation The animation started when a View exit the screen.
     * @see #getOutAnimation()
     * @see #setOutAnimation(android.content.Context, int)
     */
    public void setOutAnimation(Animation outAnimation) {
        mOutAnimation = outAnimation;
    }

    /**
     * Specifies the animation used to animate a View that enters the screen.
     *
     * @param context    The application's environment.
     * @param resourceID The resource id of the animation.
     * @see #getInAnimation()
     * @see #setInAnimation(android.view.animation.Animation)
     */
    public void setInAnimation(Context context, int resourceID) {
        setInAnimation(AnimationUtils.loadAnimation(context, resourceID));
    }

    /**
     * Specifies the animation used to animate a View that exit the screen.
     *
     * @param context    The application's environment.
     * @param resourceID The resource id of the animation.
     * @see #getOutAnimation()
     * @see #setOutAnimation(android.view.animation.Animation)
     */
    public void setOutAnimation(Context context, int resourceID) {
        setOutAnimation(AnimationUtils.loadAnimation(context, resourceID));
    }

    public void setHideBeforeReveal(boolean value) {
        mHideBeforeReveal = value;
    }

    public boolean getHideBeforeReveal() {
        return mHideBeforeReveal;
    }

    public void setAnimationDuration(int value) {
        mAnimationDuration = value;
    }

    public int getAnimationDuration() {
        return mAnimationDuration;
    }

    public void setInterpolator(final Interpolator mInterpolator) {
        this.mInterpolator = mInterpolator;
    }

    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    public boolean getAnimateFirstView() {
        return mAnimateFirstTime;
    }

    public void setAnimateFirstView(boolean animate) {
        mAnimateFirstTime = animate;
    }

    @Override
    public int getBaseline() {
        return (getCurrentView() == null) ? super.getBaseline() : getCurrentView().getBaseline();
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(ViewAnimator.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(ViewAnimator.class.getName());
    }
}
