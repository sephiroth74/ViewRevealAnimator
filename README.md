[![AndroidLibs](https://img.shields.io/badge/AndroidLibs-ViewRevealAnimator-brightgreen.svg?style=flat)](https://www.android-libs.com/lib/viewrevealanimator?utm_source=github-badge&utm_medium=github-badge&utm_campaign=github-badge)
		ViewRevealAnimator Widget
==================

ViewAnimator view with a lollipop style reveal effect. Regular animation can be set (just like the default ViewAnimator) for android < 21.


Installation
===

Just add this to your gradle build script:

    compile 'it.sephiroth.android.library.viewrevealanimator:view-reveal-animator:+'

Usage
===

    <it.sephiroth.android.library.viewrevealanimator.ViewRevealAnimator
        android:layout_centerHorizontal="true"
        android:id="@+id/animator"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:animationDuration="@android:integer/config_longAnimTime"
        app:vra_hideBeforeReveal="true"
        android:measureAllChildren="true"
        android:animateFirstView="true"
        android:outAnimation="@anim/out_animation"
        android:inAnimation="@anim/in_animation"
        android:interpolator="@android:interpolator/accelerate_decelerate">


        <Your Views here />


    </it.sephiroth.android.library.viewrevealanimator.ViewRevealAnimator>


Then in your code:

    ViewRevealAnimator mViewAnimator = (ViewRevealAnimator) findViewById(R.id.animator);


To show the next or previos index :

    mViewAnimator.showNext();
    mViewAnimator.showPrevious();

or:

    mViewAnimator.setDisplayedChild(mViewAnimator.getDisplayedChild()+1, true, new Point(250, 250));

where the first parameter is the targetChild to display, second means animation on/off, and
third parameter (optional) is to set a custom origin for the reveal/hide animation


Show previous view:


Preview
===

![Preview](./output.gif)