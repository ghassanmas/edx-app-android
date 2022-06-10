package org.edx.mobile.view;

import static org.edx.mobile.http.util.CallUtil.executeStrict;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Java6Assertions;
import org.edx.mobile.R;
import org.edx.mobile.base.HiltTestActivity;
import org.edx.mobile.course.CourseAPI;
import org.edx.mobile.model.api.EnrolledCoursesResponse;
import org.edx.mobile.model.course.CourseComponent;
import org.edx.mobile.model.course.CourseStructureV1Model;
import org.edx.mobile.model.course.VideoBlockModel;
import org.edx.mobile.util.UiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.support.v4.SupportFragmentController;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;

// We should add mock downloads, mock play, and state retention tests
// later. Also, online/offline transition tests; although the
// onOnline() and onOffline() methods don't seem to be called from
// anywhere yet?
@RunWith(RobolectricTestRunner.class)
public abstract class BaseCourseUnitVideoFragmentTest extends UiTest {

    protected abstract BaseCourseUnitVideoFragment getCourseUnitPlayerFragmentInstance();

    /**
     * Method for iterating through the mock course response data, and
     * returning the first video block leaf.
     *
     * @return The first {@link VideoBlockModel} leaf in the mock course data
     */
    VideoBlockModel getVideoUnit() {
        final EnrolledCoursesResponse courseData;
        try {
            courseData = executeStrict(courseAPI.getEnrolledCourses()).get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        final String courseId = courseData.getCourse().getId();
        final CourseStructureV1Model model;
        final CourseComponent courseComponent;
        try {
            model = executeStrict(courseAPI.getCourseStructure(config.getApiUrlVersionConfig().getBlocksApiVersion(), courseId));
            courseComponent = (CourseComponent) CourseAPI.normalizeCourseStructure(model, courseId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return courseComponent.getVideos().get(0);
    }

    /**
     * Testing initialization
     */
    @Test
    public void initializeTest() {
        final BaseCourseUnitVideoFragment fragment = CourseUnitVideoPlayerFragment.newInstance(getVideoUnit(), false, false);
        SupportFragmentController.setupFragment(fragment, HiltTestActivity.class,
                android.R.id.content, null);

        final View view = fragment.getView();
        assertNotNull(view);
        final View messageContainer = view.findViewById(R.id.message_container);
        assertNotNull(messageContainer);
    }

    /**
     * Generic method for testing setup on orientation changes
     *
     * @param fragment    The current fragment
     * @param orientation The orientation change to test
     */
    private void testOrientationChange(
            BaseCourseUnitVideoFragment fragment, int orientation) {
        final Resources resources = fragment.getResources();
        final Configuration config = resources.getConfiguration();
        assertNotEquals(orientation, config.orientation);
        config.orientation = orientation;
        fragment.onConfigurationChanged(config);

        final View view = fragment.getView();
        assertNotNull(view);

        final boolean isLandscape = config.orientation ==
                Configuration.ORIENTATION_LANDSCAPE;

        final View playerContainer = view.findViewById(R.id.player_container);
        if (playerContainer != null) {
            Java6Assertions.assertThat(playerContainer).isInstanceOf(ViewGroup.class);
            ViewGroup.LayoutParams layoutParams = playerContainer.getLayoutParams();
            assertNotNull(layoutParams);
            Assertions.assertThat(layoutParams.width).isEqualTo(ViewGroup.LayoutParams.MATCH_PARENT);
            final DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            final int height = isLandscape ? displayMetrics.heightPixels :
                    (displayMetrics.widthPixels * 9 / 16);
            Assertions.assertThat(layoutParams.height).isEqualTo(height);
        }
    }

    /**
     * Testing orientation changes
     */
    @Test
    public void orientationChangeTest() {
        final BaseCourseUnitVideoFragment fragment = getCourseUnitPlayerFragmentInstance();
        SupportFragmentTestUtil.startVisibleFragment(fragment, HiltTestActivity.class, 1);
        assertNotEquals(Configuration.ORIENTATION_LANDSCAPE,
                fragment.getResources().getConfiguration().orientation);

        testOrientationChange(fragment, Configuration.ORIENTATION_LANDSCAPE);
        testOrientationChange(fragment, Configuration.ORIENTATION_PORTRAIT);
    }
}