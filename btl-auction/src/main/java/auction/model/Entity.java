package auction.model;
import java.time.LocalDateTime;
public abstract class Entity {
    protected String id,name;
    protected LocalDateTime createdAt;//ghi lại thời điểm được tạo ra
    public Entity(String id, String name){
        this.id=id;
        this.name=name;
    }
}
//lop co so