package org.edx.mobile.services;

import org.edx.mobile.interfaces.SectionItemInterface;
import org.edx.mobile.model.Filter;
import org.edx.mobile.model.api.IPathNode;
import org.edx.mobile.model.api.LectureModel;
import org.edx.mobile.model.api.SectionEntry;
import org.edx.mobile.model.api.SummaryModel;
import org.edx.mobile.model.api.VideoResponseModel;
import org.edx.mobile.model.course.BlockModel;
import org.edx.mobile.model.course.BlockType;
import org.edx.mobile.model.course.CourseComponent;
import org.edx.mobile.model.course.CourseStructureV1Model;
import org.edx.mobile.model.course.HasDownloadEntry;
import org.edx.mobile.model.course.HtmlBlockModel;
import org.edx.mobile.model.course.IBlock;
import org.edx.mobile.model.course.VideoBlockModel;
import org.edx.mobile.model.course.VideoData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  A central place for course data model transformation
 */
public class CourseManager {

    private static CourseManager instance;

    public static synchronized CourseManager getSharedInstance(){
        if ( instance == null )
            instance = new CourseManager();
        return instance;
    }


    private CourseManager(){

    }

    /**
     * Mapping from raw data structure from getCourseStructure() API
     * @param courseStructureV1Model
     * @return
     */
    public static IBlock normalizeCourseStructure(CourseStructureV1Model courseStructureV1Model){
        BlockModel topBlock = courseStructureV1Model.getBlockById(courseStructureV1Model.root);
        CourseComponent course = new CourseComponent(topBlock, null);

        for (BlockModel m : courseStructureV1Model.getDescendants(topBlock)) {
            normalizeCourseStructure(courseStructureV1Model,m,course);
        }
        return course;
    }


    private static void normalizeCourseStructure(CourseStructureV1Model courseStructureV1Model,
                                                BlockModel block,
                                                CourseComponent parent){

        if ( block.isContainer() ){
            CourseComponent child = new CourseComponent(block, parent);
            for (BlockModel m : courseStructureV1Model.getDescendants(block)) {
                normalizeCourseStructure(courseStructureV1Model, m, child);
            }
        } else {
            if ( BlockType.VIDEO == block.type && block.data != null
                && (block.data  instanceof VideoData) ){
                new VideoBlockModel(block, parent);
            } else { //everything else.. we fallback to html component
                new HtmlBlockModel(block, parent);
            }
        }
    }

    /**
     * we map the new course outline data to old data model.
     * TODO : Ideally we should update all the code to match the new data model.
     * @param courseComponent
     * @return
     */
    public static List<SectionItemInterface> mappingAllVideoResponseModelFrom(CourseComponent courseComponent, Filter<VideoResponseModel> filter){
        List<SectionItemInterface> items = new ArrayList<>();
        for(HasDownloadEntry item : courseComponent.getVideos()){
            VideoResponseModel model = mappingVideoResponseModelFrom((VideoBlockModel)item);
            if ( filter == null )
                items.add( model );
            else {
                if (filter.apply(model)){
                    items.add( model );
                }
            }
        }
        return items;
    }

    /**
     * from new VideoBlockModel to legacy VideoRsponseModel
     * @param videoBlockModel
     * @return
     */
    public static VideoResponseModel mappingVideoResponseModelFrom(VideoBlockModel videoBlockModel){
        VideoResponseModel model = new VideoResponseModel();
        model.setCourseId(videoBlockModel.getRoot().getId());
        SummaryModel summaryModel = mappingSummaryModelFrom(videoBlockModel);
        model.setSummary(summaryModel);
        model.videoBlockModel = videoBlockModel;
        model.setSectionUrl(videoBlockModel.getParent().getBlockUrl());
        model.setUnitUrl(videoBlockModel.getBlockUrl());

        return model;
    }


