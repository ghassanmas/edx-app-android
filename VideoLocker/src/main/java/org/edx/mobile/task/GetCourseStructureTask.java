package org.edx.mobile.task;

import android.content.Context;

import org.edx.mobile.base.MainApplication;
import org.edx.mobile.model.course.CourseComponent;
import org.edx.mobile.module.prefs.PrefManager;
import org.edx.mobile.services.ServiceManager;

import java.util.Date;

public abstract class GetCourseStructureTask extends
Task<CourseComponent> {

    public GetCourseStructureTask(Context context) {
        super(context);
    }

    protected CourseComponent doInBackground(Object... params) {
        try {
            String courseId = (String) (params[0]);
            if(courseId!=null){
                PrefManager.UserPrefManager prefManager = new PrefManager.UserPrefManager(MainApplication.instance());
                long lastFetchTime = prefManager.getLastCourseStructureFetch(courseId);
                long curTime = new Date().getTime();
                boolean useCache = true;
                //if last fetch happened over one hour ago, re-fetch data
                if ( lastFetchTime + 3600 * 1000 < curTime ){
                    useCache = false;
                    prefManager.setLastCourseStructureFetch(courseId, curTime);
                }
                final CourseComponent model = ServiceManager.getInstance().getCourseStructure(courseId, useCache);
                if (model != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            onFinish(model);
                            stopProgress();
                        }
                    });
                }
            }
        } catch (Exception ex) {
            handle(ex);
            logger.error(ex, true);
        }
        return null;
    }
}
