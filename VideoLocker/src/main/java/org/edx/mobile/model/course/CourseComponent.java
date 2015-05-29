package org.edx.mobile.model.course;

import org.edx.mobile.model.api.IPathNode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * Default implementation of IBlock
 */
public class CourseComponent implements IBlock, IPathNode {
    private String id;
    private BlockType type;
    private String name;
    private boolean graded;
    private boolean gradedSubDAG;
    private boolean mobileSupported;
    private String blockUrl;
    private String webUrl;
    private BlockCount blockCount;
    protected CourseComponent parent;
    protected CourseComponent root;
    protected List<CourseComponent> children = new ArrayList<>();

    public CourseComponent(){}

    /**
     *
     * @param blockModel
     * @param parent  is null if and only if this is the root
     */
    public CourseComponent(BlockModel blockModel, CourseComponent parent){
        this.id = blockModel.id;
        this.type = blockModel.type;
        this.name = blockModel.displayName;
        this.graded = blockModel.graded;
        this.gradedSubDAG = blockModel.gradedSubDAG;
        this.blockUrl = blockModel.blockUrl;
        this.webUrl = blockModel.webUrl;
        // this.mobileSupported = mobileSupported;
        //FIXME -for testing only
        this.mobileSupported =  blockUrl.hashCode() % 2 == 0;
        this.blockCount = blockModel.blockCount == null ? new BlockCount() : blockModel.blockCount;
        this.parent = parent;
        if ( parent == null){
            this.root = this;
        } else {
            parent.getChildren().add(this);
            //we cache the root to improve the performance
            this.root = (CourseComponent)parent.getRoot();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public BlockType getType() {
        return type;
    }

    @Override
    public void setType(BlockType type) {
        this.type = type;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public void setDisplayName(String name) {
        this.name = name;
    }

    @Override
    public boolean isGraded() {
        return graded;
    }

    @Override
    public void setGraded(boolean graded) {
        this.graded = graded;
    }

    @Override
    public boolean isGradedSubDAG() {
        return gradedSubDAG;
    }

    @Override
    public void setGradedSubDAG(boolean gradedSubDAG) {
        this.gradedSubDAG = gradedSubDAG;
    }

    @Override
    public String getBlockUrl() {
        return blockUrl;
    }

    @Override
    public void setBlockUrl(String url) {
        this.blockUrl = url;
    }

    @Override
    public String getWebUrl() {
        return webUrl;
    }

    @Override
    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    @Override
    public BlockCount getBlockCount() {
        return blockCount;
    }

    @Override
    public void setBlockCount(BlockCount count) {
        this.blockCount = blockCount;
    }

    @Override
    public CourseComponent getParent() {
        return parent;
    }

    @Override
    public List<IBlock> getChildren() {
        return (List) children;
    }

    @Override
    public CourseComponent getRoot(){
        return root;
    }


    public boolean isMobileSupported() {
        return mobileSupported;
    }

    public void setMobileSupported(boolean mobileSupported) {
        this.mobileSupported = mobileSupported;
    }

    public boolean isContainer(){
        return children != null && children.size() > 0;
    }

    /**
     * get direct children who have child.  it is not based on the block type, but on
     * the real tree structure.
     * @return
     */
    public List<CourseComponent> getChildContainers(){
        List<CourseComponent> childContainers = new ArrayList<>();
        if ( children != null ){
            for(CourseComponent c : children){
                if ( c.isContainer() )
                    childContainers.add(c);
            }
        }
        return childContainers;
    }

    /**
     * get direct children who is leaf.  it is not based on the block type, but on
     * the real tree structure.
     * @return
     */
    public List<CourseComponent> getChildLeafs(){
        List<CourseComponent> childLeafs = new ArrayList<>();
        if ( children != null ){
            for(CourseComponent c : children){
                if ( !c.isContainer() )
                    childLeafs.add(c);
            }
        }
        return childLeafs;
    }

    /**
     * return all videos blocks under this node
     */
    public List<HasDownloadEntry> getVideos(){
        List<CourseComponent> videos = new ArrayList<>();
        fetchAllLeafComponents(videos, EnumSet.of(BlockType.VIDEO));
        return (List)videos;
    }

    /**
     * used for navigation.
     * @return <code>true</code> if it is the last child of direct parent. or it does not has direct parent
     *         <code>false</code> if it is not
     */
    public boolean isLastChild(){
        if ( parent == null )
            return true;
        List<IBlock> sibling = parent.getChildren();
        if ( sibling == null ) {
            return false;  //it wont happen. TODO - should we log here?
        }
        return sibling.indexOf(this) == sibling.size() -1;
    }

    /**
     * we get all the leaves below this node.  if this node itself is leaf,
     * just add it to list
     * @param leaves
     */
    public void fetchAllLeafComponents(List<CourseComponent> leaves, EnumSet<BlockType> types){
         if ( !isContainer() && types.contains(type)){
             leaves.add(this);
         } else {
             for( CourseComponent comp : children ){
                 comp.fetchAllLeafComponents(leaves, types);
             }
         }
    }

    /**
     * get the ancestor based on level, level = 0, means itself.
     * if level is out of the boundary, just return the toppest one
     * @param level
     * @return it will never return null.
     */
    public CourseComponent getAncestor(int level){
        if ( parent == null || level == 0 )
            return this;

        IBlock root = parent;
        while ( level != 0  && root.getParent() != null ){
            root = root.getParent();
            level--;
        }
        return (CourseComponent)root;
    }

    /**
     * get ancestor with give blockType, starting from itself
     */
    public CourseComponent getAncestor(EnumSet<BlockType> types){
        if( types.contains(type) )
            return this;
        IBlock ancestor = parent;
        if ( ancestor == null )
            return null;
        do{
           if ( types.contains( ancestor.getType() ) )
               return (CourseComponent) ancestor;
        }while ((ancestor = ancestor.getParent()) != null );
        return null;
    }

    @Override
    public boolean equals(Object obj){
        if ( obj == null || !(obj instanceof CourseComponent) )
            return false;
        CourseComponent other = (CourseComponent)obj;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode(){
        return this.id.hashCode();
    }


    //// implement IPathNode interface, for backward compatibility only
    @Override
    public boolean isChapter() {
        return  getType() == BlockType.CHAPTER;
    }

    @Override
    public boolean isSequential() {
        return  getType() == BlockType.SEQUENTIAL;
    }

    @Override
    public boolean isVertical() {
        return  getType() == BlockType.VERTICAL;
    }

    @Override
    public String getCategory() {
        return  getType().name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String getName() {
        return  getDisplayName();
    }
}
