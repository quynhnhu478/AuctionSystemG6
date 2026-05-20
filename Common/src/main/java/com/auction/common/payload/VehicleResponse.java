<<<<<<<< HEAD:Server/src/main/java/com/auction/server/payload/Item/VehicleResponse.java
package com.auction.server.payload.Item;

import com.auction.server.model.Item.Categories;
========
package com.auction.common.payload;

import com.auction.common.enums.Categories;
>>>>>>>> origin/ngọc_2:Common/src/main/java/com/auction/common/payload/VehicleResponse.java

public class VehicleResponse extends ItemResponse {
    public VehicleResponse() {
        super();
    }
    public VehicleResponse(Long id, String name, Double price, String description, Enum<Categories> categories, Long sellerId) {
        super(id, name, price, description, categories, sellerId);
    }
}
