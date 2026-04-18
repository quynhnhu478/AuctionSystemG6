package auction.model;
import java.time.LocalDateTime;
public abstract class Entity {

    protected String id,name;
    protected LocalDateTime createdAt;

    public Entity(String id, String name) {
        this.id=id;
        this.name=name;
        this.createdAt=LocalDateTime.now();
    }

    protected Entity() {
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}