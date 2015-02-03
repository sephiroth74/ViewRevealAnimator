package it.sephiroth.android.library.demo_viewrevealanimator;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import it.sephiroth.android.library.viewrevealanimator.ViewRevealAnimator;

public class MainActivity extends ActionBarActivity
    implements View.OnClickListener, ViewRevealAnimator.OnViewChangedListener, CompoundButton.OnCheckedChangeListener, ViewRevealAnimator.OnViewAnimationListener {
    private static final String TAG = "MainActivity";
    ViewRevealAnimator mViewAnimator;
    private boolean mHideBeforeReveal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewAnimator = (ViewRevealAnimator) findViewById(R.id.animator);
        findViewById(R.id.next).setOnClickListener(this);
        findViewById(R.id.next2).setOnClickListener(this);
        findViewById(R.id.previous).setOnClickListener(this);

        CheckBox checkbox = (CheckBox) findViewById(R.id.checkBox);
        checkbox.setOnCheckedChangeListener(this);

        mHideBeforeReveal = checkbox.isChecked();
        mViewAnimator.setHideBeforeReveal(mHideBeforeReveal);

        mViewAnimator.setOnTouchListener(
            new View.OnTouchListener() {
                @Override
                public boolean onTouch(final View v, final MotionEvent event) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        mViewAnimator.showNext();
                        return true;
                    }
                    return false;
                }
            });

        mViewAnimator.setOnViewChangedListener(this);
        mViewAnimator.setOnViewAnimationListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(final View v) {
        final int id = v.getId();

        switch (id) {
            case R.id.next:
                mViewAnimator.showNext();
                break;

            case R.id.previous:
                mViewAnimator.showPrevious();
                break;

            case R.id.next2:
                int current = mViewAnimator.getDisplayedChild() + 2;
                current = current % mViewAnimator.getChildCount();
                mViewAnimator.setDisplayedChild(current, null);
                break;
        }
    }

    @Override
    public void onViewChanged(final int previousIndex, final int currentIndex) {
        Log.d(TAG, "onViewChanged(" + previousIndex + ":" + currentIndex + ")");
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        mHideBeforeReveal = isChecked;
        mViewAnimator.setHideBeforeReveal(mHideBeforeReveal);
    }

    @Override
    public void onViewAnimationStarted(final int previousIndex, final int currentIndex) {
        Log.d(TAG, "onViewanimationStarted(" + previousIndex + ":" + currentIndex + ")");
    }

    @Override
    public void onViewAnimationCompleted(final int previousIndex, final int currentIndex) {
        Log.d(TAG, "onViewanimationCompleted(" + previousIndex + ":" + currentIndex + ")");
    }
}
