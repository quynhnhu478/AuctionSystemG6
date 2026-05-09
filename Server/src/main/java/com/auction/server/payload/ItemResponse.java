package com.auction.server.payload;

import com.auction.server.model.Categories;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Setter;
import lombok.Getter;
// 1. Chỉ định dùng trường có sẵn trong class (EXISTING_PROPERTY)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
// 2. Map các giá trị String của Enum (name()) với các Class tương ứng
@JsonSubTypes({
        @JsonSubTypes.Type(value = ElectronicsResponse.class, name = "ELECTRONICS"),
        @JsonSubTypes.Type(value = ArtResponse.class, name = "ART"),
        @JsonSubTypes.Type(value = VehicleResponse.class, name = "CLOTHES")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemResponse {
    @Setter
    @Getter
    private Long id;
    @Setter
    @Getter
    private String name;
    @Getter
    @Setter
    private Double price;
    @Getter
    @Setter
    private String description;
    @Getter
    @Setter
    private Enum<Categories> categories;
    @Getter
    @Setter
    private Long sellerId;

    public ItemResponse() {}
    public ItemResponse(Long id, String name, Double price, String description, Enum<Categories> categories, Long sellerId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.categories = categories;
        this.sellerId = sellerId;
    }

}
