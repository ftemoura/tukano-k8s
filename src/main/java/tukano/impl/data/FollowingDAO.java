package tukano.impl.data;

public class FollowingDAO extends Following{
    private static final String ID_FORMAT = "%s_%s";
    private String id;

    public FollowingDAO() {
        super();
    }

    public FollowingDAO(Following f) {
        super(f.follower, f.followee);
        this.id = String.format(ID_FORMAT, f.follower, f.followee);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