    private static SummaryModel mappingSummaryModelFrom(VideoBlockModel videoBlockModel){
        SummaryModel model = new SummaryModel();
        model.setType(videoBlockModel.getType());
        model.setDisplayName(videoBlockModel.getDisplayName());
        model.setVideoUrl(videoBlockModel.getData().encodedVideos.getPreferredVideoInfo().url);
        model.setDuration((int)videoBlockModel.getData().duration);
        //FIXME - is this field missing?
        //model.setOnlyOnWeb(videoBlockModel.);
       // public boolean onlyOnWeb;
        model.setId(videoBlockModel.getId());
        model.setSize(videoBlockModel.getSize());
        model.setTranscripts(videoBlockModel.getData().transcripts);
        //FIXME = is this field missing?
       // private EncodingsModel encodings;
        return model;
    }

    /**
     * from new CourseComponent to legacy data structure.
     * @param courseComponent
     * @return
     */
    public static Map<String, SectionEntry> mappingCourseHierarchyFrom(CourseComponent courseComponent){
        Map<String, SectionEntry> map = new HashMap<>();
        for(IBlock block : courseComponent.getChildren()){
            CourseComponent chapter = (CourseComponent)block;
            SectionEntry entry = new SectionEntry();
            entry.chapter = chapter.getName();
            entry.isChapter = true;
            entry.section_url = chapter.getBlockUrl();
            map.put(entry.chapter, entry);

            for( IBlock subBlock : chapter.getChildren() ){
                CourseComponent section = (CourseComponent)subBlock;

                entry.sections.put(section.getName(),
                    (ArrayList)CourseManager.mappingAllVideoResponseModelFrom(section, null));
            }
        }
        return map;
    }

    /**
     * we handle both name and id for backward compatibility. legacy code use name, it is not a good idea as name is not
     * grantee to be unique.
     */
    public static LectureModel getLecture(CourseComponent courseComponent, String chapterName, String chapterId, String lectureName, String lectureId)
        throws Exception {
        //TODO - we may use a generic filter to fetch the data?
        for(IBlock chapter : courseComponent.getChildren()){
            if ( chapter.getId().equals(chapterId) ){
                for(IBlock lecture : chapter.getChildren() ){
                    //TODO - check to see if need to compare id or not
                    if ( lecture.getId().equals(lectureId) ){
                        LectureModel lm = new LectureModel();
                        lm.name = lecture.getDisplayName();
                        lm.videos = (ArrayList) mappingAllVideoResponseModelFrom((CourseComponent)lecture, null );
                        return lm;
                    }
                }
            }
        }
        //if we can not find object by id, try to get by name.
        for(IBlock chapter : courseComponent.getChildren()){
            if ( chapter.getDisplayName().equals(chapterName) ){
                for(IBlock lecture : chapter.getChildren() ){
                    //TODO - check to see if need to compare id or not
                    if ( lecture.getDisplayName().equals(lectureName) ){
                        LectureModel lm = new LectureModel();
                        lm.name = lecture.getDisplayName();
                        lm.videos = (ArrayList) mappingAllVideoResponseModelFrom((CourseComponent)lecture, null );
                        return lm;
                    }
                }
            }
        }

        return null;
    }

    public static VideoResponseModel getVideoById(CourseComponent courseComponent, String videoId)
        throws Exception {
        for(HasDownloadEntry item : courseComponent.getVideos()) {
            VideoBlockModel model = (VideoBlockModel)item;
            if (model.getId().equals(videoId))
                return mappingVideoResponseModelFrom((VideoBlockModel) item);
        }
        return null;
    }

    /**
     *
     * @param courseComponent
     * @param subsectionId
     * @return
     */
    public static VideoResponseModel getSubsectionById(CourseComponent courseComponent, String subsectionId){
        ////TODO - we may use a generic filter to fetch the data?
        Map<String, SectionEntry> map = mappingCourseHierarchyFrom(courseComponent);
        for (Map.Entry<String, SectionEntry> chapterentry : map.entrySet()) {
            // iterate lectures
            for (Map.Entry<String, ArrayList<VideoResponseModel>> entry :
                chapterentry.getValue().sections.entrySet()) {
                // iterate videos
                for (VideoResponseModel v : entry.getValue()) {
                    // identify the subsection (module) if id matches
                    IPathNode node = v.getSection();
                    if (node != null  && subsectionId.equals(node.getId())) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

}
