package tukano.impl.data;

public class LikesDAO  extends Likes{
    private static final String ID_FORMAT = "%s_%s";
    private String id;

    public LikesDAO() {
        super();
    }

    public LikesDAO(Likes l) {
        super(l.userId, l.shortId, l.ownerId);
        this.id = String.format(ID_FORMAT, l.userId, l.shortId);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
